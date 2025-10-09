package io.dscope.camel.snowflake.mcp;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Integration test exercising the REST entry point with the token bucket rate limiter.
 */
public class McpRateLimitIntegrationTest extends CamelTestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT = new TypeReference<>() { };

    private int port;

    @AfterEach
    void clearRateLimitProperties() {
        System.clearProperty("mcp.rate.bucketCapacity");
        System.clearProperty("mcp.rate.refillPerSecond");
    }

    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        super.bindToRegistry(registry);

        McpMethodDefinition methodDefinition = new McpMethodDefinition();
        methodDefinition.setName("selectSample");
        methodDefinition.setTitle("Select Sample Rows");
        methodDefinition.setDescription("Demo method for integration testing");
        methodDefinition.setQuery("SELECT 1");
        methodDefinition.setInputSchema(Map.of(
                "type", "object",
                "required", List.of("user_id")));

        McpMethodCatalog catalog = new McpMethodCatalog(List.of(methodDefinition));

        System.setProperty("mcp.rate.bucketCapacity", "1");
        System.setProperty("mcp.rate.refillPerSecond", "0");

        registry.bind("mcpMethodCatalog", catalog);
        registry.bind("mcpToolsList", new McpToolsListProcessor(catalog));
        registry.bind("mcpRequestSizeGuard", new McpRequestSizeGuardProcessor());
        registry.bind("mcpRateLimit", new McpRateLimitProcessor());
        registry.bind("mcpHttpValidator", new McpHttpValidatorProcessor());
        registry.bind("mcpJsonRpcEnvelope", new McpJsonRpcEnvelopeProcessor());
        registry.bind("mcpSnowflakeError", new McpSnowflakeErrorProcessor());
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        if (port == 0) {
            port = AvailablePortFinder.getNextAvailable();
        }
        final int httpPort = port;
        return new RouteBuilder() {
            @Override
            public void configure() {
                getContext().getGlobalOptions().put("CamelJacksonEnableTypeConverter", "true");

                onException(Exception.class)
                        .handled(true)
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                        .process("mcpSnowflakeError");

                from("netty-http:http://0.0.0.0:" + httpPort + "/mcp?httpMethodRestrict=POST")
                        .routeId("mcp-entry-test")
                        .process("mcpRequestSizeGuard")
                        .process("mcpRateLimit")
                        .process("mcpHttpValidator")
                        .process(exchange -> {
                            String json = exchange.getMessage().getBody(String.class);
                            try {
                                Map<String, Object> payload = MAPPER.readValue(json, MAP_STRING_OBJECT);
                                exchange.getMessage().setBody(payload);
                            } catch (IOException e) {
                                throw new IllegalArgumentException("Unable to parse JSON payload", e);
                            }
                        })
                        .process("mcpJsonRpcEnvelope")
                        .choice()
                            .when(simple("${exchangeProperty.mcp.jsonrpc.method} == 'tools/list'"))
                                .process("mcpToolsList")
                            .otherwise()
                                .setProperty("mcp.error.message", constant("Unsupported MCP method"))
                                .throwException(new IllegalArgumentException("Unsupported MCP method"))
                        .end();
            }
        };
    }

    @Test
    void rateLimitExceededProducesJsonRpcErrorEnvelope() throws Exception {
        final String endpointUri = "netty-http:http://localhost:" + port + "/mcp";
        final String payload = "{" +
                "\"jsonrpc\":\"2.0\"," +
                "\"id\":\"req-1\"," +
                "\"method\":\"tools/list\"," +
                "\"params\":{}}";

        Map<String, Object> baseHeaders = new HashMap<>();
        baseHeaders.put(Exchange.HTTP_METHOD, "POST");
        baseHeaders.put(Exchange.CONTENT_TYPE, "application/json");
        baseHeaders.put("Accept", "application/json, text/event-stream");
        baseHeaders.put("MCP-Protocol-Version", "2025-06-18");

        Exchange first = template.request(endpointUri, exchange -> {
            exchange.getMessage().setBody(payload);
            exchange.getMessage().setHeaders(new HashMap<>(baseHeaders));
        });

        int firstCode = first.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, 200, int.class);
        if (firstCode != 200) {
            String initialBody = first.getMessage().getBody(String.class);
            Assertions.fail("Initial request failed with status " + firstCode + " and body: " + initialBody);
        }

        Exchange second = template.request(endpointUri, exchange -> {
            exchange.getMessage().setBody(payload);
            exchange.getMessage().setHeaders(new HashMap<>(baseHeaders));
        });

        int status = second.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, 0, int.class);
        Assertions.assertEquals(400, status);

        Map<String, Object> envelope = null;
        Object rawBody = second.getMessage().getBody();
        String rawText = second.getMessage().getBody(String.class);
        if (rawBody instanceof Map<?, ?>) {
            Map<?, ?> mapBody = (Map<?, ?>) rawBody;
            LinkedHashMap<String, Object> tmp = new LinkedHashMap<>();
            mapBody.forEach((key, value) -> tmp.put(String.valueOf(key), value));
            envelope = tmp;
        } else {
            if (rawText != null && rawText.trim().startsWith("{") && rawText.contains("\"")) {
                envelope = MAPPER.readValue(rawText, MAP_STRING_OBJECT);
            }
        }

        if (envelope != null) {
            Assertions.assertEquals("2.0", String.valueOf(envelope.get("jsonrpc")));
            @SuppressWarnings("unchecked")
            Map<String, Object> error = (Map<String, Object>) envelope.get("error");
            Assertions.assertNotNull(error, "error block should be present");
            Assertions.assertEquals(-32602, ((Number) error.get("code")).intValue());
            Assertions.assertTrue(String.valueOf(error.get("message")).toLowerCase().contains("rate limit exceeded"));
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) error.get("data");
            Assertions.assertNotNull(data, "error.data should be present");
            Assertions.assertEquals(400, ((Number) data.get("httpStatus")).intValue());
        } else {
            String envelopeText;
            if (rawText != null) {
                envelopeText = rawText;
            } else if (rawBody instanceof byte[] bytes) {
                envelopeText = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            } else {
                envelopeText = String.valueOf(rawBody);
            }
            Assertions.assertTrue(envelopeText.contains("code=-32602"), () -> "Unexpected envelope: " + envelopeText);
            Assertions.assertTrue(envelopeText.toLowerCase().contains("rate limit exceeded"), () -> "Unexpected envelope: " + envelopeText);
            Assertions.assertTrue(envelopeText.contains("httpStatus=400"), () -> "Unexpected envelope: " + envelopeText);
        }
    }
}

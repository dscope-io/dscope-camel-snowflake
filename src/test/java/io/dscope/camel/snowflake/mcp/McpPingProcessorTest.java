package io.dscope.camel.snowflake.mcp;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class McpPingProcessorTest {

    @Test
    void shouldRespondWithOkAndEcho() throws Exception {
        McpPingProcessor processor = new McpPingProcessor();
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            ObjectMapper mapper = new ObjectMapper();
            Exchange exchange = new DefaultExchange(ctx);
            exchange.setProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID, "ping-1");
            exchange.setProperty(McpHttpValidatorProcessor.EXCHANGE_PROTOCOL_VERSION, "2025-06-18");
            exchange.getIn().setBody(Map.of("sequence", 7));

            processor.process(exchange);

            String body = exchange.getIn().getBody(String.class);
            assertNotNull(body);

            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = mapper.readValue(body, Map.class);
            assertNotNull(envelope);
            assertEquals("2.0", envelope.get("jsonrpc"));
            assertEquals("ping-1", envelope.get("id"));
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) envelope.get("result");
            assertNotNull(result);
            assertEquals(Boolean.TRUE, result.get("ok"));
            assertTrue(result.containsKey("timestamp"));
            @SuppressWarnings("unchecked")
            Map<String, Object> echo = (Map<String, Object>) result.get("echo");
            assertNotNull(echo);
            assertEquals(7, echo.get("sequence"));

            assertEquals("2025-06-18", exchange.getIn().getHeader("MCP-Protocol-Version"));
            assertEquals("no-store", exchange.getIn().getHeader("Cache-Control"));
            assertEquals("application/json", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
            assertEquals(200, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        }
    }
}

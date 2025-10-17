package io.dscope.camel.snowflake.mcp;

import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dscope.camel.mcp.catalog.McpMethodDefinition;

@BindToRegistry("mcpSnowflakeError")
public class McpSnowflakeErrorProcessor implements Processor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void process(Exchange exchange) {
        Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        int httpCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, 0, Integer.class);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", determineErrorCode(throwable));
        error.put("message", throwable != null && throwable.getMessage() != null
                ? throwable.getMessage()
                : defaultMessage(httpCode));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "error");
        data.put("httpStatus", httpCode == 0 ? 500 : httpCode);
        if (throwable != null) {
            data.put("exception", throwable.getClass().getSimpleName());
        }
        McpMethodDefinition methodDefinition = exchange.getProperty("mcp.method.definition", McpMethodDefinition.class);
        if (methodDefinition != null) {
            data.put("method", methodDefinition.getName());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> requestSnapshot = exchange.getProperty("mcp.snowflake.request", Map.class);
        if (requestSnapshot != null) {
            data.put("request", requestSnapshot);
        }
        error.put("data", data);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("jsonrpc", "2.0");
        if (exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID) != null) {
            envelope.put("id", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID));
        } else {
            envelope.put("id", null);
        }
        envelope.put("error", error);

        writeJson(exchange, envelope);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        Object protocolVersion = exchange.getProperty(McpHttpValidatorProcessor.EXCHANGE_PROTOCOL_VERSION);
        if (protocolVersion == null) {
            protocolVersion = McpHttpValidatorProcessor.DEFAULT_PROTOCOL_VERSION;
        }
        exchange.getIn().setHeader("MCP-Protocol-Version", protocolVersion);
        exchange.getIn().setHeader("Cache-Control", "no-store");
    }

    private int determineErrorCode(Throwable throwable) {
        if (throwable == null) {
            return -32603; // internal
        }
        if (throwable instanceof JsonProcessingException) {
            return -32700; // parse error
        }
        if (throwable instanceof IllegalArgumentException iae) {
            String msg = iae.getMessage() == null ? "" : iae.getMessage().toLowerCase();
            if (msg.contains("conflicting authentication overrides")) {
                return -32010; // custom application error: auth conflict
            }
            return -32602; // invalid params
        }
        return -32603; // internal error
    }

    private String defaultMessage(int httpStatus) {
        return httpStatus >= 400 && httpStatus < 500 ? "Invalid MCP request" : "Unexpected server error";
    }

    static void writeJson(Exchange exchange, Object payload) {
        Objects.requireNonNull(exchange, "exchange");
        try {
            exchange.getIn().setBody(OBJECT_MAPPER.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize MCP response", e);
        }
    }    
}

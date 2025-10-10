package io.dscope.camel.snowflake.mcp;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

@BindToRegistry("mcpPing")
public class McpPingProcessor implements Processor {

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        if (exchange == null) {
            throw new IllegalArgumentException("Exchange must not be null");
        }

        Map<String, Object> params = exchange.getIn().getBody(Map.class);
        if (params == null) {
            params = Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", Boolean.TRUE);
        result.put("timestamp", Instant.now().toString());
        if (!params.isEmpty()) {
            result.put("echo", params);
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("jsonrpc", "2.0");
        envelope.put("id", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID));
        envelope.put("result", result);

        McpJsonWriter.writeJson(exchange, envelope);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        propagateProtocolVersion(exchange);
    }

    private void propagateProtocolVersion(Exchange exchange) {
        Object version = exchange.getProperty(McpHttpValidatorProcessor.EXCHANGE_PROTOCOL_VERSION);
        if (version == null) {
            version = McpHttpValidatorProcessor.DEFAULT_PROTOCOL_VERSION;
        }
        exchange.getIn().setHeader("MCP-Protocol-Version", version);
        exchange.getIn().setHeader("Cache-Control", "no-store");
    }
}

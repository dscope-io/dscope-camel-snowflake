package io.dscope.camel.snowflake.mcp;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpJsonRpcEnvelopeProcessorTest {

    @Test
    void shouldHandleInitializeRequest() throws Exception {
        McpJsonRpcEnvelopeProcessor processor = new McpJsonRpcEnvelopeProcessor();
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            Map<String, Object> params = Map.of("clientInfo", Map.of("name", "test", "version", "1.0"));
            exchange.getIn().setBody(Map.of(
                    "jsonrpc", "2.0",
                    "id", "init-1",
                    "method", "initialize",
                    "params", params));

            processor.process(exchange);

            assertEquals("REQUEST", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TYPE));
            assertEquals("initialize", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_METHOD));
            assertEquals("init-1", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID));
            assertSame(params, exchange.getIn().getBody());
        }
    }

    @Test
    void shouldDefaultInitializeParamsToEmptyMap() throws Exception {
        McpJsonRpcEnvelopeProcessor processor = new McpJsonRpcEnvelopeProcessor();
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            exchange.getIn().setBody(Map.of(
                    "jsonrpc", "2.0",
                    "id", 7,
                    "method", "initialize"));

            processor.process(exchange);

            assertEquals("REQUEST", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TYPE));
            assertEquals("initialize", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_METHOD));
            assertEquals(7, exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID));
            Object body = exchange.getIn().getBody();
            assertTrue(body instanceof Map);
            assertTrue(((Map<?, ?>) body).isEmpty());
        }
    }

    @Test
    void shouldHandlePingRequest() throws Exception {
        McpJsonRpcEnvelopeProcessor processor = new McpJsonRpcEnvelopeProcessor();
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            Map<String, Object> params = Map.of("sequence", 42);
            exchange.getIn().setBody(Map.of(
                    "jsonrpc", "2.0",
                    "id", "ping-1",
                    "method", "ping",
                    "params", params));

            processor.process(exchange);

            assertEquals("REQUEST", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TYPE));
            assertEquals("ping", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_METHOD));
            assertEquals("ping-1", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID));
            assertSame(params, exchange.getIn().getBody());
        }
    }
}

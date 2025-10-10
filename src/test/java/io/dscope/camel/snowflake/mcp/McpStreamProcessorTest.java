package io.dscope.camel.snowflake.mcp;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class McpStreamProcessorTest {

    @Test
    void shouldSetEventStreamHeaders() throws Exception {
        McpStreamProcessor processor = new McpStreamProcessor();
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);

            processor.process(exchange);

            assertEquals(200, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
            assertEquals("text/event-stream", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
            assertEquals("no-store", exchange.getIn().getHeader("Cache-Control"));
            assertEquals("keep-alive", exchange.getIn().getHeader("Connection"));
            String body = exchange.getIn().getBody(String.class);
            assertNotNull(body);
            assertTrue(body.contains("event: heartbeat"));
        }
    }
}

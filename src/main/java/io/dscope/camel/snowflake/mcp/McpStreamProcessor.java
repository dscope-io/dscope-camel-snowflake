package io.dscope.camel.snowflake.mcp;

import java.time.Instant;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Provides a minimal Server-Sent Events handshake body so MCP clients can
 * subscribe to the stream endpoint even if no events are emitted yet.
 */
@BindToRegistry("mcpStream")
public class McpStreamProcessor implements Processor {

    private static final String SSE_CONTENT_TYPE = "text/event-stream";

    @Override
    public void process(Exchange exchange) {
        if (exchange == null) {
            throw new IllegalArgumentException("Exchange must not be null");
        }

        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, SSE_CONTENT_TYPE);
        exchange.getIn().setHeader("Cache-Control", "no-store");
        exchange.getIn().setHeader("Connection", "keep-alive");
        exchange.getIn().setHeader("X-Accel-Buffering", "no");

    StringBuilder body = new StringBuilder();
    body.append(": stream established\n");
    body.append("event: heartbeat\n");
    body.append("data: \"");
    body.append(Instant.now());
    body.append('"');
    body.append('\n');
    body.append('\n');

        exchange.getIn().setBody(body.toString());
    }
}

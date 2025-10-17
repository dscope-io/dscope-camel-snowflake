package io.dscope.camel.snowflake.mcp;

import java.time.Instant;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;

/**
 * Delegates the core SSE setup to the shared processor while customising the initial body.
 */
@BindToRegistry("mcpStream")
public class McpStreamProcessor extends io.dscope.camel.mcp.processor.McpStreamProcessor {

    @Override
    public void process(Exchange exchange) {
        super.process(exchange);
        exchange.getIn().setHeader("X-Accel-Buffering", "no");
        exchange.getIn().setBody(buildHeartbeatPayload());
    }

    private String buildHeartbeatPayload() {
        StringBuilder body = new StringBuilder();
        body.append(": stream established\n");
        body.append("event: heartbeat\n");
        body.append("data: \"");
        body.append(Instant.now());
        body.append('\"');
        body.append('\n');
        body.append('\n');
        return body.toString();
    }
}

package io.dscope.camel.snowflake.mcp;

import org.apache.camel.BindToRegistry;

/**
 * Registers the shared rate limiter with the Snowflake registry.
 */
@BindToRegistry("mcpRateLimit")
public class McpRateLimitProcessor extends io.dscope.camel.mcp.processor.McpRateLimitProcessor {
}

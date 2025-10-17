package io.dscope.camel.snowflake.mcp;

import org.apache.camel.BindToRegistry;

/**
 * Registers the shared ping processor in the Snowflake component registry.
 */
@BindToRegistry("mcpPing")
public class McpPingProcessor extends io.dscope.camel.mcp.processor.McpPingProcessor {
}

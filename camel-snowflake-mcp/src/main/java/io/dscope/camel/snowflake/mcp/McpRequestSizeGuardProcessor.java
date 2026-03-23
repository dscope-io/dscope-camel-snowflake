package io.dscope.camel.snowflake.mcp;

import org.apache.camel.BindToRegistry;

/**
 * Uses the shared request size guard implementation.
 */
@BindToRegistry("mcpRequestSizeGuard")
public class McpRequestSizeGuardProcessor extends io.dscope.camel.mcp.processor.McpRequestSizeGuardProcessor {
}

package io.dscope.camel.snowflake.mcp;

import org.apache.camel.BindToRegistry;

/**
 * Exposes the shared JSON-RPC envelope processor so it can be referenced from the Snowflake routes.
 */
@BindToRegistry("mcpJsonRpcEnvelope")
public class McpJsonRpcEnvelopeProcessor extends io.dscope.camel.mcp.processor.McpJsonRpcEnvelopeProcessor {
}

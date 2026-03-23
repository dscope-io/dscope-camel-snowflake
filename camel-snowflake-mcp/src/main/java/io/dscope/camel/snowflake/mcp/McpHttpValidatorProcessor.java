package io.dscope.camel.snowflake.mcp;

import java.util.Optional;
import java.util.Set;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Message;

@BindToRegistry("mcpHttpValidator")
public class McpHttpValidatorProcessor extends io.dscope.camel.mcp.processor.McpHttpValidatorProcessor {

    public static final String SNOWFLAKE_PROTOCOL_VERSION_PROPERTY = "mcp.protocol.version";
    public static final String SNOWFLAKE_HTTP_PROTOCOL_PROPERTY =
        io.dscope.camel.mcp.processor.McpHttpValidatorProcessor.EXCHANGE_PROTOCOL_VERSION;

    private static final Set<String> LEGACY_PROTOCOL_VERSIONS = Set.of("2025-03-26", "2024-11-05");

    @Override
    public void process(Exchange exchange) {
        if (exchange == null) {
            throw new IllegalArgumentException("Exchange must not be null");
        }

        Message in = exchange.getIn();
        Optional<String> normalized = normalize(in.getHeader("MCP-Protocol-Version", String.class));
        String originalVersion = normalized.orElse(null);
        boolean legacyVersionRequested = originalVersion != null && LEGACY_PROTOCOL_VERSIONS.contains(originalVersion);

        if (legacyVersionRequested) {
            in.setHeader("MCP-Protocol-Version", baseDefaultProtocol());
        }

        super.process(exchange);

        String negotiated = exchange.getProperty(SNOWFLAKE_HTTP_PROTOCOL_PROPERTY, String.class);
        if (legacyVersionRequested) {
            negotiated = originalVersion;
            exchange.setProperty(SNOWFLAKE_HTTP_PROTOCOL_PROPERTY, negotiated);
            in.setHeader("MCP-Protocol-Version", negotiated);
        } else if (negotiated == null || negotiated.isBlank()) {
            negotiated = baseDefaultProtocol();
        }

        exchange.setProperty(SNOWFLAKE_PROTOCOL_VERSION_PROPERTY, negotiated);
        exchange.getIn().setHeader("Cache-Control", "no-store");
    }

    private Optional<String> normalize(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    private static String baseDefaultProtocol() {
        return io.dscope.camel.mcp.processor.McpHttpValidatorProcessor.DEFAULT_PROTOCOL_VERSION;
    }
}

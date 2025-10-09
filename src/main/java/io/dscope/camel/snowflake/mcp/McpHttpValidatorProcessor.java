package io.dscope.camel.snowflake.mcp;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
@BindToRegistry("mcpHttpValidator")
public class McpHttpValidatorProcessor implements Processor {

    public static final String EXCHANGE_PROTOCOL_VERSION = "mcp.protocol.version";
    public static final String DEFAULT_PROTOCOL_VERSION = "2025-06-18";
    private static final Set<String> SUPPORTED_VERSIONS = Set.of("2025-06-18", "2025-03-26", "2024-11-05");

    @Override
    public void process(Exchange exchange) {
        if (exchange == null) {
            throw new IllegalArgumentException("Exchange must not be null");
        }

        Message in = exchange.getIn();
        String accept = in.getHeader("Accept", String.class);
        if (!containsAllMediaTypes(accept, "application/json", "text/event-stream")) {
            throw new IllegalArgumentException(
                "Accept header must include application/json and text/event-stream for MCP Streamable HTTP transport");
        }

        String contentType = in.getHeader("Content-Type", String.class);
        if (!containsAnyMediaType(contentType, "application/json")) {
            throw new IllegalArgumentException("Content-Type must be application/json for MCP requests");
        }

        String protocolVersion = normalize(in.getHeader("MCP-Protocol-Version", String.class))
                .filter(SUPPORTED_VERSIONS::contains)
                .orElse(DEFAULT_PROTOCOL_VERSION);
        exchange.setProperty(EXCHANGE_PROTOCOL_VERSION, protocolVersion);
    }

    private boolean containsAllMediaTypes(String headerValue, String... mediaTypes) {
        if (headerValue == null || headerValue.isBlank()) {
            return false;
        }
        return Arrays.stream(mediaTypes).allMatch(type -> containsAnyMediaType(headerValue, type));
    }

    private boolean containsAnyMediaType(String headerValue, String mediaType) {
        if (headerValue == null || headerValue.isBlank()) {
            return false;
        }
        return streamValues(headerValue)
                .anyMatch(value -> matchesMediaType(value, mediaType));
    }

    private Stream<String> streamValues(String headerValue) {
        return Arrays.stream(headerValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(value -> value.contains(";") ? value.substring(0, value.indexOf(';')).trim() : value)
                .map(value -> value.toLowerCase(Locale.ROOT));
    }

    private boolean matchesMediaType(String candidate, String required) {
        if (candidate.equals(required.toLowerCase(Locale.ROOT)) || candidate.equals("*/*")) {
            return true;
        }
        int slashIndex = candidate.indexOf('/');
        if (slashIndex <= 0) {
            return false;
        }
        String candidateType = candidate.substring(0, slashIndex);
        String candidateSubtype = candidate.substring(slashIndex + 1);
        int requiredSlash = required.indexOf('/');
        if (requiredSlash <= 0) {
            return false;
        }
        String requiredType = required.substring(0, requiredSlash).toLowerCase(Locale.ROOT);
        String requiredSubtype = required.substring(requiredSlash + 1).toLowerCase(Locale.ROOT);
        return (candidateType.equals(requiredType) && (candidateSubtype.equals(requiredSubtype) || candidateSubtype.equals("*")));
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
}

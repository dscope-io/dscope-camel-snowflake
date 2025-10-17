package io.dscope.camel.snowflake.mcp;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dscope.camel.mcp.catalog.McpMethodCatalog;
import io.dscope.camel.mcp.catalog.McpMethodDefinition;
import io.dscope.camel.snowflake.SnowflakeConstants;
import io.dscope.camel.mcp.processor.AbstractMcpRequestProcessor;

@BindToRegistry("mcpSnowflakeRequest")
public class McpSnowflakeRequestProcessor extends AbstractMcpRequestProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> RESERVED_PARAMETER_KEYS = Set.of(
        "query",
        "connection",
        "enableparameterbinding");

    private final McpMethodCatalog methodCatalog;
    private final Map<String, String> queries = new LinkedHashMap<>();
    private String query;
    private Boolean defaultEnableParameterBinding = Boolean.TRUE;

    public McpSnowflakeRequestProcessor() {
        this(new McpMethodCatalog());
    }

    public McpSnowflakeRequestProcessor(McpMethodCatalog methodCatalog) {
        this.methodCatalog = methodCatalog;
    }

    @Override
    protected void handleRequest(Exchange exchange, Map<String, Object> payload) throws Exception {
        String toolName = getToolName(exchange);
        McpMethodDefinition methodDefinition = methodCatalog.findByName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + toolName));
        exchange.setProperty("mcp.method.definition", methodDefinition);

        if (payload == null) {
            throw new IllegalArgumentException("Tool arguments must be a JSON object");
        }

        String resolvedQuery = resolveConfiguredQuery(methodDefinition.getName());
        if (resolvedQuery == null || resolvedQuery.isBlank()) {
            throw new IllegalStateException("No query configured for tool: " + methodDefinition.getName());
        }
        exchange.getIn().setHeader(SnowflakeConstants.HEADER_QUERY, resolvedQuery);

        boolean enableBinding = resolveBoolean(payload.get("enableParameterBinding"),
            defaultEnableParameterBinding == null ? true : defaultEnableParameterBinding);
        exchange.getIn().setHeader(SnowflakeConstants.HEADER_ENABLE_PARAMETER_BINDING, enableBinding);

        Map<String, Object> parameterSnapshot = new LinkedHashMap<>();
        Object parametersNode = Optional.ofNullable(payload.get("parameters"))
                .orElseGet(() -> Optional.ofNullable(payload.get("arguments"))
                        .orElse(payload));
        if (parametersNode instanceof Map<?, ?> parameterMap) {
            for (Map.Entry<?, ?> entry : parameterMap.entrySet()) {
                String key = entry.getKey() == null ? null : entry.getKey().toString();
                if (key == null) {
                    continue;
                }
                String trimmedKey = key.trim();
                if (trimmedKey.isEmpty()) {
                    continue;
                }
                if (isReservedParameterKey(trimmedKey)) {
                    continue;
                }
                Object sanitized = sanitizeParameterValue(entry.getValue());
                parameterSnapshot.put(trimmedKey, sanitized);
                exchange.getIn().setHeader("snowflake." + trimmedKey, sanitized);
            }
        }

        Map<String, Object> connectionSnapshot = new LinkedHashMap<>();
        Object connectionNode = payload.get("connection");
        if (connectionNode instanceof Map<?, ?> connectionMap) {
            applyConnectionOverrides(exchange, connectionSnapshot, (Map<?, ?>) connectionMap);
        }

        // Allow top-level overrides without wrapping in "connection"
        applyConnectionOverrides(exchange, connectionSnapshot, payload);

        // Validate incompatible authentication combinations after both passes
        boolean hasPassword = exchange.getIn().getHeader(SnowflakeConstants.HEADER_PASSWORD) != null;
        boolean hasPrivateKey = exchange.getIn().getHeader(SnowflakeConstants.HEADER_PRIVATE_KEY) != null
                || exchange.getIn().getHeader(SnowflakeConstants.HEADER_PRIVATE_KEY_FILE) != null;
        boolean hasOauth = exchange.getIn().getHeader(SnowflakeConstants.HEADER_OAUTH_TOKEN) != null;

        if (hasOauth && (hasPassword || hasPrivateKey)) {
            throw new IllegalArgumentException("Conflicting authentication overrides: oauthToken cannot be combined with password or private key authentication");
        }
        if (hasPassword && hasPrivateKey) {
            throw new IllegalArgumentException("Conflicting authentication overrides: password cannot be combined with private key authentication");
        }

        Map<String, Object> requestSnapshot = new LinkedHashMap<>();
    requestSnapshot.put("tool", methodDefinition.getName());
    requestSnapshot.put("query", resolvedQuery);
        requestSnapshot.put("enableParameterBinding", enableBinding);
        requestSnapshot.put("parameters", parameterSnapshot);
        if (!connectionSnapshot.isEmpty()) {
            requestSnapshot.put("connection", connectionSnapshot);
        }
        validateRequiredArguments(methodDefinition, parameterSnapshot);
        exchange.setProperty("mcp.snowflake.request", requestSnapshot);
    }

    public Map<String, String> getQueries() {
        return queries;
    }

    public void setQueries(Map<String, String> queries) {
        this.queries.clear();
        if (queries != null) {
            queries.forEach((key, value) -> {
                if (key == null || value == null) {
                    return;
                }
                String trimmedKey = key.trim();
                String trimmedValue = value.trim();
                if (trimmedKey.isEmpty() || trimmedValue.isEmpty()) {
                    return;
                }
                this.queries.put(trimmedKey, trimmedValue);
            });
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        if (query == null) {
            this.query = null;
            return;
        }
        String trimmed = query.trim();
        this.query = trimmed.isEmpty() ? null : trimmed;
    }

    public Boolean getEnableParameterBinding() {
        return defaultEnableParameterBinding;
    }

    public void setEnableParameterBinding(Boolean enableParameterBinding) {
        this.defaultEnableParameterBinding = enableParameterBinding;
    }

    private String resolveConfiguredQuery(String methodName) {
        String configured = null;
        if (methodName != null) {
            configured = queries.get(methodName);
            if (configured != null) {
                configured = configured.trim();
                if (configured.isEmpty()) {
                    configured = null;
                }
            }
        }
        if (configured == null) {
            configured = query;
        }
        return configured;
    }

    private void validateRequiredArguments(McpMethodDefinition definition, Map<String, Object> provided) {
        List<String> required = definition.getRequiredArguments();
        if (required.isEmpty()) {
            return;
        }
        List<String> missing = required.stream()
                .filter(name -> isMissingRequired(name, provided))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required argument(s): " + String.join(", ", missing));
        }
    }

    private boolean isMissingRequired(String name, Map<String, Object> provided) {
        Object value = provided.get(name);
        return value == null || value.toString().isBlank();
    }

    private boolean isReservedParameterKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.trim().toLowerCase();
        return RESERVED_PARAMETER_KEYS.contains(normalized);
    }

    private void applyConnectionOverrides(Exchange exchange, Map<String, Object> accumulator, Map<?, ?> source) {
        source.forEach((rawKey, rawValue) -> {
            String key = rawKey == null ? null : rawKey.toString();
            if (key == null || rawValue == null) {
                return;
            }

            String trimmedKey = key.trim();
            String trimmedValue = rawValue.toString().trim();
            if (trimmedValue.isEmpty()) {
                return;
            }

            switch (trimmedKey) {
                case "account" -> setOverride(exchange, accumulator, SnowflakeConstants.HEADER_ACCOUNT, trimmedKey, trimmedValue);
                case "username" -> setOverride(exchange, accumulator, SnowflakeConstants.HEADER_USERNAME, trimmedKey, trimmedValue);
                case "password" -> setOverrideMasked(exchange, accumulator, SnowflakeConstants.HEADER_PASSWORD, trimmedKey, trimmedValue);
                case "privateKey", "private_key" -> setOverrideMasked(exchange, accumulator, SnowflakeConstants.HEADER_PRIVATE_KEY, trimmedKey, trimmedValue);
                case "privateKeyFile", "private_key_file" -> setOverrideMasked(exchange, accumulator, SnowflakeConstants.HEADER_PRIVATE_KEY_FILE, trimmedKey, trimmedValue);
                case "privateKeyFilePassword", "private_key_file_password" -> setOverrideMasked(exchange, accumulator, SnowflakeConstants.HEADER_PRIVATE_KEY_FILE_PASSWORD, trimmedKey, trimmedValue);
                case "oauthToken", "oauth_token" -> setOverrideMasked(exchange, accumulator, SnowflakeConstants.HEADER_OAUTH_TOKEN, trimmedKey, trimmedValue);
                case "database" -> setOverride(exchange, accumulator, SnowflakeConstants.HEADER_DATABASE, trimmedKey, trimmedValue);
                case "schema" -> setOverride(exchange, accumulator, SnowflakeConstants.HEADER_SCHEMA, trimmedKey, trimmedValue);
                case "warehouse" -> setOverride(exchange, accumulator, SnowflakeConstants.HEADER_WAREHOUSE, trimmedKey, trimmedValue);
                case "role" -> setOverride(exchange, accumulator, SnowflakeConstants.HEADER_ROLE, trimmedKey, trimmedValue);
                case "authenticator" -> setOverride(exchange, accumulator, SnowflakeConstants.HEADER_AUTHENTICATOR, trimmedKey, trimmedValue);
                case "parameterPrefix", "parameter_prefix" -> setOverride(exchange, accumulator, SnowflakeConstants.HEADER_PARAMETER_PREFIX, trimmedKey, trimmedValue);
                default -> {
                    // ignore unrecognised keys
                }
            }
        });
    }

    private void setOverride(Exchange exchange, Map<String, Object> accumulator, String header, String key, Object value) {
        exchange.getIn().setHeader(header, value);
        accumulator.put(key, value);
    }

    private void setOverrideMasked(Exchange exchange, Map<String, Object> accumulator, String header, String key, Object value) {
        exchange.getIn().setHeader(header, value);
        accumulator.put(key, "***");
    }

    private Object sanitizeParameterValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            try {
                return OBJECT_MAPPER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                return value.toString();
            }
        }
        if (value.getClass().isArray()) {
            return value.toString();
        }
        return Objects.toString(value);
    }

    private boolean resolveBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(text);
    }
}

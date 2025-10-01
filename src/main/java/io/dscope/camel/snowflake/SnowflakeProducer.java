package io.dscope.camel.snowflake;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dscope.camel.snowflake.jdbc.SnowflakeJdbcConnectionManager;
import io.dscope.camel.snowflake.sql.SqlParameterBinder;

/**
 * Snowflake producer that executes SQL queries with parameter binding support.
 */
public class SnowflakeProducer extends DefaultProducer {
    
    private static final Logger LOG = LoggerFactory.getLogger(SnowflakeProducer.class);
    
    private final SnowflakeConfiguration configuration;
    
    public SnowflakeProducer(SnowflakeEndpoint endpoint) {
        super(endpoint);
        this.configuration = endpoint.getConfiguration();
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        // Build effective configuration: configuration -> endpoint params -> headers
        SnowflakeConfiguration effectiveConfig = applyDynamicOverrides(exchange);

        // Get SQL query from configuration or message body
        String sql = getSqlQuery(exchange, effectiveConfig);
        
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("No SQL query provided. Set query in endpoint configuration or message body.");
        }
        
        LOG.info("Processing Snowflake query: {}", sql);
        
        // Bind parameters if enabled
        SqlParameterBinder.ParameterBindingResult bindingResult = null;
        if (effectiveConfig.isEnableParameterBinding()) {
            bindingResult = SqlParameterBinder.bindParameters(sql, exchange, effectiveConfig.getParameterPrefix());
            sql = bindingResult.getProcessedSql();
            
            // Set binding information in headers for debugging/monitoring
            exchange.getIn().setHeader("CamelSnowflakeBoundParameters", bindingResult.getBoundParameters());
            exchange.getIn().setHeader("CamelSnowflakeUnboundParameters", bindingResult.getUnboundParameters());
            exchange.getIn().setHeader("CamelSnowflakeParameterCount", bindingResult.getBoundParameterCount());
        } else {
            // Even when binding is disabled, set diagnostic headers to sensible defaults
            exchange.getIn().setHeader("CamelSnowflakeBoundParameters", java.util.Collections.emptyMap());
            exchange.getIn().setHeader("CamelSnowflakeUnboundParameters", java.util.Collections.emptyMap());
            exchange.getIn().setHeader("CamelSnowflakeParameterCount", 0);
        }
        
        // Execute query
    try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(effectiveConfig)) {
            
            if (bindingResult != null && bindingResult.getBoundParameterCount() > 0) {
                // Use prepared statement with parameters
                executeParameterizedQuery(connection, sql, bindingResult, exchange);
            } else {
                // Use simple statement
                executeSimpleQuery(connection, sql, exchange);
            }
            
        } catch (Exception e) {
            LOG.error("Error executing Snowflake query: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Execute query with bound parameters using PreparedStatement.
     */
    private void executeParameterizedQuery(Connection connection, String sql, 
                                         SqlParameterBinder.ParameterBindingResult bindingResult, 
                                         Exchange exchange) throws Exception {
        
        LOG.info("Executing parameterized query: {} with {} parameters", sql, bindingResult.getBoundParameterCount());
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            
            // Bind parameters
            Object[] paramValues = bindingResult.getParameterValues();
            for (int i = 0; i < paramValues.length; i++) {
                statement.setObject(i + 1, paramValues[i]);
                LOG.debug("Parameter {}: {}", i + 1, paramValues[i]);
            }
            
            // Execute query
            if (isSelectQuery(sql)) {
                ResultSet resultSet = statement.executeQuery();
                List<Map<String, Object>> results = processResultSet(resultSet);
                writeResults(exchange, results);
            } else {
                int updateCount = statement.executeUpdate();
                exchange.getIn().setBody(updateCount);
                exchange.getIn().setHeader("CamelSnowflakeUpdateCount", updateCount);
            }
        }
    }
    
    /**
     * Execute simple query without parameters.
     */
    private void executeSimpleQuery(Connection connection, String sql, Exchange exchange) throws Exception {
        LOG.info("Executing simple query: {}", sql);
        
        try (var statement = connection.createStatement()) {
            if (isSelectQuery(sql)) {
                ResultSet resultSet = statement.executeQuery(sql);
                List<Map<String, Object>> results = processResultSet(resultSet);
                writeResults(exchange, results);
            } else {
                int updateCount = statement.executeUpdate(sql);
                exchange.getIn().setBody(updateCount);
                exchange.getIn().setHeader("CamelSnowflakeUpdateCount", updateCount);
            }
        }
    }
    
    /**
     * Get SQL query from configuration or message body.
     */
    private String getSqlQuery(Exchange exchange, SnowflakeConfiguration cfg) {
        // Prefer message body if it looks like SQL; otherwise fall back to endpoint/config query
        String bodyQuery = exchange.getIn().getBody(String.class);
        if (bodyQuery != null && !bodyQuery.trim().isEmpty()) {
            if (isLikelySql(bodyQuery)) {
                return bodyQuery;
            }
            // Body is non-empty but not SQL (e.g., a description string); use configured query if present
            if (cfg.getQuery() != null && !cfg.getQuery().isBlank()) {
                return cfg.getQuery();
            }
            return bodyQuery; // as last resort
        }

        // Fall back to configuration query
        return cfg.getQuery();
    }

    private boolean isLikelySql(String s) {
        String u = s.trim().toUpperCase();
        return u.startsWith("SELECT") || u.startsWith("INSERT") || u.startsWith("UPDATE") ||
               u.startsWith("DELETE") || u.startsWith("CREATE") || u.startsWith("DROP") ||
               u.startsWith("ALTER")  || u.startsWith("WITH")   || u.startsWith("CALL") ||
               u.startsWith("MERGE")  || u.startsWith("TRUNCATE") || u.startsWith("DESC") ||
               u.startsWith("DESCRIBE") || u.startsWith("SHOW");
    }
    
    /**
     * Check if query is a SELECT statement.
     */
    private boolean isSelectQuery(String sql) {
        return sql.trim().toUpperCase().startsWith("SELECT") || 
               sql.trim().toUpperCase().startsWith("SHOW") ||
               sql.trim().toUpperCase().startsWith("DESCRIBE") ||
               sql.trim().toUpperCase().startsWith("WITH");
    }
    
    /**
     * Process ResultSet into List of Maps.
     */
    private List<Map<String, Object>> processResultSet(ResultSet resultSet) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }
        
        LOG.info("Query returned {} rows", results.size());
        return results;
    }

    private void writeResults(Exchange exchange, List<Map<String, Object>> results) throws Exception {
        exchange.getIn().setHeader("CamelSnowflakeRowCount", results.size());
        String fmt = configuration.getOutputFormat();
        if (fmt == null || fmt.isBlank() || fmt.equalsIgnoreCase("rows")) {
            exchange.getIn().setBody(results);
            return;
        }

        if (fmt.equalsIgnoreCase("json")) {
            // Serialize to JSON
            Object mapper = getObjectMapper();
            String json = (String) mapper.getClass().getMethod("writeValueAsString", Object.class).invoke(mapper, results);
            exchange.getIn().setBody(json);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
            return;
        }

        if (fmt.equalsIgnoreCase("xml")) {
            // Basic XML serialization: <rows><row><col>value</col>...</row>...</rows>
            StringBuilder sb = new StringBuilder();
            sb.append("<rows>");
            for (Map<String, Object> row : results) {
                sb.append("<row>");
                for (Map.Entry<String, Object> e : row.entrySet()) {
                    sb.append('<').append(escapeXmlName(e.getKey())).append('>')
                      .append(escapeXml(String.valueOf(e.getValue())))
                      .append("</").append(escapeXmlName(e.getKey())).append('>');
                }
                sb.append("</row>");
            }
            sb.append("</rows>");
            exchange.getIn().setBody(sb.toString());
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/xml");
            return;
        }

        // Fallback
        exchange.getIn().setBody(results);
    }

    private Object getObjectMapper() throws Exception {
        try {
            Class<?> clazz = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JSON output requires Jackson (com.fasterxml.jackson.core:jackson-databind) on the classpath.");
        }
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escapeXmlName(String s) {
        if (s == null || s.isBlank()) return "col";
        // very basic name sanitization; XML names cannot start with digits etc.
    String t = s.replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (!Character.isLetter(t.charAt(0)) && t.charAt(0) != '_') {
            t = "n_" + t;
        }
        return t;
    }

    @Override
    public SnowflakeEndpoint getEndpoint() {
        return (SnowflakeEndpoint) super.getEndpoint();
    }

    /**
     * Apply dynamic overrides from headers to a copy of the base configuration.
     * This avoids mutating the endpoint-scoped configuration shared across exchanges.
     */
    private SnowflakeConfiguration applyDynamicOverrides(Exchange exchange) {
        SnowflakeConfiguration copy = configuration.copy();
        // First overlay endpoint parameter values (may differ from underlying configuration if changed late)
        SnowflakeEndpoint ep = getEndpoint();
    copy.overlayFromEndpoint(ep);
        var in = exchange.getIn();

        // Utility to apply string header if present & not blank
        java.util.function.BiConsumer<String, java.util.function.Consumer<String>> apply = (header, setter) -> {
            Object val = in.getHeader(header);
            if (val != null) {
                String s = val.toString();
                if (!s.isBlank()) {
                    setter.accept(s);
                }
            }
        };

        apply.accept(SnowflakeConstants.HEADER_ACCOUNT, copy::setAccount);
        apply.accept(SnowflakeConstants.HEADER_USERNAME, copy::setUsername);
        apply.accept(SnowflakeConstants.HEADER_PASSWORD, copy::setPassword);
        apply.accept(SnowflakeConstants.HEADER_PRIVATE_KEY, copy::setPrivateKey);
        apply.accept(SnowflakeConstants.HEADER_DATABASE, copy::setDatabase);
        apply.accept(SnowflakeConstants.HEADER_SCHEMA, copy::setSchema);
        apply.accept(SnowflakeConstants.HEADER_WAREHOUSE, copy::setWarehouse);
        apply.accept(SnowflakeConstants.HEADER_ROLE, copy::setRole);
        apply.accept(SnowflakeConstants.HEADER_QUERY, copy::setQuery);
        apply.accept(SnowflakeConstants.HEADER_OPERATION, copy::setOperation);
        apply.accept(SnowflakeConstants.HEADER_JDBC_URL, copy::setJdbcUrl);
        apply.accept(SnowflakeConstants.HEADER_AUTHENTICATOR, copy::setAuthenticator);
        apply.accept(SnowflakeConstants.HEADER_PARAMETER_PREFIX, copy::setParameterPrefix);

        Object enableBinding = in.getHeader(SnowflakeConstants.HEADER_ENABLE_PARAMETER_BINDING);
        if (enableBinding instanceof Boolean b) {
            copy.setEnableParameterBinding(b);
        } else if (enableBinding != null) {
            copy.setEnableParameterBinding(Boolean.parseBoolean(enableBinding.toString()));
        }

        // Warn if both password and private key were supplied after overrides (private key wins downstream)
        if (LOG.isWarnEnabled()) {
            if (copy.getPrivateKey() != null && !copy.getPrivateKey().isBlank() &&
                copy.getPassword() != null && !copy.getPassword().isBlank()) {
                LOG.warn("Both password and privateKey provided; privateKey authentication (JWT) will be used and password ignored");
            }
        }

        // Record effective configuration for downstream traceability
        in.setHeader("CamelSnowflakeEffectiveAccount", copy.getAccount());
        in.setHeader("CamelSnowflakeEffectiveDatabase", copy.getDatabase());
        in.setHeader("CamelSnowflakeEffectiveSchema", copy.getSchema());
        in.setHeader("CamelSnowflakeEffectiveWarehouse", copy.getWarehouse());
        in.setHeader("CamelSnowflakeEffectiveRole", copy.getRole());
        in.setHeader("CamelSnowflakeEffectiveOperation", copy.getOperation());
        in.setHeader("CamelSnowflakeEffectiveQuery", copy.getQuery());
        in.setHeader("CamelSnowflakeEffectiveAuthenticator", copy.getAuthenticator());
        return copy;
    }
}
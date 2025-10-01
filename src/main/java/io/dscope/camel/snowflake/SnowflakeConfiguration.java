package io.dscope.camel.snowflake;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration class for Snowflake component.
 */
@UriParams
public class SnowflakeConfiguration {

    @UriParam(description = "Snowflake account URL")
    private String account;

    @UriParam(description = "Snowflake username")
    private String username;

    @UriParam(description = "Snowflake password", secret = true)
    private String password;

    @UriParam(description = "Snowflake database name")
    private String database;

    @UriParam(description = "Snowflake schema name")
    private String schema;

    @UriParam(description = "Snowflake warehouse name")
    private String warehouse;

    @UriParam(description = "Snowflake role")
    private String role;

    @UriParam(description = "SQL query to execute", defaultValue = "")
    private String query = "";

    @UriParam(description = "Table name for operations")
    private String table;

    @UriParam(description = "Operation type: select, insert, update, delete", defaultValue = "select")
    private String operation = "select";

    // Getters and Setters
    public String getAccount() {
        if (notBlank(account)) return account;
        String v = System.getProperty("snowflake.account");
        if (notBlank(v)) this.account = v;
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUsername() {
        if (notBlank(username)) return username;
        String v = System.getProperty("snowflake.username");
        if (notBlank(v)) this.username = v;
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        if (notBlank(password)) return password;
        String v = System.getProperty("snowflake.password");
        if (notBlank(v)) this.password = v;
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        if (notBlank(database)) return database;
        String v = System.getProperty("snowflake.database");
        if (notBlank(v)) this.database = v;
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchema() {
        if (notBlank(schema)) return schema;
        String v = System.getProperty("snowflake.schema");
        if (notBlank(v)) this.schema = v;
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getWarehouse() {
        if (notBlank(warehouse)) return warehouse;
        String v = System.getProperty("snowflake.warehouse");
        if (notBlank(v)) this.warehouse = v;
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getRole() {
        if (notBlank(role)) return role;
        String v = System.getProperty("snowflake.role");
        if (notBlank(v)) this.role = v;
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getQuery() {
        if (notBlank(query)) return query;
        String v = System.getProperty("snowflake.query");
        if (notBlank(v)) this.query = v;
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTable() {
        if (notBlank(table)) return table;
        String v = System.getProperty("snowflake.table");
        if (notBlank(v)) this.table = v;
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getOperation() {
        if (notBlank(operation)) return operation;
        String v = System.getProperty("snowflake.operation");
        if (notBlank(v)) this.operation = v;
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    @UriParam(description = "Custom JDBC URL override for testing")
    private String jdbcUrl;

        @UriParam(description = "Private key for key-pair authentication (Base64 encoded)", secret = true)
    private String privateKey;

    @UriParam(description = "Path to a PKCS#8 private key file (PEM or DER). Used if 'privateKey' is not provided.")
    private String privateKeyFile;

    @UriParam(description = "Enable parameter binding for SQL queries (:#paramName syntax)", defaultValue = "true")
    private boolean enableParameterBinding = true;

    @UriParam(description = "Header prefix for parameter binding", defaultValue = "snowflake.")
    private String parameterPrefix = "snowflake.";

    @UriParam(description = "Snowflake authenticator (e.g., snowflake, snowflake_jwt, externalbrowser, oauth)", defaultValue = "snowflake")
    private String authenticator = "snowflake";

    @UriParam(description = "Output format of the message body: rows | json | xml | arrow (driver). 'rows' returns List<Map<String,Object>>; 'json' returns a JSON string; 'xml' returns an XML string. Also controls JDBC result format: 'arrow' uses Arrow; others use JSON to avoid JVM --add-opens.", defaultValue = "rows")
    private String outputFormat; // default resolved lazily to 'rows' to allow system property override

    @UriParam(description = "OAuth access token used when authenticator=oauth", secret = true)
    private String token;

    // Arbitrary Snowflake JDBC driver parameters passed via URI as jdbc.<NAME>=<value>
    // Example: jdbc.ROLE=myrole (Note: for built-ins we already map via dedicated fields)
    @UriParam(prefix = "jdbc.", description = "Additional Snowflake JDBC driver parameters to append to the JDBC URL")
    private Map<String, String> jdbcParameters = new LinkedHashMap<>();

    public String getJdbcUrl() {
        if (notBlank(jdbcUrl)) return jdbcUrl;
        String v = System.getProperty("snowflake.jdbcUrl");
        if (notBlank(v)) this.jdbcUrl = v;
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getPrivateKey() {
        if (notBlank(privateKey)) return privateKey;
        String v = System.getProperty("snowflake.privateKey");
        if (notBlank(v)) this.privateKey = v;
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPrivateKeyFile() {
        if (notBlank(privateKeyFile)) return privateKeyFile;
        String v = System.getProperty("snowflake.privateKeyFile");
        if (notBlank(v)) this.privateKeyFile = v;
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public boolean isEnableParameterBinding() {
        // System property override if provided
        String v = System.getProperty("snowflake.enableParameterBinding");
        if (v != null && !v.isBlank()) {
            this.enableParameterBinding = Boolean.parseBoolean(v);
        }
        return enableParameterBinding;
    }

    public void setEnableParameterBinding(boolean enableParameterBinding) {
        this.enableParameterBinding = enableParameterBinding;
    }

    public String getParameterPrefix() {
        if (notBlank(parameterPrefix)) return parameterPrefix;
        String v = System.getProperty("snowflake.parameterPrefix");
        if (notBlank(v)) this.parameterPrefix = v;
        return parameterPrefix;
    }

    public void setParameterPrefix(String parameterPrefix) {
        this.parameterPrefix = parameterPrefix;
    }

    public String getAuthenticator() {
        if (notBlank(authenticator)) return authenticator;
        String v = System.getProperty("snowflake.authenticator");
        if (notBlank(v)) this.authenticator = v;
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public String getOutputFormat() {
        // Prefer explicit system property if present
        String sys = System.getProperty("snowflake.outputFormat");
        if (notBlank(sys)) {
            this.outputFormat = sys;
            return outputFormat;
        }
        // If endpoint explicitly set a value, use it; otherwise fall back to default 'rows'
        if (notBlank(outputFormat)) {
            return outputFormat;
        }
        this.outputFormat = "rows";
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getToken() {
        if (notBlank(token)) return token;
        String v = System.getProperty("snowflake.token");
        if (notBlank(v)) this.token = v;
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Map<String, String> getJdbcParameters() {
        // Merge any system properties starting with snowflake.jdbc. as pass-through JDBC parameters
        if (jdbcParameters == null) {
            jdbcParameters = new LinkedHashMap<>();
        }
        for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (k instanceof String && v instanceof String) {
                String key = (String) k;
                if (key.startsWith("snowflake.jdbc.")) {
                    String param = key.substring("snowflake.jdbc.".length());
                    // Do not override if already explicitly provided
                    jdbcParameters.putIfAbsent(param, (String) v);
                }
            }
        }
        return jdbcParameters;
    }

    public void setJdbcParameters(Map<String, String> jdbcParameters) {
        this.jdbcParameters = (jdbcParameters != null) ? new LinkedHashMap<>(jdbcParameters) : new LinkedHashMap<>();
    }

    /**
     * Create a shallow copy of this configuration so per-exchange overrides can be applied
     * without mutating the shared endpoint configuration (which may be accessed concurrently).
     */
    public SnowflakeConfiguration copy() {
        SnowflakeConfiguration c = new SnowflakeConfiguration();
        c.account = this.account;
        c.username = this.username;
        c.password = this.password;
        c.database = this.database;
        c.schema = this.schema;
        c.warehouse = this.warehouse;
        c.role = this.role;
        c.query = this.query;
        c.table = this.table;
        c.operation = this.operation;
        c.jdbcUrl = this.jdbcUrl;
        c.privateKey = this.privateKey;
    c.privateKeyFile = this.privateKeyFile;
        c.enableParameterBinding = this.enableParameterBinding;
        c.parameterPrefix = this.parameterPrefix;
        c.authenticator = this.authenticator;
        c.outputFormat = this.outputFormat;
        c.token = this.token;
        if (this.jdbcParameters != null) {
            c.jdbcParameters = new LinkedHashMap<>(this.jdbcParameters);
        }
        return c;
    }

    /**
     * Overlay non-null / non-blank endpoint parameter values onto this configuration.
     * Endpoint fields reflect URI-specified values which should have higher precedence
     * than the base (component-level) configuration, but still be overridable by headers.
     */
    public void overlayFromEndpoint(SnowflakeEndpoint ep) {
        if (ep == null) return;
        if (notBlank(ep.getAccount())) setAccount(ep.getAccount());
        if (notBlank(ep.getUsername())) setUsername(ep.getUsername());
        if (notBlank(ep.getPassword())) setPassword(ep.getPassword());
        if (notBlank(ep.getPrivateKey())) setPrivateKey(ep.getPrivateKey());
    if (notBlank(ep.getPrivateKeyFile())) setPrivateKeyFile(ep.getPrivateKeyFile());
        if (notBlank(ep.getDatabase())) setDatabase(ep.getDatabase());
        if (notBlank(ep.getSchema())) setSchema(ep.getSchema());
        if (notBlank(ep.getWarehouse())) setWarehouse(ep.getWarehouse());
        if (notBlank(ep.getRole())) setRole(ep.getRole());
        if (notBlank(ep.getQuery())) setQuery(ep.getQuery());
        if (notBlank(ep.getOperation())) setOperation(ep.getOperation());
        if (notBlank(ep.getJdbcUrl())) setJdbcUrl(ep.getJdbcUrl());
        if (ep.getEnableParameterBinding() != null) setEnableParameterBinding(ep.getEnableParameterBinding());
        if (notBlank(ep.getParameterPrefix())) setParameterPrefix(ep.getParameterPrefix());
        if (notBlank(ep.getAuthenticator())) setAuthenticator(ep.getAuthenticator());
        if (notBlank(ep.getOutputFormat())) setOutputFormat(ep.getOutputFormat());
        if (notBlank(ep.getToken())) setToken(ep.getToken());
        if (ep.getJdbcParameters() != null && !ep.getJdbcParameters().isEmpty()) {
            setJdbcParameters(ep.getJdbcParameters());
        }
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    /**
     * Builds the Snowflake JDBC URL from configuration parameters.
     * If jdbcUrl is set, it will be used instead.
     */
    public String buildJdbcUrl() {
        // Ensure we populate unset fields from System properties before constructing URL
        // This allows using -Dsnowflake.* without editing endpoint URIs
        getAccount();
        getDatabase();
        getSchema();
        getWarehouse();
        getRole();
        getOutputFormat();
        getJdbcParameters();
        // Use custom JDBC URL if provided (useful for testing) but append extra jdbcParameters if present
        if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            String url = jdbcUrl;
            if (jdbcParameters != null && !jdbcParameters.isEmpty()) {
                url = appendParams(url, jdbcParameters);
            }
            return url;
        }
        
        StringBuilder url = new StringBuilder("jdbc:snowflake://");
        url.append(account).append(".snowflakecomputing.com/");
        
        boolean hasQuery = false;
        if (database != null && !database.isEmpty()) {
            url.append("?db=").append(database);
            hasQuery = true;
            if (schema != null && !schema.isEmpty()) {
                url.append("&schema=").append(schema);
            }
            if (warehouse != null && !warehouse.isEmpty()) {
                url.append("&warehouse=").append(warehouse);
            }
            if (role != null && !role.isEmpty()) {
                url.append("&role=").append(role);
            }
        }

        // Set JDBC_QUERY_RESULT_FORMAT to control driver result format
        String fmt = outputFormat;
        url.append(hasQuery ? "&" : "?")
           .append("JDBC_QUERY_RESULT_FORMAT=")
           .append((fmt != null && fmt.equalsIgnoreCase("arrow")) ? "ARROW" : "JSON");

        // Append any additional JDBC parameters
        if (jdbcParameters != null && !jdbcParameters.isEmpty()) {
            url = new StringBuilder(appendParams(url.toString(), jdbcParameters));
        }
        
        return url.toString();
    }

    private String appendParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) return baseUrl;
        StringBuilder sb = new StringBuilder(baseUrl);
        boolean hasQuery = baseUrl.contains("?");
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank()) continue;
            String key = e.getKey();
            String val = e.getValue() != null ? e.getValue() : "";
            sb.append(hasQuery ? '&' : '?');
            hasQuery = true;
            sb.append(key).append('=')
              .append(URLEncoder.encode(val, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
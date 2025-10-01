package io.dscope.camel.snowflake.test;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.util.Optional;

/**
 * Utility class for loading environment variables from .env files and system environment.
 * This class provides a convenient way to access Snowflake connection properties
 * from environment variables or .env files for testing purposes.
 * 
 * <p>The loading order is:
 * <ol>
 *   <li>System environment variables</li>
 *   <li>.env.local file (for local overrides)</li>
 *   <li>.env file (for default values)</li>
 * </ol>
 */
public class SnowflakeTestEnvironment {
    
    private static final Dotenv dotenv;
    
    static {
        // Initialize dotenv with support for .env.local override
        Dotenv temp = null;
        
        // First try .env.local (highest priority)
        java.io.File envLocalFile = new java.io.File(".env.local");
        if (envLocalFile.exists()) {
            try {
                temp = Dotenv.configure()
                        .filename(".env.local")
                        .load();
            } catch (Exception e) {
                // .env.local exists but couldn't be loaded
                System.err.println("Warning: .env.local exists but couldn't be loaded: " + e.getMessage());
            }
        }
        
        // If .env.local wasn't loaded, try .env
        if (temp == null) {
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                try {
                    temp = Dotenv.configure()
                            .filename(".env")
                            .load();
                } catch (Exception e) {
                    // .env exists but couldn't be loaded
                    System.err.println("Warning: .env exists but couldn't be loaded: " + e.getMessage());
                }
            }
        }
        
        dotenv = temp;
    }
    
    /**
     * Gets an environment variable value, checking system environment first,
     * then .env files as fallback.
     */
    public static String get(String key) {
        // First check system environment
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        
        // Then check .env files
        if (dotenv != null) {
            value = dotenv.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        
        return null;
    }
    
    /**
     * Gets an environment variable value with a default fallback.
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Gets an environment variable as an Optional.
     */
    public static Optional<String> getOptional(String key) {
        return Optional.ofNullable(get(key));
    }
    
    /**
     * Gets an environment variable as an integer with a default fallback.
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Log warning but return default
                System.err.println("Warning: Invalid integer value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Checks if running in integration test mode (when real Snowflake credentials are provided).
     * Supports both password and private key authentication.
     */
    public static boolean isIntegrationMode() {
        // Check for valid account and username
        boolean hasValidAccount = getOptional("SNOWFLAKE_ACCOUNT")
                .filter(account -> !account.equals("your-account.region") && 
                                 !account.equals("testaccount"))
                .isPresent();
        
        boolean hasValidUsername = getOptional("SNOWFLAKE_USERNAME")
                .filter(username -> !username.equals("your-username") && 
                                  !username.equals("testuser"))
                .isPresent();
        
        if (!hasValidAccount || !hasValidUsername) {
            return false;
        }
        
        // Check for either password or private key authentication
        boolean hasValidPassword = getOptional("SNOWFLAKE_PASSWORD")
                .filter(password -> !password.equals("your-password-here") && 
                                  !password.equals("testpass") &&
                                  !password.trim().isEmpty())
                .isPresent();
        
    boolean hasValidPrivateKey = getOptional("SNOWFLAKE_PRIVATE_KEY")
        .map(String::trim)
        .filter(pk -> !pk.isEmpty() && !pk.equals("your-private-key-here"))
        // Heuristic: PKCS#8 base64 payloads are typically quite long (>500 chars)
        .filter(pk -> pk.replace("-----BEGIN PRIVATE KEY-----", "")
                 .replace("-----END PRIVATE KEY-----", "")
                 .replaceAll("\\s+", "").length() > 500)
        .isPresent();
        
        return hasValidPassword || hasValidPrivateKey;
    }
    
    // Snowflake Connection Properties
    public static class Snowflake {
        public static String getAccount() { return get("SNOWFLAKE_ACCOUNT", "testaccount"); }
        public static String getUsername() { return get("SNOWFLAKE_USERNAME", "testuser"); }
        public static String getPassword() { return get("SNOWFLAKE_PASSWORD", "testpass"); }
        public static String getDatabase() { return get("SNOWFLAKE_DATABASE", "testdb"); }
        public static String getSchema() { return get("SNOWFLAKE_SCHEMA", "testschema"); }
        public static String getWarehouse() { return get("SNOWFLAKE_WAREHOUSE", "testwh"); }
        public static String getRole() { return get("SNOWFLAKE_ROLE", "testrole"); }
        public static String getPrivateKey() { return get("SNOWFLAKE_PRIVATE_KEY"); }
        public static String getTestTable() { return get("SNOWFLAKE_TEST_TABLE", "testtable"); }
        public static String getTestQuery() { return get("SNOWFLAKE_TEST_QUERY", "SELECT * FROM testtable LIMIT 10"); }
        public static String getJdbcUrl() { return get("SNOWFLAKE_JDBC_URL"); }
        public static int getMaxPoolSize() { return getInt("SNOWFLAKE_MAX_POOL_SIZE", 10); }
        public static int getMinPoolSize() { return getInt("SNOWFLAKE_MIN_POOL_SIZE", 2); }
        public static int getConnectionTimeout() { return getInt("SNOWFLAKE_CONNECTION_TIMEOUT", 30000); }
    }
}
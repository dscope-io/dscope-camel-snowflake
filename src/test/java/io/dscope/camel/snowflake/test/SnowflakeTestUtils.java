package io.dscope.camel.snowflake.test;

import io.dscope.camel.snowflake.SnowflakeConfiguration;

/**
 * Utility class for creating test configurations for Snowflake component tests.
 * This class provides pre-configured test setups to make testing easier.
 * 
 * <p>Configuration values are loaded from environment variables or .env files when available,
 * falling back to default test values for unit testing.
 */
public class SnowflakeTestUtils {

    /**
     * Creates a basic test configuration for Snowflake component.
     * Uses environment variables from .env files when available.
     */
    public static SnowflakeConfiguration createBasicTestConfiguration() {
        SnowflakeConfiguration config = new SnowflakeConfiguration();
        config.setAccount(SnowflakeTestEnvironment.Snowflake.getAccount());
        config.setUsername(SnowflakeTestEnvironment.Snowflake.getUsername());
        config.setPassword(SnowflakeTestEnvironment.Snowflake.getPassword());
        config.setDatabase(SnowflakeTestEnvironment.Snowflake.getDatabase());
        config.setSchema(SnowflakeTestEnvironment.Snowflake.getSchema());
        config.setWarehouse(SnowflakeTestEnvironment.Snowflake.getWarehouse());
        config.setRole(SnowflakeTestEnvironment.Snowflake.getRole());
        
        // Set private key if available (preferred over password authentication)
        String privateKey = SnowflakeTestEnvironment.Snowflake.getPrivateKey();
        if (privateKey != null && !privateKey.trim().isEmpty()) {
            config.setPrivateKey(privateKey);
            // When using private key authentication, clear password to avoid conflicts
            config.setPassword(null);
        }
        
        return config;
    }

    /**
     * Creates a configuration for producer testing.
     */
    public static SnowflakeConfiguration createProducerTestConfiguration() {
        SnowflakeConfiguration config = createBasicTestConfiguration();
        config.setTable(SnowflakeTestEnvironment.get("SNOWFLAKE_PRODUCER_TEST_TABLE", "producer_test_table"));
        config.setOperation("insert");
        return config;
    }

    /**
     * Creates a configuration for consumer testing.
     */
    public static SnowflakeConfiguration createConsumerTestConfiguration() {
        SnowflakeConfiguration config = createBasicTestConfiguration();
        config.setQuery(SnowflakeTestEnvironment.get("SNOWFLAKE_CONSUMER_TEST_QUERY", "SELECT * FROM consumer_test_table"));
        config.setOperation("select");
        return config;
    }

    /**
     * Creates a configuration with custom parameters.
     */
    public static SnowflakeConfiguration createCustomConfiguration(String account, String database, String table) {
        SnowflakeConfiguration config = new SnowflakeConfiguration();
        config.setAccount(account);
        config.setDatabase(database);
        config.setTable(table);
        config.setUsername("customuser");
        config.setPassword("custompass");
        return config;
    }

    /**
     * Validates that a configuration has the expected values.
     */
    public static void validateConfiguration(SnowflakeConfiguration config, 
                                           String expectedAccount, 
                                           String expectedDatabase) {
        if (config == null) {
            throw new AssertionError("Configuration should not be null");
        }
        if (!expectedAccount.equals(config.getAccount())) {
            throw new AssertionError("Expected account: " + expectedAccount + ", but was: " + config.getAccount());
        }
        if (!expectedDatabase.equals(config.getDatabase())) {
            throw new AssertionError("Expected database: " + expectedDatabase + ", but was: " + config.getDatabase());
        }
    }

    /**
     * Creates test data for Snowflake operations.
     */
    public static String createTestData(String prefix, int recordCount) {
        StringBuilder data = new StringBuilder();
        for (int i = 1; i <= recordCount; i++) {
            data.append(prefix).append("_record_").append(i);
            if (i < recordCount) {
                data.append("\n");
            }
        }
        return data.toString();
    }

    /**
     * Simulates a Snowflake JDBC URL for testing.
     */
    public static String createTestJdbcUrl(String account, String database) {
        return "jdbc:snowflake://" + account + ".snowflakecomputing.com/?db=" + database;
    }

    /**
     * Common test constants. These now use environment variables when available.
     */
    public static class TestConstants {
        public static String getTestAccount() { return SnowflakeTestEnvironment.Snowflake.getAccount(); }
        public static String getTestDatabase() { return SnowflakeTestEnvironment.Snowflake.getDatabase(); }
        public static String getTestSchema() { return SnowflakeTestEnvironment.Snowflake.getSchema(); }
        public static String getTestTable() { return SnowflakeTestEnvironment.Snowflake.getTestTable(); }
        public static String getTestWarehouse() { return SnowflakeTestEnvironment.Snowflake.getWarehouse(); }
        public static String getTestUsername() { return SnowflakeTestEnvironment.Snowflake.getUsername(); }
        public static String getTestPassword() { return SnowflakeTestEnvironment.Snowflake.getPassword(); }
        public static String getTestRole() { return SnowflakeTestEnvironment.Snowflake.getRole(); }
    }
    
    /**
     * Checks if integration tests should be run (real Snowflake credentials are available).
     */
    public static boolean shouldRunIntegrationTests() {
        // Require an explicit opt-in flag to run real integration tests to avoid accidental runs
        String explicitFlag = SnowflakeTestEnvironment.get("SNOWFLAKE_RUN_INTEGRATION");
        if (explicitFlag == null) {
            explicitFlag = SnowflakeTestEnvironment.get("SNOWFLAKE_INTEGRATION");
        }
        boolean optedIn = explicitFlag != null && (explicitFlag.equalsIgnoreCase("true") || explicitFlag.equals("1") || explicitFlag.equalsIgnoreCase("yes"));
        return optedIn && SnowflakeTestEnvironment.isIntegrationMode();
    }
    
    /**
     * Creates a test configuration from environment variables for integration testing.
     */
    public static SnowflakeConfiguration createIntegrationTestConfiguration() {
        if (!shouldRunIntegrationTests()) {
            throw new IllegalStateException("Integration test configuration requested but no real Snowflake credentials found in environment");
        }
        return createBasicTestConfiguration();
    }
}
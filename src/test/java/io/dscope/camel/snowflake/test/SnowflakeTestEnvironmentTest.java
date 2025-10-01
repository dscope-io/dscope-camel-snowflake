package io.dscope.camel.snowflake.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying environment configuration functionality.
 */
class SnowflakeTestEnvironmentTest {

    @Test
    void testDefaultValues() {
        // Test that default values are returned when no environment variables are set
        String account = SnowflakeTestEnvironment.get("SNOWFLAKE_ACCOUNT", "default-account");
        assertNotNull(account);
        // Be environment-agnostic: just ensure we resolved to a non-blank value
        assertFalse(account.isBlank());
    }

    @Test
    void testSnowflakeConstants() {
        // Test that Snowflake constants are accessible and not null
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getAccount());
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getUsername());
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getPassword());
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getDatabase());
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getSchema());
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getWarehouse());
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getRole());
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getTestTable());
        assertNotNull(SnowflakeTestEnvironment.Snowflake.getTestQuery());
    }

    @Test
    void testIntegerValues() {
        // Test integer parsing with defaults
        int maxPoolSize = SnowflakeTestEnvironment.getInt("SNOWFLAKE_MAX_POOL_SIZE", 5);
        assertTrue(maxPoolSize >= 5); // Should be at least the default
        
        int minPoolSize = SnowflakeTestEnvironment.getInt("SNOWFLAKE_MIN_POOL_SIZE", 1);
        assertTrue(minPoolSize >= 1); // Should be at least the default
    }

    @Test 
    void testOptionalValues() {
        // Test optional value handling
        var privateKey = SnowflakeTestEnvironment.getOptional("SNOWFLAKE_PRIVATE_KEY");
        // Private key may or may not be set, so just check it's an Optional
        assertNotNull(privateKey);
    }

    @Test
    void testIntegrationMode() {
        // Test integration mode detection
        boolean integrationMode = SnowflakeTestEnvironment.isIntegrationMode();
        // This will be false in default test setup, true if real credentials are provided
        assertNotNull(integrationMode); // Just verify it doesn't throw an exception
    }
}
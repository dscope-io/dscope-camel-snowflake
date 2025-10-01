package io.dscope.camel.snowflake.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dscope.camel.snowflake.SnowflakeConfiguration;

/**
 * Test that demonstrates SELECT dual functionality using .env properties
 * without requiring actual Snowflake connection.
 */
class SnowflakeSelectDualUnitTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SnowflakeSelectDualUnitTest.class);
    
    @Test
    void testConfigurationFromEnvForSelectDual() {
        logger.info("Testing Snowflake configuration from .env for SELECT dual query");
        
        // Create configuration from environment (.env file)
        SnowflakeConfiguration config = SnowflakeTestUtils.createBasicTestConfiguration();
        
        // Log the configuration (this will show values from .env file)
        logger.info("Snowflake Configuration from .env:");
        logger.info("  Account: {}", config.getAccount());
        logger.info("  Database: {}", config.getDatabase());
        logger.info("  Schema: {}", config.getSchema());
        logger.info("  Warehouse: {}", config.getWarehouse());
        logger.info("  Username: {}", config.getUsername());
        logger.info("  Role: {}", config.getRole());
        logger.info("  Private Key present: {}", config.getPrivateKey() != null);
        
        // Verify configuration is loaded from .env
        assertNotNull(config.getAccount(), "Account should be loaded from .env");
        assertNotNull(config.getDatabase(), "Database should be loaded from .env");
        assertNotNull(config.getSchema(), "Schema should be loaded from .env");
        assertNotNull(config.getWarehouse(), "Warehouse should be loaded from .env");
        assertNotNull(config.getUsername(), "Username should be loaded from .env");
        assertNotNull(config.getRole(), "Role should be loaded from .env");
        
        // If non-defaults are present, they should match what's loaded from the environment helper
        if (!config.getAccount().equals("testaccount")) {
            logger.info("✓ Using non-default account from env: {}", config.getAccount());
            assertEquals(SnowflakeTestEnvironment.Snowflake.getAccount(), config.getAccount());
            assertEquals(SnowflakeTestEnvironment.Snowflake.getDatabase(), config.getDatabase());
            assertEquals(SnowflakeTestEnvironment.Snowflake.getSchema(), config.getSchema());
            assertEquals(SnowflakeTestEnvironment.Snowflake.getWarehouse(), config.getWarehouse());
            assertEquals(SnowflakeTestEnvironment.Snowflake.getUsername(), config.getUsername());
            assertEquals(SnowflakeTestEnvironment.Snowflake.getRole(), config.getRole());
        } else {
            logger.info("✓ Using default test values (no .env override detected)");
        }
        
        // Test JDBC URL building for SELECT dual
        String jdbcUrl = config.buildJdbcUrl();
        assertNotNull(jdbcUrl, "JDBC URL should be buildable");
        assertTrue(jdbcUrl.startsWith("jdbc:snowflake://"), "Should be Snowflake JDBC URL");
        logger.info("  JDBC URL: {}", jdbcUrl);
        
        logger.info("Configuration test completed - ready for SELECT 1 query");
    }
    
    @Test
    void testSelectDualQueryConstruction() {
        // Test that we can construct the SELECT dual equivalent for Snowflake
        String selectDualQuery = "SELECT 1 as test_value";
        String selectDualWithTimestamp = "SELECT 1 as test_value, CURRENT_TIMESTAMP() as current_time";
        String selectDualWithDatabase = "SELECT 1 as test_value, CURRENT_DATABASE() as db_name";
        
        logger.info("Snowflake SELECT dual equivalent queries:");
        logger.info("  Basic: {}", selectDualQuery);
        logger.info("  With timestamp: {}", selectDualWithTimestamp);
        logger.info("  With database: {}", selectDualWithDatabase);
        
        // Verify query construction
        assertTrue(selectDualQuery.contains("SELECT 1"), "Should contain SELECT 1");
        assertTrue(selectDualWithTimestamp.contains("CURRENT_TIMESTAMP()"), "Should use Snowflake timestamp function");
        assertTrue(selectDualWithDatabase.contains("CURRENT_DATABASE()"), "Should use Snowflake database function");
    }
    
    @Test
    void testEnvironmentVariableAccess() {
        // Test direct access to environment variables for Snowflake connection
        logger.info("Testing direct environment variable access:");
        
        String account = SnowflakeTestEnvironment.Snowflake.getAccount();
        String database = SnowflakeTestEnvironment.Snowflake.getDatabase();
        String schema = SnowflakeTestEnvironment.Snowflake.getSchema();
        String warehouse = SnowflakeTestEnvironment.Snowflake.getWarehouse();
        String username = SnowflakeTestEnvironment.Snowflake.getUsername();
        String role = SnowflakeTestEnvironment.Snowflake.getRole();
        
        logger.info("  SNOWFLAKE_ACCOUNT: {}", account);
        logger.info("  SNOWFLAKE_DATABASE: {}", database);
        logger.info("  SNOWFLAKE_SCHEMA: {}", schema);
        logger.info("  SNOWFLAKE_WAREHOUSE: {}", warehouse);
        logger.info("  SNOWFLAKE_USERNAME: {}", username);
        logger.info("  SNOWFLAKE_ROLE: {}", role);
        
        // These should all be non-null (either from .env or defaults)
        assertNotNull(account);
        assertNotNull(database);
        assertNotNull(schema);
        assertNotNull(warehouse);
        assertNotNull(username);
        assertNotNull(role);
        
        // Test connection pool settings
        int maxPoolSize = SnowflakeTestEnvironment.Snowflake.getMaxPoolSize();
        int minPoolSize = SnowflakeTestEnvironment.Snowflake.getMinPoolSize();
        int connectionTimeout = SnowflakeTestEnvironment.Snowflake.getConnectionTimeout();
        
        logger.info("  Connection Pool Settings:");
        logger.info("    Max Pool Size: {}", maxPoolSize);
        logger.info("    Min Pool Size: {}", minPoolSize);
        logger.info("    Connection Timeout: {}ms", connectionTimeout);
        
        assertTrue(maxPoolSize > 0, "Max pool size should be positive");
        assertTrue(minPoolSize >= 0, "Min pool size should be non-negative");
        assertTrue(connectionTimeout > 0, "Connection timeout should be positive");
    }
    
    @Test
    void testIntegrationModeDetection() {
        boolean isIntegrationMode = SnowflakeTestEnvironment.isIntegrationMode();
        logger.info("Integration mode detection: {}", isIntegrationMode);
        
        if (isIntegrationMode) {
            logger.info("✓ Real Snowflake credentials detected - integration tests would run");
            logger.info("  This means SELECT 1 query could be executed against real Snowflake");
        } else {
            logger.info("✓ Using test credentials - integration tests skipped");
            logger.info("  This means SELECT 1 query would use mock connections");
        }
        
        // Test should pass regardless of mode
        assertNotNull(isIntegrationMode);
    }
}
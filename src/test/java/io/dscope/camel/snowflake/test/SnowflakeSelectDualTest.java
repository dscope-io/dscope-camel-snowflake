package io.dscope.camel.snowflake.test;

import io.dscope.camel.snowflake.SnowflakeConfiguration;
import io.dscope.camel.snowflake.jdbc.SnowflakeJdbcConnectionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that performs a real Snowflake query using properties from .env file.
 * This test demonstrates connecting to Snowflake and executing a simple SELECT query.
 */
class SnowflakeSelectDualTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SnowflakeSelectDualTest.class);
    
    @Test
    @EnabledIf("io.dscope.camel.snowflake.test.SnowflakeTestUtils#shouldRunIntegrationTests")
    void testSelectDualFromSnowflake() throws SQLException {
        // This test only runs if real Snowflake credentials are provided in .env
        logger.info("Running Snowflake SELECT 1 integration test");
        
        // Create configuration from environment variables
        SnowflakeConfiguration config = SnowflakeTestUtils.createIntegrationTestConfiguration();
        
        // Log the connection details (without sensitive info)
        logger.info("Connecting to Snowflake account: {}", config.getAccount());
        logger.info("Database: {}, Schema: {}, Warehouse: {}", 
                   config.getDatabase(), config.getSchema(), config.getWarehouse());
        
        // Get connection using static method
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config)) {
            assertNotNull(connection, "Connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");
            
            logger.info("Successfully connected to Snowflake");
            
            // Execute a simple SELECT 1 query (equivalent to Oracle's SELECT FROM dual)
            String sql = "SELECT 1 as test_value, CURRENT_TIMESTAMP() as current_time, CURRENT_DATABASE() as db_name";
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                logger.info("Executing query: {}", sql);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next(), "Query should return at least one row");
                    
                    // Verify the results
                    int testValue = resultSet.getInt("test_value");
                    String currentTime = resultSet.getString("current_time");
                    String dbName = resultSet.getString("db_name");
                    
                    assertEquals(1, testValue, "SELECT 1 should return 1");
                    assertNotNull(currentTime, "Current timestamp should not be null");
                    assertNotNull(dbName, "Database name should not be null");
                    
                    logger.info("Query results:");
                    logger.info("  Test Value: {}", testValue);
                    logger.info("  Current Time: {}", currentTime);
                    logger.info("  Database Name: {}", dbName);
                    
                    // Verify database name matches configuration
                    assertEquals(config.getDatabase(), dbName, 
                               "Database name from query should match configuration");
                    
                    // Ensure no more rows
                    assertFalse(resultSet.next(), "Query should return exactly one row");
                }
            }
            
            logger.info("Snowflake SELECT 1 test completed successfully");
            
        } catch (SQLException e) {
            logger.error("SQL Exception during Snowflake test: {}", e.getMessage(), e);
            fail("Failed to execute Snowflake query: " + e.getMessage());
        }
    }
    
    @Test
    @EnabledIf("io.dscope.camel.snowflake.test.SnowflakeTestUtils#shouldRunIntegrationTests")
    void testShowDatabases() throws SQLException {
        // Test SHOW DATABASES command to verify access permissions
        logger.info("Running Snowflake SHOW DATABASES integration test");
        
        SnowflakeConfiguration config = SnowflakeTestUtils.createIntegrationTestConfiguration();
        
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config)) {
            String sql = "SHOW DATABASES";
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                logger.info("Executing query: {}", sql);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    boolean foundDatabase = false;
                    int databaseCount = 0;
                    
                    while (resultSet.next()) {
                        databaseCount++;
                        String dbName = resultSet.getString("name");
                        logger.info("Found database: {}", dbName);
                        
                        if (config.getDatabase().equalsIgnoreCase(dbName)) {
                            foundDatabase = true;
                            logger.info("✓ Target database '{}' found in accessible databases", dbName);
                        }
                    }
                    
                    assertTrue(databaseCount > 0, "Should have access to at least one database");
                    assertTrue(foundDatabase, 
                              "Should have access to configured database: " + config.getDatabase());
                    
                    logger.info("Found {} accessible databases", databaseCount);
                }
            }
            
            logger.info("SHOW DATABASES test completed successfully");
            
        } catch (SQLException e) {
            logger.error("SQL Exception during SHOW DATABASES test: {}", e.getMessage(), e);
            fail("Failed to execute SHOW DATABASES: " + e.getMessage());
        }
    }
    
    @Test 
    @EnabledIf("io.dscope.camel.snowflake.test.SnowflakeTestUtils#shouldRunIntegrationTests")
    void testWarehouseAndRoleContext() throws SQLException {
        // Test that we can use the configured warehouse and role
        logger.info("Running Snowflake context verification test");
        
        SnowflakeConfiguration config = SnowflakeTestUtils.createIntegrationTestConfiguration();
        
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config)) {
            // Test current warehouse
            String warehouseSql = "SELECT CURRENT_WAREHOUSE() as warehouse_name";
            try (PreparedStatement statement = connection.prepareStatement(warehouseSql);
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next()) {
                    String currentWarehouse = resultSet.getString("warehouse_name");
                    logger.info("Current warehouse: {}", currentWarehouse);
                    
                    if (config.getWarehouse() != null) {
                        assertEquals(config.getWarehouse().toUpperCase(), 
                                   currentWarehouse.toUpperCase(),
                                   "Current warehouse should match configuration");
                    }
                }
            }
            
            // Test current role
            String roleSql = "SELECT CURRENT_ROLE() as role_name";
            try (PreparedStatement statement = connection.prepareStatement(roleSql);
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next()) {
                    String currentRole = resultSet.getString("role_name");
                    logger.info("Current role: {}", currentRole);
                    
                    if (config.getRole() != null) {
                        assertEquals(config.getRole().toUpperCase(), 
                                   currentRole.toUpperCase(),
                                   "Current role should match configuration");
                    }
                }
            }
            
            // Test current schema
            String schemaSql = "SELECT CURRENT_SCHEMA() as schema_name";
            try (PreparedStatement statement = connection.prepareStatement(schemaSql);
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next()) {
                    String currentSchema = resultSet.getString("schema_name");
                    logger.info("Current schema: {}", currentSchema);
                    
                    if (config.getSchema() != null) {
                        assertEquals(config.getSchema().toUpperCase(), 
                                   currentSchema.toUpperCase(),
                                   "Current schema should match configuration");
                    }
                }
            }
            
            logger.info("Context verification test completed successfully");
            
        } catch (SQLException e) {
            logger.error("SQL Exception during context verification: {}", e.getMessage(), e);
            fail("Failed to verify Snowflake context: " + e.getMessage());
        }
    }
}
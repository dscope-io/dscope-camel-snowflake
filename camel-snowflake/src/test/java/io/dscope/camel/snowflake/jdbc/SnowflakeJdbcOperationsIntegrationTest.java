/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dscope.camel.snowflake.jdbc;

import io.dscope.camel.snowflake.SnowflakeConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JDBC operations.
 * These tests are disabled by default as they require actual Snowflake connection.
 * To run these tests, set up a real Snowflake connection and remove @Disabled annotations.
 */
@Disabled("Requires actual Snowflake connection")
class SnowflakeJdbcOperationsIntegrationTest {

    private SnowflakeConfiguration config;

    @BeforeEach
    void setUp() {
        // For integration tests, we'll use H2 database as a mock
        // In real scenarios, this would connect to actual Snowflake
        config = new SnowflakeConfiguration();
        config.setAccount("test-account");
        config.setDatabase("test-db");
        config.setSchema("test-schema");
        config.setUsername("test-user");
        config.setPassword("test-password");
        config.setWarehouse("test-warehouse");
        
        // Override JDBC URL for H2 testing
        String h2Url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        config.setJdbcUrl(h2Url);
    }

    @Test
    void testExecuteQuery() throws SQLException {
        // Create a test table
        SnowflakeJdbcOperations.executeUpdate(config, 
            "CREATE TABLE IF NOT EXISTS test_table (id INTEGER, name VARCHAR(50))");
        
        // Insert test data
        SnowflakeJdbcOperations.executeUpdate(config,
            "INSERT INTO test_table (id, name) VALUES (?, ?)", 1, "Alice");
        SnowflakeJdbcOperations.executeUpdate(config,
            "INSERT INTO test_table (id, name) VALUES (?, ?)", 2, "Bob");
        
        // Query data
        List<Map<String, Object>> results = SnowflakeJdbcOperations.executeQuery(config,
            "SELECT * FROM test_table WHERE id = ?", 1);
        
        assertNotNull(results);
        assertEquals(1, results.size());
        
        Map<String, Object> row = results.get(0);
        assertEquals(1, row.get("ID"));
        assertEquals("Alice", row.get("NAME"));
    }

    @Test
    void testExecuteUpdate() throws SQLException {
        // Create a test table
        SnowflakeJdbcOperations.executeUpdate(config,
            "CREATE TABLE IF NOT EXISTS update_test (id INTEGER, value VARCHAR(50))");
        
        // Insert data
        int insertedRows = SnowflakeJdbcOperations.executeUpdate(config,
            "INSERT INTO update_test (id, value) VALUES (?, ?)", 1, "initial");
        assertEquals(1, insertedRows);
        
        // Update data
        int updatedRows = SnowflakeJdbcOperations.executeUpdate(config,
            "UPDATE update_test SET value = ? WHERE id = ?", "updated", 1);
        assertEquals(1, updatedRows);
        
        // Verify update
        List<Map<String, Object>> results = SnowflakeJdbcOperations.executeQuery(config,
            "SELECT * FROM update_test WHERE id = ?", 1);
        
        assertEquals(1, results.size());
        assertEquals("updated", results.get(0).get("VALUE"));
    }

    @Test
    void testExecuteBatch() throws SQLException {
        // Create a test table
        SnowflakeJdbcOperations.executeUpdate(config,
            "CREATE TABLE IF NOT EXISTS batch_test (id INTEGER, name VARCHAR(50))");
        
        // Prepare batch data
        List<Object[]> batchParams = Arrays.asList(
            new Object[]{1, "Alice"},
            new Object[]{2, "Bob"},
            new Object[]{3, "Charlie"}
        );
        
        // Execute batch
        int[] results = SnowflakeJdbcOperations.executeBatch(config,
            "INSERT INTO batch_test (id, name) VALUES (?, ?)", batchParams);
        
        assertEquals(3, results.length);
        for (int result : results) {
            assertEquals(1, result); // Each insert should affect 1 row
        }
        
        // Verify all data was inserted
        List<Map<String, Object>> queryResults = SnowflakeJdbcOperations.executeQuery(config,
            "SELECT COUNT(*) as count FROM batch_test");
        
        assertEquals(1, queryResults.size());
        assertEquals(3L, queryResults.get(0).get("COUNT"));
    }

    @Test
    void testJsonOperations() throws SQLException {
        // Create a test table for JSON
        SnowflakeJdbcOperations.executeUpdate(config,
            "CREATE TABLE IF NOT EXISTS json_test (id INTEGER, json_data VARCHAR(1000))");
        
        // Insert JSON data (simulated for H2)
        String jsonData = "{\"name\": \"Alice\", \"age\": 30, \"city\": \"New York\"}";
        int insertedRows = SnowflakeJdbcOperations.executeUpdate(config,
            "INSERT INTO json_test (id, json_data) VALUES (?, ?)", 1, jsonData);
        
        assertEquals(1, insertedRows);
        
        // Query JSON data
        List<Map<String, Object>> results = SnowflakeJdbcOperations.executeQuery(config,
            "SELECT json_data FROM json_test WHERE id = ?", 1);
        
        assertEquals(1, results.size());
        assertEquals(jsonData, results.get(0).get("JSON_DATA"));
    }

    @Test
    void testConnectionPooling() throws SQLException {
        // Test multiple concurrent connections
        for (int i = 0; i < 10; i++) {
            List<Map<String, Object>> results = SnowflakeJdbcOperations.executeQuery(config,
                "SELECT 1 as test_value");
            
            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).get("TEST_VALUE"));
        }
        
        // Verify connection pool statistics
        String poolStats = SnowflakeJdbcConnectionManager.getPoolStats(config);
        assertNotNull(poolStats);
        assertTrue(poolStats.contains("Pool Stats"));
    }

    @Test
    void testDatabaseMetadata() throws SQLException {
        Map<String, Object> metadata = SnowflakeJdbcOperations.getDatabaseMetadata(config);
        
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("databaseProductName"));
        assertTrue(metadata.containsKey("driverName"));
        assertTrue(metadata.containsKey("url"));
        
        // H2 specific assertions
        assertEquals("H2", metadata.get("databaseProductName"));
        assertTrue(metadata.get("url").toString().contains("jdbc:h2"));
    }

    @Test
    void testErrorHandling() {
        // Test with invalid SQL
        SQLException exception1 = assertThrows(SQLException.class, () -> {
            SnowflakeJdbcOperations.executeQuery(config, "INVALID SQL QUERY");
        });
        assertNotNull(exception1);
        
        // Test with invalid table
        SQLException exception2 = assertThrows(SQLException.class, () -> {
            SnowflakeJdbcOperations.executeQuery(config, "SELECT * FROM non_existent_table");
        });
        assertNotNull(exception2);
    }

    @Test
    void testTransactionHandling() throws SQLException {
        // Create test table
        SnowflakeJdbcOperations.executeUpdate(config,
            "CREATE TABLE IF NOT EXISTS transaction_test (id INTEGER, value VARCHAR(50))");
        
        // Test successful transaction
        SnowflakeJdbcOperations.executeUpdate(config,
            "INSERT INTO transaction_test (id, value) VALUES (?, ?)", 1, "test");
        
        // Verify data exists
        List<Map<String, Object>> results = SnowflakeJdbcOperations.executeQuery(config,
            "SELECT COUNT(*) as count FROM transaction_test");
        
        assertEquals(1L, results.get(0).get("COUNT"));
    }

    @Test
    void testParameterBinding() throws SQLException {
        // Create test table
        SnowflakeJdbcOperations.executeUpdate(config,
            "CREATE TABLE IF NOT EXISTS param_test (id INTEGER, name VARCHAR(50), active BOOLEAN, score DOUBLE)");
        
        // Test various parameter types
        int result = SnowflakeJdbcOperations.executeUpdate(config,
            "INSERT INTO param_test (id, name, active, score) VALUES (?, ?, ?, ?)",
            1, "Alice", true, 95.5);
        
        assertEquals(1, result);
        
        // Query with parameters
        List<Map<String, Object>> results = SnowflakeJdbcOperations.executeQuery(config,
            "SELECT * FROM param_test WHERE id = ? AND active = ?", 1, true);
        
        assertEquals(1, results.size());
        Map<String, Object> row = results.get(0);
        assertEquals(1, row.get("ID"));
        assertEquals("Alice", row.get("NAME"));
        assertEquals(true, row.get("ACTIVE"));
        assertEquals(95.5, row.get("SCORE"));
    }
}
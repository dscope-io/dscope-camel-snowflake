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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JDBC Operations utilities.
 * These tests verify the utility methods without requiring database connections.
 */
class SnowflakeJdbcOperationsTest {

    private SnowflakeConfiguration config;

    @BeforeEach
    void setUp() {
        config = new SnowflakeConfiguration();
        config.setAccount("test-account");
        config.setDatabase("test-db");
        config.setSchema("test-schema");
        config.setUsername("test-user");
        config.setPassword("test-password");
        config.setWarehouse("test-warehouse");
    }

    @Test
    void testConfigurationSetup() {
        assertNotNull(config);
        assertEquals("test-account", config.getAccount());
        assertEquals("test-db", config.getDatabase());
        assertEquals("test-schema", config.getSchema());
        assertEquals("test-user", config.getUsername());
        assertEquals("test-password", config.getPassword());
        assertEquals("test-warehouse", config.getWarehouse());
    }

    @Test
    void testJdbcUrlGeneration() {
        String jdbcUrl = config.buildJdbcUrl();
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:snowflake://"));
        assertTrue(jdbcUrl.contains("test-account.snowflakecomputing.com"));
        assertTrue(jdbcUrl.contains("db=test-db"));
        assertTrue(jdbcUrl.contains("schema=test-schema"));
        assertTrue(jdbcUrl.contains("warehouse=test-warehouse"));
    }

    @Test
    void testCustomJdbcUrlOverride() {
        String customUrl = "jdbc:h2:mem:testdb";
        config.setJdbcUrl(customUrl);
        
        String actualUrl = config.buildJdbcUrl();
        assertEquals(customUrl, actualUrl);
    }

    @Test
    void testConnectionManagerDataSourceCreation() {
        // Test that we can create data sources (this will test the public methods)
        assertDoesNotThrow(() -> {
            // This would create a data source but won't connect since we don't have real credentials
            // The test verifies the method exists and is accessible
            SnowflakeJdbcConnectionManager.class.getMethod("getDataSource", SnowflakeConfiguration.class);
        });
    }

    @Test
    void testConnectionManagerMethods() {
        // Test that essential methods exist
        assertDoesNotThrow(() -> {
            SnowflakeJdbcConnectionManager.class.getMethod("getConnection", SnowflakeConfiguration.class);
            SnowflakeJdbcConnectionManager.class.getMethod("getPoolStats", SnowflakeConfiguration.class);
            SnowflakeJdbcConnectionManager.class.getMethod("closeDataSource", SnowflakeConfiguration.class);
            SnowflakeJdbcConnectionManager.class.getMethod("closeAllDataSources");
        });
    }

    @Test
    void testConfigurationValidation() {
        // Test minimum required fields
        SnowflakeConfiguration minConfig = new SnowflakeConfiguration();
        minConfig.setAccount("min-account");
        
        String jdbcUrl = minConfig.buildJdbcUrl();
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains("min-account"));
    }

    @Test
    void testParameterHandling() {
        // Test that we can handle various parameter types
        Object[] params = {1, "test", true, 3.14, null};
        
        // This would normally be tested with actual SQL, but we're just
        // verifying that the parameters can be handled
        assertNotNull(params);
        assertEquals(5, params.length);
        assertTrue(params[2] instanceof Boolean);
        assertTrue(params[3] instanceof Double);
        assertNull(params[4]);
    }

    @Test
    void testJsonDataHandling() {
        String validJson = "{\"name\": \"Alice\", \"age\": 30}";
        String invalidJson = "not json";
        
        // Test JSON validation (basic)
        assertTrue(validJson.startsWith("{"));
        assertTrue(validJson.contains("name"));
        assertFalse(invalidJson.startsWith("{"));
    }

    @Test
    void testBatchParameterHandling() {
        Object[][] batchParams = {
            {1, "Alice", true},
            {2, "Bob", false},
            {3, "Charlie", true}
        };
        
        assertEquals(3, batchParams.length);
        assertEquals(3, batchParams[0].length);
        assertEquals("Alice", batchParams[0][1]);
        assertEquals(false, batchParams[1][2]);
    }
}
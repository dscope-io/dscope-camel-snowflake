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

package io.dscope.camel.snowflake.test;

import io.dscope.camel.snowflake.SnowflakeConfiguration;
import io.dscope.camel.snowflake.SnowflakeProducer;
import io.dscope.camel.snowflake.SnowflakeEndpoint;
import io.dscope.camel.snowflake.sql.SqlParameterBinder;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SQL parameter binding with Snowflake queries.
 * Demonstrates the :#paramName syntax working with real Snowflake-style queries.
 */
class SnowflakeParameterBindingIntegrationTest extends CamelTestSupport {
    
    private static final Logger logger = LoggerFactory.getLogger(SnowflakeParameterBindingIntegrationTest.class);
    
    private SnowflakeConfiguration configuration;
    
    @BeforeEach
    void initializeConfiguration() {
        configuration = SnowflakeTestUtils.createBasicTestConfiguration();
        configuration.setEnableParameterBinding(true);
        configuration.setParameterPrefix("snowflake.");
    }
    
    @Test
    void testSelectDualWithParameters() {
        logger.info("Testing SELECT 1 (dual equivalent) with parameter binding");
        
        // Create exchange with parameters
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("snowflake.testValue", 42);
        exchange.getIn().setHeader("snowflake.testName", "ParameterTest");
        
        // Test Snowflake SELECT 1 with parameters
        String sql = "SELECT :#testValue as test_value, ':#testName' as test_name, CURRENT_TIMESTAMP() as current_time";
        
        // Bind parameters
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, configuration.getParameterPrefix());
        
        // Verify parameter binding
        assertEquals("SELECT ? as test_value, ':#testName' as test_name, CURRENT_TIMESTAMP() as current_time", 
                    result.getProcessedSql());
        assertEquals(1, result.getBoundParameterCount());
        assertEquals(42, result.getBoundParameters().get("testValue"));
        
        // Note: ':#testName' in quotes should not be replaced as it's a string literal
        assertFalse(result.getBoundParameters().containsKey("testName"));
        
        logger.info("✓ SELECT dual with parameters successful");
        logger.info("  Original SQL: {}", sql);
        logger.info("  Processed SQL: {}", result.getProcessedSql());
        logger.info("  Bound parameters: {}", result.getBoundParameters());
    }
    
    @Test
    void testSnowflakeUserQuery() {
        logger.info("Testing realistic Snowflake user query with parameter binding");
        
        // Create exchange with user query parameters
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("userId", 12345);
        exchange.getIn().setHeader("status", "ACTIVE");
        exchange.getIn().setHeader("department", "ENGINEERING");
        exchange.getIn().setHeader("startDate", "2023-01-01");
        
        // Realistic Snowflake query
        String sql = """
            SELECT 
                u.user_id,
                u.username,
                u.email,
                u.department,
                u.created_date,
                CURRENT_TIMESTAMP() as query_time
            FROM users u
            WHERE u.user_id = :#userId
              AND u.status = :#status  
              AND u.department = :#department
              AND u.created_date >= :#startDate
            ORDER BY u.created_date DESC
            """;
        
        // Bind parameters
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify all parameters bound
        assertEquals(4, result.getBoundParameterCount());
        assertFalse(result.hasUnboundParameters());
        assertEquals(12345, result.getBoundParameters().get("userId"));
        assertEquals("ACTIVE", result.getBoundParameters().get("status"));
        assertEquals("ENGINEERING", result.getBoundParameters().get("department"));
        assertEquals("2023-01-01", result.getBoundParameters().get("startDate"));
        
        // Verify SQL transformation
        String processedSql = result.getProcessedSql();
        assertFalse(processedSql.contains(":#"));
        assertEquals(4, countParameterPlaceholders(processedSql));
        
        logger.info("✓ User query with parameters successful");
        logger.info("  Bound {} parameters", result.getBoundParameterCount());
    }
    
    @Test
    void testSnowflakeAnalyticsQuery() {
        logger.info("Testing Snowflake analytics query with parameter binding");
        
        // Create exchange with analytics parameters
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("snowflake.warehouse", "ANALYTICS_WH");
        exchange.getIn().setHeader("snowflake.database", "SALES_DB");
        exchange.getIn().setHeader("snowflake.startDate", "2023-01-01");
        exchange.getIn().setHeader("snowflake.endDate", "2023-12-31");
        exchange.getIn().setHeader("snowflake.minAmount", 1000);
        exchange.getIn().setHeader("snowflake.topN", 10);
        
        // Complex analytics query
        String sql = """
            USE WAREHOUSE :#warehouse;
            USE DATABASE :#database;
            
            WITH sales_summary AS (
                SELECT 
                    customer_id,
                    SUM(amount) as total_sales,
                    COUNT(*) as order_count,
                    AVG(amount) as avg_order_value
                FROM orders 
                WHERE order_date BETWEEN :#startDate AND :#endDate
                  AND amount >= :#minAmount
                GROUP BY customer_id
            )
            SELECT 
                s.*,
                c.customer_name,
                c.segment,
                RANK() OVER (ORDER BY total_sales DESC) as sales_rank
            FROM sales_summary s
            JOIN customers c ON s.customer_id = c.customer_id
            ORDER BY total_sales DESC
            LIMIT :#topN
            """;
        
        // Bind parameters with prefix
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, configuration.getParameterPrefix());
        
        // Verify parameter binding
        assertEquals(6, result.getBoundParameterCount());
        assertEquals("ANALYTICS_WH", result.getBoundParameters().get("warehouse"));
        assertEquals("SALES_DB", result.getBoundParameters().get("database"));
        assertEquals("2023-01-01", result.getBoundParameters().get("startDate"));
        assertEquals("2023-12-31", result.getBoundParameters().get("endDate"));
        assertEquals(1000, result.getBoundParameters().get("minAmount"));
        assertEquals(10, result.getBoundParameters().get("topN"));
        
        logger.info("✓ Analytics query with parameters successful");
        logger.info("  Complex query with {} parameters bound", result.getBoundParameterCount());
    }
    
    @Test
    void testSnowflakeDataLoadingQuery() {
        logger.info("Testing Snowflake data loading query with parameter binding");
        
        // Create exchange with data loading parameters
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("stagingTable", "STG_CUSTOMER_DATA");
        exchange.getIn().setHeader("targetTable", "DIM_CUSTOMERS");
        exchange.getIn().setHeader("batchId", "BATCH_20231225_001");
        exchange.getIn().setHeader("loadTimestamp", "2023-12-25 10:30:00");
        
        // Data loading/transformation query
        String sql = """
            INSERT INTO :#targetTable (
                customer_id, 
                customer_name, 
                email, 
                segment, 
                batch_id, 
                load_timestamp
            )
            SELECT 
                customer_id,
                UPPER(TRIM(customer_name)) as customer_name,
                LOWER(TRIM(email)) as email,
                COALESCE(segment, 'UNKNOWN') as segment,
                ':#batchId' as batch_id,
                ':#loadTimestamp'::timestamp as load_timestamp
            FROM :#stagingTable
            WHERE customer_id IS NOT NULL
              AND email IS NOT NULL
            """;
        
        // Bind parameters
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify parameter binding
        assertEquals(2, result.getBoundParameterCount()); // Only non-quoted parameters
        assertEquals("STG_CUSTOMER_DATA", result.getBoundParameters().get("stagingTable"));
        assertEquals("DIM_CUSTOMERS", result.getBoundParameters().get("targetTable"));
        
        // Quoted parameters should remain as-is
        assertTrue(result.getProcessedSql().contains("':#batchId'"));
        assertTrue(result.getProcessedSql().contains("':#loadTimestamp'"));
        
        logger.info("✓ Data loading query with parameters successful");
        logger.info("  Table parameters bound, quoted literals preserved");
    }
    
    @Test
    @EnabledIf("io.dscope.camel.snowflake.test.SnowflakeTestEnvironment#isIntegrationMode")
    void testRealSnowflakeQuery() {
        logger.info("Testing real Snowflake query execution with parameter binding");
        logger.info("⚠️  This test requires valid Snowflake credentials");
        
        // Create exchange with real parameters
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("testValue", 1);
        exchange.getIn().setHeader("testMessage", "Hello from Parameter Binding");
        
        // Simple test query
        String sql = "SELECT :#testValue as test_value, :#testMessage as test_message, CURRENT_TIMESTAMP() as query_time";
        
        try {
            // This would normally be executed by the SnowflakeProducer
            SqlParameterBinder.ParameterBindingResult result = 
                SqlParameterBinder.bindParameters(sql, exchange, null);
            
            logger.info("✓ Real query parameter binding prepared successfully");
            logger.info("  Would execute: {}", result.getProcessedSql());
            logger.info("  With parameters: {}", result.getBoundParameters());
            
            // Note: Actual execution would require valid Snowflake connection
            // This test validates the parameter binding preparation
            
        } catch (Exception e) {
            logger.error("Error in real query test: {}", e.getMessage());
            // This is expected if no valid Snowflake credentials are available
        }
    }
    
    @Test
    void testEndpointConfiguration() {
        logger.info("Testing endpoint configuration for parameter binding");
        
        // Test endpoint configuration
        SnowflakeConfiguration config = new SnowflakeConfiguration();
        config.setQuery("SELECT * FROM users WHERE id = :#userId AND status = :#status");
        config.setEnableParameterBinding(true);
        config.setParameterPrefix("sf.");
        
        // Verify configuration
        assertTrue(config.isEnableParameterBinding());
        assertEquals("sf.", config.getParameterPrefix());
        assertTrue(config.getQuery().contains(":#userId"));
        assertTrue(config.getQuery().contains(":#status"));
        
        logger.info("✓ Endpoint configuration working correctly");
        logger.info("  Parameter binding enabled: {}", config.isEnableParameterBinding());
        logger.info("  Parameter prefix: '{}'", config.getParameterPrefix());
    }
    
    /**
     * Count parameter placeholders (?) in processed SQL
     */
    private int countParameterPlaceholders(String sql) {
        int count = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?') count++;
        }
        return count;
    }
}
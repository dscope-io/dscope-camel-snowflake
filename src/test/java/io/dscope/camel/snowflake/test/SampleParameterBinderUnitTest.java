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

import io.dscope.camel.snowflake.sql.SqlParameterBinder;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for dynamic parameter binding using the sample table schema.
 * Uses the sample query pattern from the YAML sample: SOME_TABLE with USER_ID and dates.
 */
public class SampleParameterBinderUnitTest extends CamelTestSupport {
    
  private static final Logger logger = LoggerFactory.getLogger(SampleParameterBinderUnitTest.class);
    
    @Test
    void testSampleSimpleParameterQuery() {
        logger.info("Testing sample query with dynamic user_id parameter");
        
        // Create exchange with user_id header
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("user_id", 1);
        
        // SQL query with parameter binding syntax
        String sql = """
            SELECT
              amount
            FROM
              SOME_TABLE
            WHERE USER_ID = :#user_id
            """;
        
        // Process parameter binding
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify results
        assertFalse(result.hasUnboundParameters());
        assertEquals(1, result.getBoundParameterCount());
        assertEquals(1, result.getBoundParameters().get("user_id"));
        
        // Check processed SQL
        String processedSql = result.getProcessedSql();
        assertFalse(processedSql.contains(":#user_id"));
        assertTrue(processedSql.contains("WHERE USER_ID = ?"));
        assertEquals(1, countOccurrences(processedSql, "?"));
        
        logger.info("✓ Sample query parameter binding successful");
        logger.info("  Original SQL: {}", sql.replaceAll("\\s+", " ").trim());
        logger.info("  Processed SQL: {}", processedSql.replaceAll("\\s+", " ").trim());
        logger.info("  Bound parameters: {}", result.getBoundParameters());
        
        // Verify parameter values array for PreparedStatement binding
        Object[] parameterValues = result.getParameterValues();
        assertEquals(1, parameterValues.length);
        assertEquals(1, parameterValues[0]);
    }
    
    @Test
    void testSampleQueryWithMultipleParameters() {
        logger.info("Testing sample query with multiple dynamic parameters");
        
        // Create exchange with multiple headers
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("user_id", 1);
        exchange.getIn().setHeader("min_amount", 50);
        exchange.getIn().setHeader("min_date", "2025-01-01");
        
        // Enhanced SQL query with multiple parameters using sample table
        String sql = """
            SELECT
              amount,
              details,
              created_at
            FROM
              SOME_TABLE
            WHERE USER_ID = :#user_id
              AND AMOUNT > :#min_amount
              AND TO_DATE(CREATED_AT) >= :#min_date
            ORDER BY CREATED_AT DESC
            """;
        
        // Process parameter binding
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify results
        assertFalse(result.hasUnboundParameters());
        assertEquals(3, result.getBoundParameterCount());
        assertEquals(1, result.getBoundParameters().get("user_id"));
        assertEquals(50, result.getBoundParameters().get("min_amount"));
        assertEquals("2025-01-01", result.getBoundParameters().get("min_date"));
        
        // Check processed SQL
        String processedSql = result.getProcessedSql();
        assertFalse(processedSql.contains(":#"));
        assertEquals(3, countOccurrences(processedSql, "?"));
        
        logger.info("✓ Multi-parameter sample query binding successful");
        logger.info("  Bound parameters: {}", result.getBoundParameters());
        logger.info("  Parameter count: {}", result.getBoundParameterCount());
    }
    
    @Test
    void testSampleQueryWithPrefixedHeaders() {
        logger.info("Testing sample query with prefixed headers");
        
        // Create exchange with prefixed headers (snowflake.user_id)
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("snowflake.user_id", 1);
        exchange.getIn().setHeader("snowflake.min_date", "2025-01-01");
        
        // SQL query with parameter binding
        String sql = """
            SELECT
              amount,
              created_at
            FROM
              SOME_TABLE
            WHERE USER_ID = :#user_id
              AND TO_DATE(CREATED_AT) >= :#min_date
            ORDER BY CREATED_AT DESC
            LIMIT 1
            """;
        
        // Process parameter binding with prefix
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, "snowflake.");
        
        // Verify results
        assertFalse(result.hasUnboundParameters());
        assertEquals(2, result.getBoundParameterCount());
        assertEquals(1, result.getBoundParameters().get("user_id"));
        assertEquals("2025-01-01", result.getBoundParameters().get("min_date"));
        
        logger.info("✓ Prefixed header sample query binding successful");
        logger.info("  Bound parameters: {}", result.getBoundParameters());
    }
    
    @Test
    void testSampleQueryWithDateRange() {
        logger.info("Testing sample query with date range parameters");
        
        // Create exchange with date range headers
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("user_id", 1);
        exchange.getIn().setHeader("start_date", "2023-01-01");
        exchange.getIn().setHeader("end_date", "2023-12-31");
        
        // Complex SQL query with date filtering
        String sql = """
            SELECT
              st.details,
              st.user_id,
              st.created_at,
              st.amount
            FROM
              SOME_TABLE st
            WHERE st.USER_ID = :#user_id
              AND DATE(st.created_at) BETWEEN :#start_date AND :#end_date
            ORDER BY st.created_at DESC
            """;
        
        // Process parameter binding
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify results
        assertFalse(result.hasUnboundParameters());
        assertEquals(3, result.getBoundParameterCount());
        assertEquals(1, result.getBoundParameters().get("user_id"));
        assertEquals("2023-01-01", result.getBoundParameters().get("start_date"));
        assertEquals("2023-12-31", result.getBoundParameters().get("end_date"));
        
        // Verify SQL structure
        String processedSql = result.getProcessedSql();
        assertTrue(processedSql.contains("st.USER_ID = ?"));
        assertTrue(processedSql.contains("BETWEEN ? AND ?"));
        assertEquals(3, countOccurrences(processedSql, "?"));
        
        logger.info("✓ Date range sample query binding successful");
        logger.info("  Processed SQL snippet: ...WHERE st.USER_ID = ? AND DATE(st.created_at) BETWEEN ? AND ?...");
    }
    
    @Test
    void testSampleQueryWithPartialBinding() {
        logger.info("Testing sample query with partial parameter binding");
        
        // Create exchange with only some headers
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("user_id", 1);
        // Note: missing 'max_amount' header intentionally
        
        // SQL query with one missing parameter
        String sql = """
            SELECT
              amount,
              details
            FROM
              SOME_TABLE
            WHERE USER_ID = :#user_id
              AND AMOUNT <= :#max_amount
            """;
        
        // Process parameter binding
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify results
        assertTrue(result.hasUnboundParameters());
        assertEquals(1, result.getBoundParameterCount());
        assertEquals(1, result.getUnboundParameters().size());
        assertEquals(1, result.getBoundParameters().get("user_id"));
        assertTrue(result.getUnboundParameters().containsKey("max_amount"));
        
        // Check that bound parameter was replaced but unbound parameter remains
        String processedSql = result.getProcessedSql();
        assertTrue(processedSql.contains("USER_ID = ?"));
        assertTrue(processedSql.contains("AMOUNT <= :#max_amount")); // Should remain unchanged
        
        logger.info("✓ Partial parameter binding handled correctly");
        logger.info("  Bound parameters: {}", result.getBoundParameters());
        logger.info("  Unbound parameters: {}", result.getUnboundParameters().keySet());
    }
    
    /**
     * Helper method to count occurrences of a substring in a string.
     */
    private int countOccurrences(String text, String substring) {
        if (text == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
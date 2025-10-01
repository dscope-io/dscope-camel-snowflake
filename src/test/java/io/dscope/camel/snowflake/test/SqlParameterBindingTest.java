package io.dscope.camel.snowflake.test;

import io.dscope.camel.snowflake.SnowflakeConfiguration;
import io.dscope.camel.snowflake.sql.SqlParameterBinder;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SQL parameter binding functionality.
 * Tests the :#paramName syntax with header value matching.
 */
class SqlParameterBindingTest extends CamelTestSupport {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlParameterBindingTest.class);
    
    @Test
    void testBasicParameterBinding() {
        logger.info("Testing basic parameter binding with :#id and :#status syntax");
        
        // Create exchange with headers
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("id", 123);
        exchange.getIn().setHeader("status", "ACTIVE");
        
        // Test SQL with parameters
        String sql = "SELECT * FROM users WHERE id = :#id AND status = :#status";
        
        // Bind parameters
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify results
        assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", result.getProcessedSql());
        assertEquals(2, result.getBoundParameterCount());
        assertEquals(123, result.getBoundParameters().get("id"));
        assertEquals("ACTIVE", result.getBoundParameters().get("status"));
        assertTrue(result.getUnboundParameters().isEmpty());
        
        logger.info("✓ Basic parameter binding successful");
        logger.info("  Original SQL: {}", sql);
        logger.info("  Processed SQL: {}", result.getProcessedSql());
        logger.info("  Bound parameters: {}", result.getBoundParameters());
    }
    
    @Test
    void testParameterBindingWithPrefix() {
        logger.info("Testing parameter binding with snowflake. prefix");
        
        // Create exchange with prefixed headers
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("snowflake.userId", 456);
        exchange.getIn().setHeader("snowflake.department", "ENGINEERING");
        exchange.getIn().setHeader("snowflake.active", true);
        
        // Test SQL with parameters
        String sql = "SELECT name, email FROM employees WHERE user_id = :#userId AND department = :#department AND active = :#active ORDER BY name";
        
        // Bind parameters with prefix
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, "snowflake.");
        
        // Verify results
        assertEquals("SELECT name, email FROM employees WHERE user_id = ? AND department = ? AND active = ? ORDER BY name", 
                    result.getProcessedSql());
        assertEquals(3, result.getBoundParameterCount());
        assertEquals(456, result.getBoundParameters().get("userId"));
        assertEquals("ENGINEERING", result.getBoundParameters().get("department"));
        assertEquals(true, result.getBoundParameters().get("active"));
        
        logger.info("✓ Prefixed parameter binding successful");
        logger.info("  Bound parameters: {}", result.getBoundParameters());
    }
    
    @Test
    void testMixedParameterBinding() {
        logger.info("Testing mixed parameter binding - some bound, some unbound");
        
        // Create exchange with partial headers
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("id", 789);
        exchange.getIn().setHeader("status", "INACTIVE");
        // Note: missing 'department' header
        
        // Test SQL with parameters
        String sql = "UPDATE users SET status = :#status, last_updated = CURRENT_TIMESTAMP WHERE id = :#id AND department = :#department";
        
        // Bind parameters
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify results
        assertEquals("UPDATE users SET status = ?, last_updated = CURRENT_TIMESTAMP WHERE id = ? AND department = :#department", 
                    result.getProcessedSql());
        assertEquals(2, result.getBoundParameterCount());
        assertEquals(789, result.getBoundParameters().get("id"));
        assertEquals("INACTIVE", result.getBoundParameters().get("status"));
        assertEquals(1, result.getUnboundParameters().size());
        assertTrue(result.getUnboundParameters().containsKey("department"));
        assertTrue(result.hasUnboundParameters());
        
        logger.info("✓ Mixed parameter binding successful");
        logger.info("  Bound parameters: {}", result.getBoundParameters());
        logger.info("  Unbound parameters: {}", result.getUnboundParameters());
    }
    
    @Test
    void testComplexQuery() {
        logger.info("Testing complex query with multiple parameter types");
        
        // Create exchange with various data types
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("startDate", "2023-01-01");
        exchange.getIn().setHeader("endDate", "2023-12-31");
        exchange.getIn().setHeader("minAmount", 1000.50);
        exchange.getIn().setHeader("maxAmount", 50000.00);
        exchange.getIn().setHeader("categories", "PREMIUM,VIP");
        
        // Complex SQL query
        String sql = """
            SELECT u.name, u.email, SUM(o.amount) as total_amount
            FROM users u 
            JOIN orders o ON u.id = o.user_id 
            WHERE o.order_date BETWEEN :#startDate AND :#endDate
              AND o.amount BETWEEN :#minAmount AND :#maxAmount
              AND u.category IN (:#categories)
            GROUP BY u.id, u.name, u.email
            HAVING SUM(o.amount) > :#minAmount
            ORDER BY total_amount DESC
            """;
        
        // Bind parameters
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify results (5 unique parameters: startDate, endDate, minAmount, maxAmount, categories)
        // Note: minAmount appears twice in the query, so 6 ? placeholders total
        assertEquals(5, result.getBoundParameterCount());
        assertFalse(result.hasUnboundParameters());
        assertEquals("2023-01-01", result.getBoundParameters().get("startDate"));
        assertEquals("2023-12-31", result.getBoundParameters().get("endDate"));
        assertEquals(1000.50, result.getBoundParameters().get("minAmount"));
        assertEquals(50000.00, result.getBoundParameters().get("maxAmount"));
        assertEquals("PREMIUM,VIP", result.getBoundParameters().get("categories"));
        
        // Check that all parameters were replaced with ? (6 total since minAmount appears twice)
        String processedSql = result.getProcessedSql();
        assertFalse(processedSql.contains(":#"));
        assertEquals(6, countOccurrences(processedSql, "?"));
        
        logger.info("✓ Complex query parameter binding successful");
        logger.info("  Parameter count: {}", result.getBoundParameterCount());
    }
    
    @Test
    void testSnowflakeSpecificQueries() {
        logger.info("Testing Snowflake-specific queries with parameter binding");
        
        // Create exchange with Snowflake-specific headers
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("tableName", "SALES_DATA");
        exchange.getIn().setHeader("warehouse", "COMPUTE_WH");
        exchange.getIn().setHeader("limit", 100);
        
        // Test various Snowflake queries
        String[] queries = {
            "SELECT * FROM :#tableName LIMIT :#limit",
            "USE WAREHOUSE :#warehouse",
            "SHOW TABLES LIKE ':#tableName%'",
            "SELECT CURRENT_TIMESTAMP(), :#limit as max_rows"
        };
        
        for (String sql : queries) {
            SqlParameterBinder.ParameterBindingResult result = 
                SqlParameterBinder.bindParameters(sql, exchange, null);
            
            logger.info("  Query: {} -> {}", sql, result.getProcessedSql());
            logger.info("  Bound: {}", result.getBoundParameters().keySet());
        }
        
        logger.info("✓ Snowflake-specific queries processed successfully");
    }
    
    @Test
    void testCamelStyleHeaders() {
        logger.info("Testing Camel-style header name matching");
        
        // Create exchange with Camel-style headers
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("CamelSnowflakeUserId", 999);
        exchange.getIn().setHeader("CamelSnowflakeStatus", "PENDING");
        
        // Test SQL with parameters
        String sql = "SELECT * FROM orders WHERE user_id = :#userId AND status = :#status";
        
        // Bind parameters
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(sql, exchange, null);
        
        // Verify results
        assertEquals(2, result.getBoundParameterCount());
        assertEquals(999, result.getBoundParameters().get("userId"));
        assertEquals("PENDING", result.getBoundParameters().get("status"));
        
        logger.info("✓ Camel-style header matching successful");
    }
    
    @Test
    void testParameterBindingConfiguration() {
        logger.info("Testing SnowflakeConfiguration parameter binding settings");
        
        SnowflakeConfiguration config = new SnowflakeConfiguration();
        
        // Test default values
        assertTrue(config.isEnableParameterBinding());
        assertEquals("snowflake.", config.getParameterPrefix());
        
        // Test setters
        config.setEnableParameterBinding(false);
        config.setParameterPrefix("sf.");
        
        assertFalse(config.isEnableParameterBinding());
        assertEquals("sf.", config.getParameterPrefix());
        
        logger.info("✓ Configuration settings working correctly");
    }
    
    @Test
    void testEdgeCases() {
        logger.info("Testing edge cases for parameter binding");
        
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader("test", "value");
        
        // Test empty/null SQL
        assertEquals("", SqlParameterBinder.bindParameters("", exchange, null).getProcessedSql());
        assertEquals("", SqlParameterBinder.bindParameters(null, exchange, null).getProcessedSql());
        
        // Test SQL without parameters
        String noParamSql = "SELECT * FROM users";
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(noParamSql, exchange, null);
        assertEquals(noParamSql, result.getProcessedSql());
        assertEquals(0, result.getBoundParameterCount());
        
        // Test malformed parameter syntax (should be ignored)
        String malformedSql = "SELECT * FROM users WHERE id = :id AND name = #name";
        result = SqlParameterBinder.bindParameters(malformedSql, exchange, null);
        assertEquals(malformedSql, result.getProcessedSql()); // Should remain unchanged
        
        logger.info("✓ Edge cases handled correctly");
    }
    
    /**
     * Helper method to count occurrences of a substring
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
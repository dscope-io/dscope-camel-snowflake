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

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Camel routes using Snowflake component with dynamic parameters.
 * Tests parameter binding functionality with :#paramName syntax in various route scenarios.
 */
public class SnowflakeDynamicParameterRouteTest extends CamelTestSupport {
    
    private static final Logger LOG = LoggerFactory.getLogger(SnowflakeDynamicParameterRouteTest.class);
    
    @EndpointInject("mock:result")
    private MockEndpoint mockResult;
    
    @EndpointInject("mock:error")
    private MockEndpoint mockError;
    
    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        
        // The Snowflake component will be auto-registered via service discovery
        // We'll use endpoint parameters to configure for testing
        
        return camelContext;
    }
    
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                
                // Error handling route
                onException(Exception.class)
                    .handled(true)
                    .log("Error occurred: ${exception.message}")
                    .to("mock:error");
                
                // Route 1: Direct endpoint with single parameter binding
                from("direct:singleParam")
                    .log("Processing single parameter query with user_id: ${header.snowflake.user_id}")
                    .setBody(constant("SELECT name, email FROM users WHERE id = :#user_id"))
                    .to("snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass")
                    .log("Single parameter result: ${body}")
                    .to("mock:result");
                
                // Route 2: Direct endpoint with multiple parameter binding
                from("direct:multipleParams")
                    .log("Processing multiple parameter query")
                    .setBody(constant("SELECT * FROM orders WHERE customer_id = :#customer_id AND status = :#status AND created_date >= :#start_date"))
                    .to("snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass")
                    .log("Multiple parameters result: ${body}")
                    .to("mock:result");
                
                // Route 3: Sample table query with dynamic user_id
                from("direct:sampleTableQuery")
                    .log("Processing sample table query for user_id: ${header.snowflake.user_id}")
                    .setBody(constant("SELECT AMOUNT, DETAILS FROM SOME_TABLE WHERE USER_ID = :#user_id"))
                    .to("snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass")
                    .log("Sample table result: ${body}")
                    .to("mock:result");
                
                // Route 4: REST-like endpoint with parameter extraction from URI
                from("direct:restLikeQuery")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            // Simulate extracting parameters from REST path or query string
                            String requestUri = exchange.getIn().getHeader("requestUri", String.class);
                            if (requestUri != null && requestUri.contains("/users/")) {
                                String userId = requestUri.substring(requestUri.lastIndexOf("/") + 1);
                                exchange.getIn().setHeader("snowflake.user_id", userId);
                                LOG.info("Extracted user_id from URI: {}", userId);
                            }
                        }
                    })
                    .setBody(constant("SELECT * FROM user_profiles WHERE user_id = :#user_id"))
                    .to("snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass")
                    .log("REST-like query result: ${body}")
                    .to("mock:result");
                
                // Route 5: Complex query with conditional parameters
                from("direct:conditionalQuery")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            // Build dynamic query based on available parameters
                            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM products WHERE 1=1");
                            
                            if (exchange.getIn().getHeader("snowflake.category") != null) {
                                queryBuilder.append(" AND category = :#category");
                            }
                            if (exchange.getIn().getHeader("snowflake.min_price") != null) {
                                queryBuilder.append(" AND price >= :#min_price");
                            }
                            if (exchange.getIn().getHeader("snowflake.max_price") != null) {
                                queryBuilder.append(" AND price <= :#max_price");
                            }
                            if (exchange.getIn().getHeader("snowflake.in_stock") != null) {
                                queryBuilder.append(" AND stock_quantity > 0");
                            }
                            
                            String dynamicQuery = queryBuilder.toString();
                            exchange.getIn().setBody(dynamicQuery);
                            LOG.info("Generated dynamic query: {}", dynamicQuery);
                        }
                    })
                    .to("snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass")
                    .log("Conditional query result: ${body}")
                    .to("mock:result");
                
                // Route 6: Batch processing with parameter arrays
                from("direct:batchQuery")
                    .split(body())
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            // Each item in the batch should have its own parameters
                            @SuppressWarnings("unchecked")
                            Map<String, Object> item = exchange.getIn().getBody(Map.class);
                            
                            // Set parameters for this specific item
                            item.forEach((key, value) -> {
                                exchange.getIn().setHeader("snowflake." + key, value);
                            });
                            
                            LOG.info("Processing batch item with parameters: {}", item);
                        }
                    })
                    .setBody(constant("INSERT INTO audit_log (user_id, action, timestamp) VALUES (:#user_id, :#action, :#timestamp)"))
                    .to("snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass")
                    .log("Batch item processed: ${body}")
                    .to("mock:result");
                
                // Route 7: Error handling with invalid parameters
                from("direct:invalidParams")
                    .log("Testing invalid parameter handling")
                    .setBody(constant("SELECT * FROM users WHERE id = :#missing_param AND status = :#another_missing"))
                    .to("snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass")
                    .log("Invalid params result: ${body}")
                    .to("mock:result");
            }
        };
    }
    
    // Mock endpoints are automatically reset between tests in CamelTestSupport
    
    @BeforeEach
    void initSchema() {
        // Ensure H2 in-memory schema exists so SELECT/INSERT queries don't fail
        final String baseUri = "snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass";
        template.sendBody(baseUri, "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(50), email VARCHAR(100))");
        template.sendBody(baseUri, "CREATE TABLE IF NOT EXISTS orders (id INT AUTO_INCREMENT PRIMARY KEY, customer_id INT, status VARCHAR(20), created_date TIMESTAMP)");
    template.sendBody(baseUri, "CREATE TABLE IF NOT EXISTS SOME_TABLE (ID INT AUTO_INCREMENT PRIMARY KEY, USER_ID INT, CREATED_AT TIMESTAMP, AMOUNT DECIMAL(10,2), DETAILS VARCHAR(255))");
        template.sendBody(baseUri, "CREATE TABLE IF NOT EXISTS user_profiles (user_id INT PRIMARY KEY, full_name VARCHAR(100), email VARCHAR(100))");
        template.sendBody(baseUri, "CREATE TABLE IF NOT EXISTS products (id INT PRIMARY KEY, name VARCHAR(100), category VARCHAR(50), price DECIMAL(18,2), stock_quantity INT)");
        template.sendBody(baseUri, "CREATE TABLE IF NOT EXISTS audit_log (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, action VARCHAR(50), timestamp VARCHAR(50))");
    }
    
    @Test
    void testSingleParameterBinding() throws Exception {
        // Setup
        mockResult.expectedMessageCount(1);
        
        // Create headers with parameter
        Map<String, Object> headers = new HashMap<>();
        headers.put("snowflake.user_id", "12345");
        
        // Send message
        template.sendBodyAndHeaders("direct:singleParam", null, headers);
        
        // Verify
        mockResult.assertIsSatisfied();
        
        Exchange resultExchange = mockResult.getReceivedExchanges().get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> boundParams = (Map<String, Object>) resultExchange.getIn().getHeader("CamelSnowflakeBoundParameters");
    Integer paramCount = (Integer) resultExchange.getIn().getHeader("CamelSnowflakeParameterCount");
        
        assertNotNull(boundParams);
        assertEquals(1, paramCount.intValue());
    assertTrue(boundParams.containsKey("user_id"));
        
        LOG.info("Single parameter test completed successfully");
    }
    
    @Test
    void testMultipleParameterBinding() throws Exception {
        // Setup
        mockResult.expectedMessageCount(1);
        
        // Create headers with multiple parameters
        Map<String, Object> headers = new HashMap<>();
        headers.put("snowflake.customer_id", "67890");
        headers.put("snowflake.status", "ACTIVE");
        headers.put("snowflake.start_date", "2025-01-01");
        
        // Send message
        template.sendBodyAndHeaders("direct:multipleParams", null, headers);
        
        // Verify
        mockResult.assertIsSatisfied();
        
        Exchange resultExchange = mockResult.getReceivedExchanges().get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> boundParams = (Map<String, Object>) resultExchange.getIn().getHeader("CamelSnowflakeBoundParameters");
        Integer paramCount = (Integer) resultExchange.getIn().getHeader("CamelSnowflakeParameterCount");
        
        assertNotNull(boundParams);
        assertEquals(3, paramCount.intValue());
    assertTrue(boundParams.containsKey("customer_id"));
    assertTrue(boundParams.containsKey("status"));
    assertTrue(boundParams.containsKey("start_date"));
        
        LOG.info("Multiple parameters test completed successfully");
    }
    
    @Test
    void testSampleTableQuery() throws Exception {
        // Setup
        mockResult.expectedMessageCount(1);
        
        // Create headers for sample scenario
        Map<String, Object> headers = new HashMap<>();
        headers.put("snowflake.user_id", 1);
        
        // Send message
    template.sendBodyAndHeaders("direct:sampleTableQuery", null, headers);
        
        // Verify
        mockResult.assertIsSatisfied();
        
        Exchange resultExchange = mockResult.getReceivedExchanges().get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> boundParams = (Map<String, Object>) resultExchange.getIn().getHeader("CamelSnowflakeBoundParameters");
        
        assertNotNull(boundParams);
    assertTrue(boundParams.containsKey("user_id"));
        
        LOG.info("Sample table test completed successfully");
    }
    
    @Test
    void testRestLikeParameterExtraction() throws Exception {
        // Setup
        mockResult.expectedMessageCount(1);
        
        // Create headers simulating REST request
        Map<String, Object> headers = new HashMap<>();
        headers.put("requestUri", "/api/users/54321");
        
        // Send message
        template.sendBodyAndHeaders("direct:restLikeQuery", null, headers);
        
        // Verify
        mockResult.assertIsSatisfied();
        
        Exchange resultExchange = mockResult.getReceivedExchanges().get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> boundParams = (Map<String, Object>) resultExchange.getIn().getHeader("CamelSnowflakeBoundParameters");
        
        assertNotNull(boundParams);
    assertTrue(boundParams.containsKey("user_id"));
        
        // Verify the extracted parameter value
        String extractedUserId = (String) resultExchange.getIn().getHeader("snowflake.user_id");
        assertEquals("54321", extractedUserId);
        
        LOG.info("REST-like parameter extraction test completed successfully");
    }
    
    @Test
    void testConditionalQueryBuilding() throws Exception {
        // Setup
        mockResult.expectedMessageCount(1);
        
        // Create headers with some optional parameters
        Map<String, Object> headers = new HashMap<>();
        headers.put("snowflake.category", "Electronics");
        headers.put("snowflake.min_price", "100.00");
        headers.put("snowflake.in_stock", "true");
        // Intentionally omit max_price to test conditional logic
        
        // Send message
        template.sendBodyAndHeaders("direct:conditionalQuery", null, headers);
        
        // Verify
        mockResult.assertIsSatisfied();
        
        Exchange resultExchange = mockResult.getReceivedExchanges().get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> boundParams = (Map<String, Object>) resultExchange.getIn().getHeader("CamelSnowflakeBoundParameters");
        
    assertNotNull(boundParams);
    assertTrue(boundParams.containsKey("category"));
    assertTrue(boundParams.containsKey("min_price"));
    assertFalse(boundParams.containsKey("max_price")); // Should not be bound since not provided
        
        LOG.info("Conditional query building test completed successfully");
    }
    
    @Test
    void testBatchProcessing() throws Exception {
        // Setup
        mockResult.expectedMinimumMessageCount(2);
        
        // Create batch data
        Map<String, Object> item1 = new HashMap<>();
        item1.put("user_id", "111");
        item1.put("action", "LOGIN");
        item1.put("timestamp", "2025-09-25 10:00:00");
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("user_id", "222");
        item2.put("action", "LOGOUT");
        item2.put("timestamp", "2025-09-25 11:00:00");
        
        List<Map<String, Object>> batchData = List.of(item1, item2);
        
        // Send batch message
        template.sendBody("direct:batchQuery", batchData);
        
        // Verify
        mockResult.assertIsSatisfied();
        
        List<Exchange> receivedExchanges = mockResult.getReceivedExchanges();
        assertEquals(2, receivedExchanges.size());
        
        // Verify each batch item was processed with its own parameters
        for (Exchange exchange : receivedExchanges) {
            @SuppressWarnings("unchecked")
            Map<String, Object> boundParams = (Map<String, Object>) exchange.getIn().getHeader("CamelSnowflakeBoundParameters");
            assertNotNull(boundParams);
            assertTrue(boundParams.containsKey("user_id"));
            assertTrue(boundParams.containsKey("action"));
            assertTrue(boundParams.containsKey("timestamp"));
        }
        
        LOG.info("Batch processing test completed successfully");
    }
    
    @Test
    void testInvalidParameterHandling() throws Exception {
    // Setup - expect this to be routed to error due to unbound parameters
    mockError.expectedMessageCount(1);
        
        // Send message without required parameters
        template.sendBody("direct:invalidParams", null);
        
        // Verify
    mockError.assertIsSatisfied();
        
    Exchange errorExchange = mockError.getReceivedExchanges().get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> unboundParams = (Map<String, Object>) errorExchange.getIn().getHeader("CamelSnowflakeUnboundParameters");
        
    assertNotNull(unboundParams);
    assertTrue(unboundParams.containsKey("missing_param"));
    assertTrue(unboundParams.containsKey("another_missing"));
        
        LOG.info("Invalid parameter handling test completed successfully");
    }
    
    @Test
    void testParameterBindingDisabled() throws Exception {
        // Setup
        mockResult.expectedMessageCount(1);
        
        // Create a route without parameter binding
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:noBinding")
                    // Build the SQL directly using Camel Simple so no parameter placeholders remain
                    .setBody(simple("SELECT * FROM users WHERE id = ${header.snowflake.user_id}"))
                    .to("snowflake://test?jdbcUrl=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1&username=testuser&password=testpass&enableParameterBinding=false") // Binding disabled via endpoint configuration
                    .to("mock:result");
            }
        });
        
        // Create headers
        Map<String, Object> headers = new HashMap<>();
        headers.put("snowflake.user_id", "12345");
        
        // Send message
        template.sendBodyAndHeaders("direct:noBinding", null, headers);
        
        // Verify
        mockResult.assertIsSatisfied();
        
        Exchange resultExchange = mockResult.getReceivedExchanges().get(0);
        
    // When parameter binding is disabled, headers should indicate zero bound parameters
    assertEquals(0, resultExchange.getIn().getHeader("CamelSnowflakeParameterCount"));
        
        LOG.info("Parameter binding disabled test completed successfully");
    }
}
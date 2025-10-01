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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test demonstrating sample queries in real Camel routes using H2.
 * Shows how to use the SQL parameter binding feature in practice against SOME_TABLE.
 */
public class SampleRouteIntegrationTest extends CamelTestSupport {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleRouteIntegrationTest.class);
    
    @Test
    void testSampleRouteWithDynamicUserId() throws Exception {
        logger.info("Testing sample route with dynamic user_id");
        
        // Setup mock endpoint expectations
        MockEndpoint mockResult = getMockEndpoint("mock:sample-result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("user_id", 1);
        
        // Send message with user_id header
        template.sendBodyAndHeader("direct:sample-query", 
            null, 
            "user_id", 1);
        
        // Verify expectations
        mockResult.assertIsSatisfied();
        
        logger.info("✓ Sample route processed successfully");
    }
    
    @Test 
    void testSampleRouteWithMultipleFilters() throws Exception {
        logger.info("Testing sample route with multiple filter parameters");
        
        // Setup mock endpoint
        MockEndpoint mockResult = getMockEndpoint("mock:filtered-sample-result");
        mockResult.expectedMessageCount(1);
        
        // Send message with multiple headers
        template.sendBodyAndHeaders("direct:sample-query-filtered", 
            null,
            java.util.Map.of(
                "user_id", 1,
                "min_amount", 50,
                "min_date", "2025-01-01"
            ));
        
        mockResult.assertIsSatisfied();
        
        logger.info("✓ Sample filtered route processed successfully");
    }

    @Test
    void testSampleRouteWithDateRange() throws Exception {
        logger.info("Testing sample route with date range filtering");
        
        MockEndpoint mockResult = getMockEndpoint("mock:date-range-sample-result");
        mockResult.expectedMessageCount(1);
        
        // Send message with date range headers
        template.sendBodyAndHeaders("direct:sample-query-date-range",
            null,
            java.util.Map.of(
                "user_id", 1,
                "start_date", "2023-01-01",
                "end_date", "2023-12-31"
            ));
        
        mockResult.assertIsSatisfied();
        
        logger.info("✓ Sample date range route processed successfully");
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                
                // Basic sample query route
                from("direct:sample-query")
                    .routeId("sample-query-route")
                    .log("Processing sample request for user_id: ${header.user_id}")
                    // Map incoming headers to Snowflake parameter prefix expected by binder
                    .setHeader("snowflake.user_id").simple("${header.user_id}")
                    .to("snowflake://sample-query?"
                        + "jdbcUrl=jdbc:h2:mem:sample;DB_CLOSE_DELAY=-1"
                        + "&username=sa"
                        + "&password="
                        + "&query=SELECT AMOUNT FROM SOME_TABLE WHERE USER_ID = :#user_id")
                    .log("Retrieved sample result: ${body}")
                    .to("mock:sample-result");
                
                // Sample query with multiple filters
                from("direct:sample-query-filtered")
                    .routeId("sample-query-filtered-route")
                    .log("Processing filtered sample request")
                    // Map incoming headers
                    .setHeader("snowflake.user_id").simple("${header.user_id}")
                    .setHeader("snowflake.min_amount").simple("${header.min_amount}")
                    .setHeader("snowflake.min_date").simple("${header.min_date}")
                    .to("snowflake://sample-filtered-query?"
                        + "jdbcUrl=jdbc:h2:mem:sample;DB_CLOSE_DELAY=-1"
                        + "&username=sa"
                        + "&password="
                        + "&query=SELECT AMOUNT, DETAILS FROM SOME_TABLE "
                        + "WHERE USER_ID = :#user_id AND AMOUNT > :#min_amount AND CAST(CREATED_AT AS DATE) >= CAST( :#min_date AS DATE) "
                        + "ORDER BY CREATED_AT DESC")
                    .log("Retrieved filtered rows: ${body}")
                    .to("mock:filtered-sample-result");
                
                // Sample query with date range
                from("direct:sample-query-date-range")
                    .routeId("sample-query-date-range-route")
                    .log("Processing sample request with date range")
                    .setHeader("snowflake.user_id").simple("${header.user_id}")
                    .setHeader("snowflake.start_date").simple("${header.start_date}")
                    .setHeader("snowflake.end_date").simple("${header.end_date}")
                    .to("snowflake://sample-date-range-query?"
                        + "jdbcUrl=jdbc:h2:mem:sample;DB_CLOSE_DELAY=-1"
                        + "&username=sa"
                        + "&password="
                        + "&query=SELECT st.DETAILS, st.USER_ID, st.CREATED_AT, st.AMOUNT "
                        + "FROM SOME_TABLE st "
                        + "WHERE st.USER_ID = :#user_id "
                        + "AND CAST(st.CREATED_AT AS DATE) BETWEEN CAST( :#start_date AS DATE) AND CAST( :#end_date AS DATE) "
                        + "ORDER BY st.CREATED_AT DESC")
                    .log("Retrieved date-range rows: ${body}")
                    .to("mock:date-range-sample-result");
            }
        };
    }

    @Override
    protected void doPostSetup() throws Exception {
        // Initialize H2 schema and seed data used by the routes (SOME_TABLE)
        final String initUri = "snowflake://init?jdbcUrl=jdbc:h2:mem:sample;DB_CLOSE_DELAY=-1&username=sa&password=";
        template.sendBody(initUri, "CREATE TABLE IF NOT EXISTS SOME_TABLE (" +
                "ID INT AUTO_INCREMENT PRIMARY KEY, " +
                "USER_ID INT NOT NULL, " +
                "CREATED_AT TIMESTAMP NOT NULL, " +
                "AMOUNT DECIMAL(10,2), " +
                "DETAILS VARCHAR(255))");
        // Clear out previous data
        template.sendBody(initUri, "DELETE FROM SOME_TABLE");
        // Seed rows
    template.sendBody(initUri, "INSERT INTO SOME_TABLE(USER_ID, CREATED_AT, AMOUNT, DETAILS) " +
        "VALUES(1,'2025-09-15 12:00:00',99.95,'order:new')");
    template.sendBody(initUri, "INSERT INTO SOME_TABLE(USER_ID, CREATED_AT, AMOUNT, DETAILS) " +
        "VALUES(1,'2025-09-10 09:30:00',150.00,'order:shipped')");
    template.sendBody(initUri, "INSERT INTO SOME_TABLE(USER_ID, CREATED_AT, AMOUNT, DETAILS) " +
        "VALUES(2,'2025-08-20 10:00:00',49.50,'refund:customer_request')");
    }
}
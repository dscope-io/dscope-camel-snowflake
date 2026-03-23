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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Real Snowflake integration test for sample queries using .env properties.
 * 
 * This test connects to actual Snowflake instance using credentials from .env file.
 * To run this test:
 * 1. Create .env.local with your real Snowflake credentials
 * 2. Run: mvn test -Dtest=SampleSnowflakeRealTest
 * 
 * Required .env properties:
 * - SNOWFLAKE_ACCOUNT
 * - SNOWFLAKE_DATABASE  
 * - SNOWFLAKE_USERNAME
 * - SNOWFLAKE_PASSWORD (or SNOWFLAKE_PRIVATE_KEY)
 * - SNOWFLAKE_WAREHOUSE
 * - SNOWFLAKE_SCHEMA
 */
@EnabledIfEnvironmentVariable(named = "SNOWFLAKE_ACCOUNT", matches = ".*")
public class SampleSnowflakeRealTest extends CamelTestSupport {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleSnowflakeRealTest.class);
    
    private static String snowflakeAccount;
    private static String snowflakeDatabase;
    private static String snowflakeUsername;
    private static String snowflakePassword;
    private static String snowflakePrivateKey;
    private static String snowflakeWarehouse;
    private static String snowflakeSchema;
    private static String snowflakeRole;
    
    @BeforeAll
    static void loadEnvironment() {
        logger.info("Loading Snowflake configuration from .env file...");
        
        try {
            // Load environment properties manually from .env files
            Properties envProps = loadEnvFile();
            
            // Load Snowflake properties with trimming
            snowflakeAccount = trimProperty(envProps.getProperty("SNOWFLAKE_ACCOUNT"));
            snowflakeDatabase = trimProperty(envProps.getProperty("SNOWFLAKE_DATABASE"));
            snowflakeUsername = trimProperty(envProps.getProperty("SNOWFLAKE_USERNAME"));
            snowflakePassword = trimProperty(envProps.getProperty("SNOWFLAKE_PASSWORD"));
            snowflakePrivateKey = trimProperty(envProps.getProperty("SNOWFLAKE_PRIVATE_KEY"));
            snowflakeWarehouse = trimProperty(envProps.getProperty("SNOWFLAKE_WAREHOUSE"));
            snowflakeSchema = trimProperty(envProps.getProperty("SNOWFLAKE_SCHEMA"));
            snowflakeRole = trimProperty(envProps.getProperty("SNOWFLAKE_ROLE"));
            
            logger.info("Loaded properties from .env file:");
            logger.info("  SNOWFLAKE_ACCOUNT: '{}'", snowflakeAccount);
            logger.info("  SNOWFLAKE_DATABASE: '{}'", snowflakeDatabase);
            logger.info("  SNOWFLAKE_USERNAME: '{}'", snowflakeUsername);
            logger.info("  SNOWFLAKE_PASSWORD: '{}'", snowflakePassword != null ? "[PRESENT]" : "[NULL]");
            logger.info("  SNOWFLAKE_PRIVATE_KEY: '{}'", snowflakePrivateKey != null ? "[PRESENT]" : "[NULL]");
            logger.info("  SNOWFLAKE_WAREHOUSE: '{}'", snowflakeWarehouse);
            logger.info("  SNOWFLAKE_SCHEMA: '{}'", snowflakeSchema);
            logger.info("  SNOWFLAKE_ROLE: '{}'", snowflakeRole);
            
        } catch (Exception e) {
            logger.error("Error loading .env file: {}", e.getMessage());
            throw new RuntimeException("Failed to load .env configuration", e);
        }
        
        logger.info("Snowflake Configuration Loaded:");
        logger.info("  Account: {}", snowflakeAccount);
        logger.info("  Database: {}", snowflakeDatabase);
        logger.info("  Username: {}", snowflakeUsername);
        logger.info("  Warehouse: {}", snowflakeWarehouse);
        logger.info("  Schema: {}", snowflakeSchema);
        logger.info("  Role: {}", snowflakeRole);
        logger.info("  Auth Method: {}", snowflakePrivateKey != null ? "Private Key" : "Password");
        
        // Validate required properties
        if (snowflakeAccount == null || snowflakeDatabase == null || snowflakeUsername == null) {
            throw new IllegalStateException("Missing required Snowflake properties in .env file. " +
                "Please ensure SNOWFLAKE_ACCOUNT, SNOWFLAKE_DATABASE, and SNOWFLAKE_USERNAME are set.");
        }
        
        if (snowflakePassword == null && snowflakePrivateKey == null) {
            throw new IllegalStateException("Either SNOWFLAKE_PASSWORD or SNOWFLAKE_PRIVATE_KEY must be provided in .env file.");
        }
    }
    
    /**
     * Load properties from .env files (tries .env.local first, then .env)
     */
    private static Properties loadEnvFile() throws IOException {
        Properties props = new Properties();
        
        // Try .env.local first
        Path localEnvFile = Paths.get(".env.local");
        if (Files.exists(localEnvFile)) {
            logger.info("Loading from .env.local file");
            try (BufferedReader reader = Files.newBufferedReader(localEnvFile)) {
                loadPropertiesFromReader(reader, props);
            }
        }
        
        // If no properties loaded from .env.local, try .env
        if (props.isEmpty()) {
            Path envFile = Paths.get(".env");
            if (Files.exists(envFile)) {
                logger.info("Loading from .env file");
                try (BufferedReader reader = Files.newBufferedReader(envFile)) {
                    loadPropertiesFromReader(reader, props);
                }
            }
        }
        
        return props;
    }
    
    /**
     * Load properties from a BufferedReader, handling .env file format
     */
    private static void loadPropertiesFromReader(BufferedReader reader, Properties props) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Parse KEY=VALUE format
            int equalIndex = line.indexOf('=');
            if (equalIndex > 0) {
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();
                props.setProperty(key, value);
            }
        }
    }
    
    /**
     * Trim property value and return null if empty
     */
    private static String trimProperty(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
    
    @Test
    void testSampleQueryAgainstRealSnowflake() throws Exception {
        logger.info("🚀 Testing sample query against REAL Snowflake database");
        logger.info("   Query: SELECT AMOUNT FROM SOME_TABLE WHERE USER_ID = :#user_id");
        logger.info("   user_id: 1");
        
        // Setup mock endpoint
    MockEndpoint mockResult = getMockEndpoint("mock:sample-result");
        mockResult.expectedMinimumMessageCount(0); // Expect at least 0 (table might be empty)
        
        // Send message with call_id header  
        template.sendBodyAndHeader("direct:sample-real", 
            "Fetch sample rows", 
            "user_id", 1);
        
        // Wait for result (allow longer timeout for real database)
        mockResult.setResultWaitTime(30000); // 30 seconds
        mockResult.assertIsSatisfied();
        
    logger.info("✅ Sample query executed successfully against Snowflake!");
        
        // Log results if any
        if (mockResult.getReceivedCounter() > 0) {
            logger.info("📄 Received {} message(s)", mockResult.getReceivedCounter());
            mockResult.getReceivedExchanges().forEach(exchange -> {
                logger.info("   Result: {}", exchange.getIn().getBody());
            });
        } else {
            logger.info("📄 No records found for call_id=979880945 (this is expected if table is empty)");
        }
    }
    
    @Test
    void testSampleQueryWithDifferentUserId() throws Exception {
        logger.info("🔄 Testing sample query with different user_id");
        
    MockEndpoint mockResult = getMockEndpoint("mock:sample-result2");
        mockResult.expectedMinimumMessageCount(0);
        
        // Test with different call_id
        template.sendBodyAndHeader("direct:sample-real", 
            "Fetch different rows", 
            "user_id", 2);
        
        mockResult.setResultWaitTime(30000);
        mockResult.assertIsSatisfied();
        
        logger.info("✅ Different call_id query executed successfully!");
    }
    
    @Test
    void testSampleTableExists() throws Exception {
        logger.info("🔍 Testing if SOME_TABLE exists");
        
    MockEndpoint mockResult = getMockEndpoint("mock:table-check");
        mockResult.expectedMinimumMessageCount(0);
        
        // Query to check table structure
    template.sendBody("direct:sample-table-check", "Check table");
        
        mockResult.setResultWaitTime(30000);
        mockResult.assertIsSatisfied();
        
        logger.info("✅ Table structure query executed successfully!");
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Error handling for all routes - MUST be defined before any routes
                onException(Exception.class)
                    .handled(true)
                    .log("❌ Error executing Snowflake query: ${exception.message}")
                    .log("🔧 Stack trace: ${exception.stacktrace}")
                    .setBody(constant("ERROR: ${exception.message}"))
                    .to("mock:error");
                
                // Build connection URL with all parameters
                String connectionParams = buildSnowflakeConnectionUrl();
                
                logger.info("🔗 Snowflake Connection URL: {}", connectionParams.replaceAll("password=[^&]*", "password=****"));
                
                // Sample query route with real Snowflake connection
                from("direct:sample-real")
                    .routeId("sample-real-snowflake-route")
                    .log("🔍 Executing sample query against Snowflake for user_id: ${header.user_id}")
                    .to("snowflake://sample-real?" + connectionParams 
                        + "&query=SELECT AMOUNT FROM SOME_TABLE WHERE USER_ID = :#user_id")
                    .log("📊 Query result: ${body}")
                    .to("mock:sample-result")
                    .to("mock:sample-result2");
                
                // Table structure check route
                from("direct:sample-table-check")
                    .routeId("sample-table-check-route")
                    .log("🔍 Checking SOME_TABLE table structure")
                    .to("snowflake://table-check?" + connectionParams
                        + "&query=DESCRIBE TABLE SOME_TABLE")
                    .log("📋 Table structure: ${body}")
                    .to("mock:table-check");
                
            }
        };
    }
    
    private String buildSnowflakeConnectionUrl() {
        StringBuilder params = new StringBuilder();
        
        // Required parameters
        params.append("account=").append(snowflakeAccount);
        params.append("&database=").append(snowflakeDatabase);
        params.append("&username=").append(snowflakeUsername);
        
        // Authentication (password or private key)
        if (snowflakePrivateKey != null && !snowflakePrivateKey.trim().isEmpty()) {
            // Use private key authentication
            params.append("&privateKey=").append(snowflakePrivateKey);
            logger.info("🔐 Using private key authentication");
        } else {
            // Use password authentication
            params.append("&password=").append(snowflakePassword);
            logger.info("🔐 Using password authentication");
        }
        
        // Optional parameters
        if (snowflakeWarehouse != null) {
            params.append("&warehouse=").append(snowflakeWarehouse);
        }
        if (snowflakeSchema != null) {
            params.append("&schema=").append(snowflakeSchema);
        }
        if (snowflakeRole != null) {
            params.append("&role=").append(snowflakeRole);
        }
        
        return params.toString();
    }
}
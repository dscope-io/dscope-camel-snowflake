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

package io.dscope.camel.snowflake;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.dscope.camel.snowflake.test.SnowflakeTestUtils;

/**
 * Configuration test for SnowflakeComponent using CamelTestSupport and test utilities.
 * This test demonstrates best practices for testing Camel components.
 */
public class SnowflakeConfigurationTest extends CamelTestSupport {

    @Test
    public void testBasicConfiguration() throws Exception {
        // Create endpoint with basic configuration
        String uri = String.format(
            "snowflake://basic?account=%s&database=%s&schema=%s&table=%s&warehouse=%s",
            SnowflakeTestUtils.TestConstants.getTestAccount(),
            SnowflakeTestUtils.TestConstants.getTestDatabase(),
            SnowflakeTestUtils.TestConstants.getTestSchema(),
            SnowflakeTestUtils.TestConstants.getTestTable(),
            SnowflakeTestUtils.TestConstants.getTestWarehouse()
        );

        SnowflakeEndpoint endpoint = (SnowflakeEndpoint) context.getEndpoint(uri);
        SnowflakeConfiguration config = endpoint.getConfiguration();

        // Validate using test utilities
        SnowflakeTestUtils.validateConfiguration(config, 
            SnowflakeTestUtils.TestConstants.getTestAccount(),
            SnowflakeTestUtils.TestConstants.getTestDatabase());

        assertEquals(SnowflakeTestUtils.TestConstants.getTestSchema(), config.getSchema());
        assertEquals(SnowflakeTestUtils.TestConstants.getTestTable(), config.getTable());
        assertEquals(SnowflakeTestUtils.TestConstants.getTestWarehouse(), config.getWarehouse());
    }

    @Test
    public void testProducerConfiguration() throws Exception {
        SnowflakeConfiguration config = SnowflakeTestUtils.createProducerTestConfiguration();
        
        assertNotNull(config);
        assertEquals("insert", config.getOperation());
        assertEquals("producer_test_table", config.getTable());
        assertEquals(SnowflakeTestUtils.TestConstants.getTestAccount(), config.getAccount());
    }

    @Test
    public void testConsumerConfiguration() throws Exception {
        SnowflakeConfiguration config = SnowflakeTestUtils.createConsumerTestConfiguration();
        
        assertNotNull(config);
        assertEquals("select", config.getOperation());
        assertEquals("SELECT * FROM consumer_test_table", config.getQuery());
        assertEquals(SnowflakeTestUtils.TestConstants.getTestDatabase(), config.getDatabase());
    }

    @Test
    public void testCustomConfiguration() throws Exception {
        String customAccount = "customaccount";
        String customDatabase = "customdb";
        String customTable = "customtable";
        
        SnowflakeConfiguration config = SnowflakeTestUtils.createCustomConfiguration(
            customAccount, customDatabase, customTable);
        
        assertEquals(customAccount, config.getAccount());
        assertEquals(customDatabase, config.getDatabase());
        assertEquals(customTable, config.getTable());
    }

    @Test
    public void testJdbcUrlBuilding() throws Exception {
        SnowflakeConfiguration config = SnowflakeTestUtils.createBasicTestConfiguration();
        String jdbcUrl = config.buildJdbcUrl();
        
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.contains(SnowflakeTestUtils.TestConstants.getTestAccount()));
        assertTrue(jdbcUrl.contains(SnowflakeTestUtils.TestConstants.getTestDatabase()));
        assertTrue(jdbcUrl.contains("snowflakecomputing.com"));
    }

    @Test
    public void testTestDataGeneration() throws Exception {
        String testData = SnowflakeTestUtils.createTestData("test", 3);
        
        assertNotNull(testData);
        assertTrue(testData.contains("test_record_1"));
        assertTrue(testData.contains("test_record_2"));
        assertTrue(testData.contains("test_record_3"));
        
        // Count the lines
        String[] lines = testData.split("\n");
        assertEquals(3, lines.length);
    }

    @Test
    public void testEndpointWithTestData() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:test-data-result");
        mockEndpoint.expectedMessageCount(1);
        
        String testData = SnowflakeTestUtils.createTestData("snowflake", 2);
        mockEndpoint.expectedBodiesReceived("Processed: " + testData);
        
        template.sendBody("direct:test-data", testData);
        
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPrivateKeyFilePasswordPreferredOverLegacy() {
        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
        // Set legacy first (deprecated) - should be null initially
        assertNull(cfg.getPrivateKeyPassword());
        cfg.setPrivateKeyPassword("legacyPass"); // legacy assignment
        // Set new property afterwards which should override legacy
        cfg.setPrivateKeyFilePassword("newPass");
        assertEquals("newPass", cfg.getPrivateKeyFilePassword(), "Expected new property to take precedence");
        String effectiveLegacy = cfg.getPrivateKeyPassword();
        assertEquals("newPass", effectiveLegacy, "Legacy getter should delegate to new value");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLegacyPrivateKeyPasswordFallback() {
        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
        cfg.setPrivateKeyPassword("legacyOnly"); // only legacy set
        assertEquals("legacyOnly", cfg.getPrivateKeyFilePassword(), "Legacy value should be returned when new not set");
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:test-data")
                    .log("Processing test data: ${body}")
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        exchange.getMessage().setBody("Processed: " + body);
                    })
                    .to("mock:test-data-result");
            }
        };
    }
}
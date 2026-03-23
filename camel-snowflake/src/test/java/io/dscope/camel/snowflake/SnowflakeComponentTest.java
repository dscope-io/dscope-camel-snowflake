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

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit test for SnowflakeComponent using CamelTestSupport.
 * This test demonstrates how to test the Snowflake component with mock endpoints.
 */
public class SnowflakeComponentTest extends CamelTestSupport {

    @Produce("direct:start")
    protected ProducerTemplate producerTemplate;

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Test route that sends data to Snowflake component
                from("direct:start")
                    .log("Sending message to Snowflake: ${body}")
                    // Use H2 for unit test to avoid real Snowflake authentication
                    .to("snowflake://testConnection?jdbcUrl=jdbc:h2:mem:compTest;DB_CLOSE_DELAY=-1&username=sa&password=")
                    .to("mock:result");
            }
        };
    }

    @Test
    public void testSnowflakeComponentWithMockEndpoints() throws Exception {
        // Set up expectations
        String testMessage = "SELECT 1 AS V";

        resultEndpoint.expectedMessageCount(1);

        // Send test message (a simple H2-friendly query)
        producerTemplate.sendBody(testMessage);

        // Verify expectations
        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testSnowflakeComponentWithHeaders() throws Exception {
        // Set up expectations
    String testMessage = "SELECT 1 AS V";
        
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived("testHeader", "testValue");

        // Send test message with headers
        producerTemplate.sendBodyAndHeader(testMessage, "testHeader", "testValue");

        // Verify expectations
        resultEndpoint.assertIsSatisfied();
        
        // Verify the header was preserved
        Exchange receivedExchange = resultEndpoint.getReceivedExchanges().get(0);
        assertEquals("testValue", receivedExchange.getIn().getHeader("testHeader"));
    }

    @Test
    public void testSnowflakeEndpointConfiguration() throws Exception {
        // Test that the endpoint is properly configured
        SnowflakeEndpoint endpoint = (SnowflakeEndpoint) context.getEndpoint(
            "snowflake://testConnection?jdbcUrl=jdbc:h2:mem:compTest2;DB_CLOSE_DELAY=-1&username=sa&password=&schema=myschema&table=mytable&warehouse=mywh"
        );
        
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConfiguration());
        
    SnowflakeConfiguration config = endpoint.getConfiguration();
    // With explicit jdbcUrl we don't populate account/database automatically in tests
    assertEquals("myschema", config.getSchema());
    assertEquals("mytable", config.getTable());
    assertEquals("mywh", config.getWarehouse());
    }
}
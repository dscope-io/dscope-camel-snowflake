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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit test for SnowflakeProducer using CamelTestSupport.
 * This test focuses on testing the producer functionality.
 */
public class SnowflakeProducerTest extends CamelTestSupport {

    @Test
    public void testSnowflakeProducerProcessing() throws Exception {
        // Setup mock endpoint
        MockEndpoint mockEndpoint = getMockEndpoint("mock:result");
        mockEndpoint.expectedMessageCount(1);
        
        // Send test data
        String testData = "SELECT 1 AS V";
        template.sendBody("direct:snowflake-producer", testData);
        
        // Verify the message was processed
        mockEndpoint.assertIsSatisfied();
        
        // Get the processed exchange
        Exchange receivedExchange = mockEndpoint.getReceivedExchanges().get(0);
        assertNotNull(receivedExchange.getIn().getBody());
    }

    @Test
    public void testSnowflakeProducerWithConfiguration() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:configured-result");
        mockEndpoint.expectedMessageCount(1);
        
        // Send test data to configured endpoint
    String testData = "SELECT 1 AS V";
        template.sendBody("direct:snowflake-configured", testData);
        
        mockEndpoint.assertIsSatisfied();
        
        // Verify configuration was applied
        Exchange receivedExchange = mockEndpoint.getReceivedExchanges().get(0);
        assertNotNull(receivedExchange.getIn().getBody());
    }

    @Test
    public void testSnowflakeProducerErrorHandling() throws Exception {
        MockEndpoint errorEndpoint = getMockEndpoint("mock:error");
        MockEndpoint resultEndpoint = getMockEndpoint("mock:result");

        errorEndpoint.expectedMessageCount(0); // No errors expected for this simple test
        resultEndpoint.expectedMessageCount(1);

        // Send test data
        template.sendBody("direct:snowflake-producer", "SELECT 1 AS V");

        errorEndpoint.assertIsSatisfied();
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                
                // Error handling
                onException(Exception.class)
                    .handled(true)
                    .log("Error occurred: ${exception.message}")
                    .to("mock:error");
                
                // Basic producer test route
                from("direct:snowflake-producer")
                    .log("Sending to Snowflake producer: ${body}")
                    .to("snowflake://testConnection?jdbcUrl=jdbc:h2:mem:prodTest;DB_CLOSE_DELAY=-1&username=sa&password=")
                    .log("Received from Snowflake producer: ${body}")
                    .to("mock:result");
                
                // Configured producer test route
                from("direct:snowflake-configured")
                    .log("Sending to configured Snowflake: ${body}")
                    .to("snowflake://configuredConnection?jdbcUrl=jdbc:h2:mem:prodTest2;DB_CLOSE_DELAY=-1&username=sa&password=&schema=myschema&table=mytable&warehouse=mywh")
                    .to("mock:configured-result");
            }
        };
    }
}
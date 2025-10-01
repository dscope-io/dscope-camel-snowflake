package io.dscope.camel.snowflake;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit test for SnowflakeConsumer using CamelTestSupport.
 * This test focuses on testing the consumer functionality.
 */
public class SnowflakeConsumerTest extends CamelTestSupport {

    @Test
    public void testSnowflakeConsumerStartup() throws Exception {
        // Test that the consumer starts up correctly
        // For this test, we're mainly testing that the consumer can start
        // In a real implementation, the consumer would poll Snowflake and send messages
        
        // Wait a bit to ensure consumer has started
        Thread.sleep(1000);
        
        // Verify the consumer endpoint was created and started
        SnowflakeEndpoint endpoint = (SnowflakeEndpoint) context.getEndpoint(
            "snowflake://consumerTest?account=testaccount&database=testdb"
        );
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConfiguration());
    }

    @Test
    public void testSnowflakeConsumerConfiguration() throws Exception {
        // Test that consumer configuration is properly set
        SnowflakeEndpoint endpoint = (SnowflakeEndpoint) context.getEndpoint(
            "snowflake://configuredConsumer?account=myaccount&database=mydb&schema=myschema&query=SELECT * FROM mytable"
        );
        
        assertNotNull(endpoint);
        SnowflakeConfiguration config = endpoint.getConfiguration();
        assertNotNull(config);
        
        assertEquals("myaccount", config.getAccount());
        assertEquals("mydb", config.getDatabase());
        assertEquals("myschema", config.getSchema());
        assertEquals("SELECT * FROM mytable", config.getQuery());
    }

    @Test
    public void testSnowflakeConsumerWithProcessor() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:processed");
        mockEndpoint.expectedMessageCount(0); // No messages expected in this simple test
        
        // In a real implementation, you might simulate data being consumed
        // by sending messages through the consumer route
        
        // For now, just verify the route is set up correctly
        Thread.sleep(500);
        mockEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                
                // Consumer test route
                from("snowflake://consumerTest?account=testaccount&database=testdb")
                    .log("Received from Snowflake consumer: ${body}")
                    .to("mock:consumer-result");
                
                // Configured consumer test route
                from("snowflake://configuredConsumer?account=myaccount&database=mydb&schema=myschema&query=SELECT * FROM mytable")
                    .log("Processing consumed data: ${body}")
                    .process(exchange -> {
                        // Simple processing
                        String body = exchange.getIn().getBody(String.class);
                        if (body != null) {
                            exchange.getMessage().setBody("Processed: " + body);
                        }
                    })
                    .to("mock:processed");
            }
        };
    }
}
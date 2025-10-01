package io.dscope.camel.snowflake;

import org.apache.camel.Component;
import org.apache.camel.impl.DefaultCamelContext;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that the Snowflake component is properly registered with Camel.
 */
public class SnowflakeComponentRegistrationTest {

    @Test
    public void testComponentRegistration() throws Exception {
        // Create a Camel context
        try (DefaultCamelContext camelContext = new DefaultCamelContext()) {
            // Start the context
            camelContext.start();
            
            // Try to get the snowflake component
            Component snowflakeComponent = camelContext.getComponent("snowflake");
            
            // Verify the component is not null and is the correct type
            assertNotNull(snowflakeComponent, "Snowflake component should be registered");
            assertTrue(snowflakeComponent instanceof SnowflakeComponent, 
                      "Component should be an instance of SnowflakeComponent");
            
            // Verify we can create an endpoint
            assertNotNull(camelContext.getEndpoint("snowflake://test"), 
                         "Should be able to create a snowflake endpoint");
        }
    }
    
    @Test 
    public void testEndpointCreation() throws Exception {
        try (DefaultCamelContext camelContext = new DefaultCamelContext()) {
            camelContext.start();
            
            // Create an endpoint with parameters
            SnowflakeEndpoint endpoint = (SnowflakeEndpoint) camelContext.getEndpoint("snowflake://myConnection");
            
            assertNotNull(endpoint, "Endpoint should be created");
            assertNotNull(endpoint.getConfiguration(), "Endpoint should have configuration");
        }
    }
}
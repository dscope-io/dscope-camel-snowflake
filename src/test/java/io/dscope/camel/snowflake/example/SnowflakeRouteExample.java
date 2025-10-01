package io.dscope.camel.snowflake.example;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * Example demonstrating how to use the Snowflake component in Camel routes.
 * This class shows how the component registration enables automatic discovery
 * of the snowflake:// endpoint scheme.
 */
public class SnowflakeRouteExample {

    public static void main(String[] args) throws Exception {
        // Create Camel context - the component will be automatically discovered
        try (CamelContext context = new DefaultCamelContext()) {
            
            // Add routes that use the snowflake component
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    
                    // Example route: consume from file and send to Snowflake
                    from("file:data/input?noop=true")
                        .log("Processing file: ${header.CamelFileName}")
                        .to("snowflake://dataWarehouse")
                        .log("Data sent to Snowflake successfully");
                    
                    // Example route: consume from Snowflake and log
                    from("snowflake://reports?delay=60000")
                        .log("Received data from Snowflake: ${body}");
                    
                    // Example route: REST endpoint to Snowflake
                    from("rest:post:/api/data")
                        .log("Received REST request: ${body}")
                        .to("snowflake://analytics")
                        .setBody(constant("Data submitted successfully"));

                    // Example route: Endpoint defines dynamic query; headers provide parameters.
                    // Using custom parameter prefix "snowflake." so header snowflake.id binds to :#id
                    from("direct:lookupUser")
                        .setHeader("snowflake.id", simple("${header.userId}"))
                        .to("snowflake:lookup?query=SELECT%20NAME%20FROM%20USERS%20WHERE%20ID%20=%20:%23id")
                        .log("Lookup result: ${body}");
                }
            });
            
            // Start the context
            context.start();
            
            System.out.println("Camel routes started. The snowflake component is registered and ready!");
            System.out.println("Available endpoints:");
            System.out.println("- snowflake://dataWarehouse - for sending data to Snowflake");
            System.out.println("- snowflake://reports - for consuming data from Snowflake");
            
            // Keep the application running for demonstration
            Thread.sleep(5000);
            
            System.out.println("Stopping Camel context...");
        }
    }
}
package io.dscope.camel.snowflake.sample;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.main.Main;

/**
 * One-shot runner that starts Camel, sends a single exchange to the
 * direct:snowflakeQuery route with sample headers, prints the result, and exits.
 */
public class RunOnce {
    public static void main(String[] args) throws Exception {
        // Load .env first to map configuration (except private key) into System properties
        EnvLoader.load();

        Main main = new Main();
        // Start Camel (routes are auto-loaded via application.properties)
        main.start();
        try {
            FluentProducerTemplate ftp = main.getCamelContext().createFluentProducerTemplate();

            Object result = ftp
                .to("direct:snowflakeQuery")
                // Use sample parameters that match the seeded data in setup.sql
                .withHeader("user_id", 1)
                .withHeader("min_date", "2025-09-01")
                .request();

            System.out.println("Snowflake query result: " + result);
        } finally {
            main.stop();
        }
    }
}

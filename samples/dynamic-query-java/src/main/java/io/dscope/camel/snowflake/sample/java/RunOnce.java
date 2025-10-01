package io.dscope.camel.snowflake.sample.java;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.main.Main;

/**
 * One-shot runner that starts Camel, sends a single exchange to the
 * direct:snowflakeQuery route with sample headers, prints the result, and exits.
 */
public class RunOnce {
    public static void main(String[] args) throws Exception {
        EnvLoader.load();
        Main main = new Main();
        main.configure().addRoutesBuilder(new DynamicQueryRoute());
        main.start();
        try {
            FluentProducerTemplate ftp = main.getCamelContext().createFluentProducerTemplate();
            Object result = ftp
                    .to("direct:snowflakeQuery")
                    .withHeader("user_id", 1)
                    .withHeader("min_date", "2025-09-01")
                    .request();
            System.out.println("Snowflake query result: " + result);
        } finally {
            main.stop();
        }
    }
}

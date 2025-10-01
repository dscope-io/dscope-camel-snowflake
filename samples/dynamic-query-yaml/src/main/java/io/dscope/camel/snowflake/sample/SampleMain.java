package io.dscope.camel.snowflake.sample;

import org.apache.camel.main.Main;

/**
 * Standalone sample runner for the YAML dynamic Snowflake query route.
 */
public class SampleMain {
    public static void main(String[] args) throws Exception {
        // Load .env (if present) and map to system properties before Camel starts
        EnvLoader.load();
        Main main = new Main();
        // application.properties and YAML routes are auto-loaded via Camel Main
        main.run(args);
    }
}

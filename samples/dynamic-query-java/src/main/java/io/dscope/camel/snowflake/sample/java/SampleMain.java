package io.dscope.camel.snowflake.sample.java;

import org.apache.camel.main.Main;

/**
 * Standalone sample runner for the Java DSL dynamic Snowflake query route.
 */
public class SampleMain {
    public static void main(String[] args) throws Exception {
        EnvLoader.load();
        Main main = new Main();
        main.configure().addRoutesBuilder(new DynamicQueryRoute());
        main.run(args);
    }
}

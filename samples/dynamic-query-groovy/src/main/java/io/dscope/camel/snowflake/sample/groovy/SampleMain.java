package io.dscope.camel.snowflake.sample.groovy;

import org.apache.camel.main.Main;

public class SampleMain {
    public static void main(String[] args) throws Exception {
        EnvLoader.load();
    Main main = new Main();
    main.configure().addRoutesBuilder(new DynamicQueryRoute());
        main.run(args);
    }
}

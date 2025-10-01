package io.dscope.camel.snowflake.sample.java;

import org.apache.camel.builder.RouteBuilder;

/**
 * Java DSL route mirroring the YAML dynamic query sample.
 */
public class DynamicQueryRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        // Resolve properties with defaults (same keys as YAML sample)
        String account = "{{snowflake.account}}";
        String database = "{{snowflake.database}}";
        String schema = "{{snowflake.schema}}";
        String warehouse = "{{snowflake.warehouse}}";
        String role = "{{snowflake.role}}";
        String authenticator = "{{snowflake.authenticator:snowflake_jwt}}";
        String enableBinding = "{{snowflake.enableParameterBinding:true}}";
        String paramPrefix = "{{snowflake.parameterPrefix:snowflake.}}";
        String defaultQuery = "{{snowflake.query}}";
        String outputFormat = "{{snowflake.outputFormat:rows}}";

        // URI constructed similarly to the YAML route
        String snowflakeUri = String.format(
            "snowflake://default?account=%s&database=%s&schema=%s&warehouse=%s&role=%s&authenticator=%s&enableParameterBinding=%s&parameterPrefix=%s&query=%s&outputFormat=%s",
            account, database, schema, warehouse, role, authenticator, enableBinding, paramPrefix, defaultQuery, outputFormat
        );

        from("direct:snowflakeQuery")
            .routeId("snowflake-dynamic-query-java")
            // Choose SQL from header 'sql' or default property
            .setHeader("CamelSnowflakeQuery").simple("${header.sql:-" + defaultQuery + "}")
            // Named parameters from headers with prefix 'snowflake.' by default
            .setHeader("snowflake.user_id").simple("${header.user_id:-{{snowflake.param.user_id:0}}}")
            .setHeader("snowflake.min_date").simple("${header.min_date:-{{snowflake.param.min_date:1970-01-01}}}")
            // Execute query
            .to(snowflakeUri)
            // Log results
            .log("Snowflake result: ${body}");
    }
}

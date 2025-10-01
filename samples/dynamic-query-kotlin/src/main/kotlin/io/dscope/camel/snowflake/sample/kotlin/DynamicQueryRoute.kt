package io.dscope.camel.snowflake.sample.kotlin

import org.apache.camel.builder.RouteBuilder

class DynamicQueryRoute : RouteBuilder() {
    override fun configure() {
        val account = "{{snowflake.account}}"
        val database = "{{snowflake.database}}"
        val schema = "{{snowflake.schema}}"
        val warehouse = "{{snowflake.warehouse}}"
        val role = "{{snowflake.role}}"
        val authenticator = "{{snowflake.authenticator:snowflake_jwt}}"
        val enableBinding = "{{snowflake.enableParameterBinding:true}}"
        val paramPrefix = "{{snowflake.parameterPrefix:snowflake.}}"
        val defaultQuery = "{{snowflake.query}}"
        val outputFormat = "{{snowflake.outputFormat:rows}}"

        val snowflakeUri = "snowflake://default?account=$account&database=$database&schema=$schema&warehouse=$warehouse&role=$role&authenticator=$authenticator&enableParameterBinding=$enableBinding&parameterPrefix=$paramPrefix&query=$defaultQuery&outputFormat=$outputFormat"

        from("direct:snowflakeQuery")
            .routeId("snowflake-dynamic-query-kotlin")
            .setHeader("CamelSnowflakeQuery", header("sql"))
            .choice()
                .`when`(header("CamelSnowflakeQuery").isNull())
                    .setHeader("CamelSnowflakeQuery").constant(defaultQuery)
            .end()
            .setHeader("snowflake.user_id", header("user_id"))
            .setHeader("snowflake.min_date", header("min_date"))
            .to(snowflakeUri)
            .log("Snowflake result: \${body}")
    }
}

package io.dscope.camel.snowflake.sample.groovy

import org.apache.camel.builder.RouteBuilder

class DynamicQueryRoute extends RouteBuilder {
    @Override
    void configure() throws Exception {
        String account = "{{snowflake.account}}"
        String database = "{{snowflake.database}}"
        String schema = "{{snowflake.schema}}"
        String warehouse = "{{snowflake.warehouse}}"
        String role = "{{snowflake.role}}"
        String authenticator = "{{snowflake.authenticator:snowflake_jwt}}"
        String enableBinding = "{{snowflake.enableParameterBinding:true}}"
        String paramPrefix = "{{snowflake.parameterPrefix:snowflake.}}"
        String defaultQuery = "{{snowflake.query}}"
        String outputFormat = "{{snowflake.outputFormat:rows}}"

        String snowflakeUri = "snowflake://default?account=${account}&database=${database}&schema=${schema}&warehouse=${warehouse}&role=${role}&authenticator=${authenticator}&enableParameterBinding=${enableBinding}&parameterPrefix=${paramPrefix}&query=${defaultQuery}&outputFormat=${outputFormat}"

        from('direct:snowflakeQuery')
            .routeId('snowflake-dynamic-query-groovy')
            .setHeader('CamelSnowflakeQuery', header('sql'))
            .choice()
                .when(header('CamelSnowflakeQuery').isNull())
                    .setHeader('CamelSnowflakeQuery').constant(defaultQuery)
            .end()
            .setHeader('snowflake.user_id', header('user_id'))
            .setHeader('snowflake.min_date', header('min_date'))
            .to(snowflakeUri)
            .log('Snowflake result: ${body}')
    }
}

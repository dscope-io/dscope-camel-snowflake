package io.dscope.camel.snowflake.sample.kotlin

import org.apache.camel.main.Main

fun main(args: Array<String>) {
    EnvLoader.load()
    val main = Main()
    main.configure().addRoutesBuilder(DynamicQueryRoute())
    main.start()
    try {
        val ftp = main.camelContext.createFluentProducerTemplate()
        val result: Any? = ftp
            .to("direct:snowflakeQuery")
            .withHeader("user_id", 1)
            .withHeader("min_date", "2025-09-01")
            .request()
        println("Snowflake query result: $result")
    } finally {
        main.stop()
    }
}

package io.dscope.camel.snowflake.sample.kotlin

import org.apache.camel.main.Main

fun main(args: Array<String>) {
    EnvLoader.load()
    val main = Main()
    main.configure().addRoutesBuilder(DynamicQueryRoute())
    main.run(args)
}

package io.dscope.camel.snowflake.sample.kotlin

import io.github.cdimascio.dotenv.Dotenv

object EnvLoader {
    fun load() {
        val useDotenv = java.lang.Boolean.getBoolean("sample.useDotenv")
        if (useDotenv) {
            val dotenv = Dotenv.configure().ignoreIfMissing().load()
            setIfPresent(dotenv, "SNOWFLAKE_ACCOUNT", "snowflake.account")
            setIfPresent(dotenv, "SNOWFLAKE_DATABASE", "snowflake.database")
            setIfPresent(dotenv, "SNOWFLAKE_SCHEMA", "snowflake.schema")
            setIfPresent(dotenv, "SNOWFLAKE_WAREHOUSE", "snowflake.warehouse")
            setIfPresent(dotenv, "SNOWFLAKE_ROLE", "snowflake.role")
            setIfPresent(dotenv, "SNOWFLAKE_USERNAME", "snowflake.username")
        }
    }

    private fun setIfPresent(dotenv: Dotenv, envKey: String, sysProp: String) {
        val v = dotenv.get(envKey)
        if (!v.isNullOrBlank()) System.setProperty(sysProp, v)
    }
}

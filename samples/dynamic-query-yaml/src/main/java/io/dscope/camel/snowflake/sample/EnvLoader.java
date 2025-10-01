package io.dscope.camel.snowflake.sample;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads environment variables from a .env file (if present) and maps them
 * into System properties consumed by application.properties for the sample.
 */
public final class EnvLoader {
    private EnvLoader() {}

    public static void load() {
        // Only load .env if explicitly enabled via system property
        boolean useDotenv = Boolean.getBoolean("sample.useDotenv");
        if (useDotenv) {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // Map .env variables to System properties expected by the sample
            setIfPresent(dotenv, "SNOWFLAKE_ACCOUNT", "snowflake.account");
            setIfPresent(dotenv, "SNOWFLAKE_DATABASE", "snowflake.database");
            setIfPresent(dotenv, "SNOWFLAKE_SCHEMA", "snowflake.schema");
            setIfPresent(dotenv, "SNOWFLAKE_WAREHOUSE", "snowflake.warehouse");
            setIfPresent(dotenv, "SNOWFLAKE_ROLE", "snowflake.role");
            setIfPresent(dotenv, "SNOWFLAKE_USERNAME", "snowflake.username");
            // Still do NOT map private key from .env
        }
    }

    private static void setIfPresent(Dotenv dotenv, String envKey, String sysProp) {
        String val = dotenv.get(envKey);
        if (val != null && !val.isBlank()) {
            System.setProperty(sysProp, val);
        }
    }
}

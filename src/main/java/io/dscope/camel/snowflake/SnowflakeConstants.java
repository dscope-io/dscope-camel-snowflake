package io.dscope.camel.snowflake;

/**
 * Header names enabling per-exchange dynamic override of Snowflake parameters.
 * Only non-null / non-empty headers are applied when processing a message.
 */
public final class SnowflakeConstants {
    private SnowflakeConstants() {}

    public static final String HEADER_ACCOUNT = "CamelSnowflakeAccount";
    public static final String HEADER_USERNAME = "CamelSnowflakeUsername";
    public static final String HEADER_PASSWORD = "CamelSnowflakePassword";
    public static final String HEADER_PRIVATE_KEY = "CamelSnowflakePrivateKey";
    public static final String HEADER_DATABASE = "CamelSnowflakeDatabase";
    public static final String HEADER_SCHEMA = "CamelSnowflakeSchema";
    public static final String HEADER_WAREHOUSE = "CamelSnowflakeWarehouse";
    public static final String HEADER_ROLE = "CamelSnowflakeRole";
    public static final String HEADER_QUERY = "CamelSnowflakeQuery";
    public static final String HEADER_OPERATION = "CamelSnowflakeOperation"; // select/insert/update/delete
    public static final String HEADER_JDBC_URL = "CamelSnowflakeJdbcUrl"; // full override
    public static final String HEADER_AUTHENTICATOR = "CamelSnowflakeAuthenticator";
    public static final String HEADER_PARAMETER_PREFIX = "CamelSnowflakeParameterPrefix";
    public static final String HEADER_ENABLE_PARAMETER_BINDING = "CamelSnowflakeEnableParameterBinding"; // boolean
}

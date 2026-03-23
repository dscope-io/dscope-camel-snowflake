/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    // Additional optional auth-related overrides (not always present)
    public static final String HEADER_PRIVATE_KEY_FILE = "CamelSnowflakePrivateKeyFile"; // path to private key file
    public static final String HEADER_PRIVATE_KEY_FILE_PASSWORD = "CamelSnowflakePrivateKeyFilePassword"; // password protecting private key file (if any)
    public static final String HEADER_OAUTH_TOKEN = "CamelSnowflakeOauthToken"; // bearer OAuth token for OAuth authentication
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

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

package io.dscope.camel.snowflake.jdbc;

import io.dscope.camel.snowflake.SnowflakeConfiguration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style test for validating connectivity to a real Snowflake account.
 *
 * This test is skipped automatically unless the required environment variables are provided.
 * Supported authentication:
 *  - Username + Password (set SNOWFLAKE_PASSWORD)
 *  - Key Pair / JWT (set SNOWFLAKE_PRIVATE_KEY_BASE64)
 *
 * Required environment variables:
 *  - SNOWFLAKE_ACCOUNT   (e.g. xy12345.us-east-1)
 *  - SNOWFLAKE_USER
 *  - ONE of: SNOWFLAKE_PASSWORD or SNOWFLAKE_PRIVATE_KEY_BASE64
 * Optional:
 *  - SNOWFLAKE_WAREHOUSE
 *  - SNOWFLAKE_DATABASE
 *  - SNOWFLAKE_SCHEMA
 *  - SNOWFLAKE_ROLE
 *  - SNOWFLAKE_AUTHENTICATOR (overrides default; for key pair auth will auto-set snowflake_jwt if not present)
 */
public class SnowflakeConnectionIntegrationTest {

    @Test
    @DisplayName("Snowflake connection test (skipped if env not configured)")
    void testConnectionIfConfigured() {
        String account = System.getenv("SNOWFLAKE_ACCOUNT");
        String user = System.getenv("SNOWFLAKE_USER");
        String password = System.getenv("SNOWFLAKE_PASSWORD");
        String privateKeyBase64 = System.getenv("SNOWFLAKE_PRIVATE_KEY_BASE64");

        // Skip if mandatory basics missing OR neither auth mechanism provided
        Assumptions.assumeTrue(account != null && !account.isBlank(), "SNOWFLAKE_ACCOUNT not set; skipping");
        Assumptions.assumeTrue(user != null && !user.isBlank(), "SNOWFLAKE_USER not set; skipping");
        Assumptions.assumeTrue(
                (password != null && !password.isBlank()) || (privateKeyBase64 != null && !privateKeyBase64.isBlank()),
                "Neither SNOWFLAKE_PASSWORD nor SNOWFLAKE_PRIVATE_KEY_BASE64 provided; skipping"
        );

        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
        cfg.setAccount(account);
        cfg.setUsername(user);
        if (password != null && !password.isBlank()) {
            cfg.setPassword(password);
        } else {
            cfg.setPrivateKey(privateKeyBase64);
        }

        // Optional settings
        maybe(System.getenv("SNOWFLAKE_WAREHOUSE"), cfg::setWarehouse);
        maybe(System.getenv("SNOWFLAKE_DATABASE"), cfg::setDatabase);
        maybe(System.getenv("SNOWFLAKE_SCHEMA"), cfg::setSchema);
        maybe(System.getenv("SNOWFLAKE_ROLE"), cfg::setRole);
        maybe(System.getenv("SNOWFLAKE_AUTHENTICATOR"), cfg::setAuthenticator);

        boolean ok = SnowflakeJdbcOperations.testConnection(cfg);
        assertTrue(ok, "Expected successful connection to Snowflake");
    }

    private static void maybe(String value, java.util.function.Consumer<String> consumer) {
        if (value != null && !value.isBlank()) {
            consumer.accept(value);
        }
    }
}

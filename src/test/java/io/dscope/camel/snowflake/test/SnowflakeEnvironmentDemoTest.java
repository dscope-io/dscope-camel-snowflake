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

package io.dscope.camel.snowflake.test;

import io.dscope.camel.snowflake.SnowflakeConfiguration;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstration test showing how environment configuration can be used
 * for integration testing with real Snowflake instances.
 */
class SnowflakeEnvironmentDemoTest extends CamelTestSupport {

    @Test
    void testDefaultEnvironmentConfiguration() {
        // This test always runs - uses default test values from .env
        SnowflakeConfiguration config = SnowflakeTestUtils.createBasicTestConfiguration();
        
        assertNotNull(config.getAccount());
        assertNotNull(config.getDatabase());
        assertNotNull(config.getUsername());
        // Password may be null if private key auth is used; just ensure one of them is present
        if (config.getPrivateKey() == null || config.getPrivateKey().isBlank()) {
            assertNotNull(config.getPassword());
        }
        // In integration environments values may be real, so avoid asserting exact defaults
    }

    @Test
    @EnabledIf("io.dscope.camel.snowflake.test.SnowflakeTestUtils#shouldRunIntegrationTests")
    void testIntegrationConfiguration() {
        // This test only runs if real Snowflake credentials are provided
        // Either via environment variables or .env.local file
        
        SnowflakeConfiguration config = SnowflakeTestUtils.createIntegrationTestConfiguration();
        
        // Verify we have real credentials (not default test values)
        assertNotEquals("testaccount", config.getAccount());
        assertNotEquals("testuser", config.getUsername());
        
        // Verify the configuration looks valid
        assertTrue(config.getAccount().contains("."), "Account should contain region separator");
        assertFalse(config.getAccount().startsWith("your-"), "Account should not be placeholder");
        assertFalse(config.getUsername().startsWith("your-"), "Username should not be placeholder");
    }

    @Test
    void testEnvironmentVariableOverride() {
        // Test that we can access environment variables directly
        String account = SnowflakeTestEnvironment.get("SNOWFLAKE_ACCOUNT", "fallback");
        assertNotNull(account);
        
        // Test optional value handling
        var privateKey = SnowflakeTestEnvironment.getOptional("SNOWFLAKE_PRIVATE_KEY");
        assertNotNull(privateKey); // Should be present as Optional, but may be empty
        
        // Test integer value handling
        int maxPoolSize = SnowflakeTestEnvironment.getInt("SNOWFLAKE_MAX_POOL_SIZE", 5);
        assertTrue(maxPoolSize >= 5);
    }

    @Test
    void testConfigurationWithPrivateKey() {
        // Test configuration with private key if available
        SnowflakeConfiguration config = SnowflakeTestUtils.createBasicTestConfiguration();
        
        // Private key is optional - test that it's handled properly
        if (SnowflakeTestEnvironment.Snowflake.getPrivateKey() != null) {
            assertEquals(SnowflakeTestEnvironment.Snowflake.getPrivateKey(), config.getPrivateKey());
        } else {
            assertNull(config.getPrivateKey());
        }
    }

    @Test
    void testIntegrationModeDetection() {
        // Test that integration mode detection works
        boolean isIntegration = SnowflakeTestEnvironment.isIntegrationMode();
        
        if (isIntegration) {
            // If in integration mode, credentials should be real
            assertNotEquals("your-account.region", SnowflakeTestEnvironment.Snowflake.getAccount());
            assertNotEquals("your-username", SnowflakeTestEnvironment.Snowflake.getUsername());
        } else {
            // If not in integration mode, ensure values are present (may be defaults or custom CI defaults)
            assertNotNull(SnowflakeTestEnvironment.Snowflake.getAccount());
            assertFalse(SnowflakeTestEnvironment.Snowflake.getAccount().isBlank());
        }
    }
}
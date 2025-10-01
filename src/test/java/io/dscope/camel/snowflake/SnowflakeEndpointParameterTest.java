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

import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for SnowflakeEndpoint parameter handling.
 */
class SnowflakeEndpointParameterTest extends CamelTestSupport {

    @Test
    void testSnowflakeEndpointParameterBinding() throws Exception {
        // Create a Snowflake endpoint with parameters from URI
        SnowflakeEndpoint endpoint = (SnowflakeEndpoint) context.getEndpoint(
            "snowflake://test?account=testaccount&database=testdb&schema=testschema&warehouse=testwarehouse&role=testrole&table=testtable&username=testuser&privateKey=testprivatekey"
        );
        
        // Verify that parameters are correctly bound to the endpoint
        assertNotNull(endpoint);
        assertEquals("testaccount", endpoint.getAccount());
        assertEquals("testdb", endpoint.getDatabase());
        assertEquals("testschema", endpoint.getSchema());
        assertEquals("testwarehouse", endpoint.getWarehouse());
        assertEquals("testrole", endpoint.getRole());
        assertEquals("testtable", endpoint.getTable());
        assertEquals("testuser", endpoint.getUsername());
        assertEquals("testprivatekey", endpoint.getPrivateKey());
        
        // Also verify that parameters are correctly synchronized to configuration
        SnowflakeConfiguration config = endpoint.getConfiguration();
        assertNotNull(config);
        assertEquals("testaccount", config.getAccount());
        assertEquals("testdb", config.getDatabase());
        assertEquals("testtable", config.getTable());
        assertEquals("testuser", config.getUsername());
        assertEquals("testprivatekey", config.getPrivateKey());
    }

    @Test
    void testSnowflakeEndpointParameterDefaults() throws Exception {
        // Create a Snowflake endpoint without parameters
        SnowflakeEndpoint endpoint = context.getEndpoint("snowflake:test", SnowflakeEndpoint.class);

        assertNotNull(endpoint);
        assertNull(endpoint.getAccount());
        assertNull(endpoint.getDatabase());
        assertNull(endpoint.getSchema());
        assertNull(endpoint.getWarehouse());
        assertNull(endpoint.getRole());
    }

    @Test
    void testSnowflakeEndpointParameterSetters() {
        SnowflakeEndpoint endpoint = new SnowflakeEndpoint();

        // Test setters
        endpoint.setAccount("myaccount");
        endpoint.setDatabase("mydb");
        endpoint.setSchema("myschema");
        endpoint.setWarehouse("mywarehouse");
        endpoint.setRole("myrole");
        endpoint.setPrivateKey("myprivatekey");

        // Verify getters return correct values
        assertEquals("myaccount", endpoint.getAccount());
        assertEquals("mydb", endpoint.getDatabase());
        assertEquals("myschema", endpoint.getSchema());
        assertEquals("mywarehouse", endpoint.getWarehouse());
        assertEquals("myrole", endpoint.getRole());
        assertEquals("myprivatekey", endpoint.getPrivateKey());
    }

    @Test
    void testSnowflakeEndpointPartialParameters() throws Exception {
        // Create endpoint with only some parameters
        SnowflakeEndpoint endpoint = context.getEndpoint(
                "snowflake:test?account=myaccount&database=mydb",
                SnowflakeEndpoint.class);

        assertNotNull(endpoint);
        assertEquals("myaccount", endpoint.getAccount());
        assertEquals("mydb", endpoint.getDatabase());
        assertNull(endpoint.getSchema());
        assertNull(endpoint.getWarehouse());
        assertNull(endpoint.getRole());
    }
}
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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Verifies that when the SQL query with :#param placeholders is defined on the endpoint/config,
 * parameter values supplied via headers are bound and the query executes.
 */
public class SnowflakeEndpointQueryBindingTest {

    private static CamelContext context;
    private static ProducerTemplate template;

    @BeforeAll
    static void setup() throws Exception {
        context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                SnowflakeConfiguration base = new SnowflakeConfiguration();
                base.setAccount("test");
                base.setUsername("sa");
                base.setPassword("");
                base.setJdbcUrl("jdbc:h2:mem:epQuery;DB_CLOSE_DELAY=-1");
                // Define dynamic WHERE query at endpoint level
                base.setQuery("SELECT NAME FROM TEST WHERE ID = :#id");

                SnowflakeComponent comp = new SnowflakeComponent();
                getContext().addComponent("snowflake", comp);
                SnowflakeEndpoint endpoint = (SnowflakeEndpoint) getContext().getEndpoint("snowflake:epQuery");
                endpoint.setConfiguration(base);

                from("direct:epQuery").to("snowflake:epQuery");
            }
        });
        context.start();
        template = context.createProducerTemplate();

        // Initialize schema/table and seed data using body-supplied SQL (body takes precedence over endpoint query)
        template.sendBody("direct:epQuery", "CREATE TABLE IF NOT EXISTS TEST(ID INT PRIMARY KEY, NAME VARCHAR(20))");
        template.sendBody("direct:epQuery", "DELETE FROM TEST");
        template.sendBody("direct:epQuery", "INSERT INTO TEST(ID, NAME) VALUES(1,'A')");
        template.sendBody("direct:epQuery", "INSERT INTO TEST(ID, NAME) VALUES(2,'B')");
    }

    @AfterAll
    static void teardown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void testEndpointQueryUsesHeaderParam() {
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rows = template.requestBodyAndHeader("direct:epQuery", null, "id", 2, List.class);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("B", rows.get(0).get("NAME"));
    }

    @Test
    void testEndpointQueryUsesCamelStyleHeader() {
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rows = template.requestBodyAndHeader("direct:epQuery", null, "CamelSnowflakeId", 1, List.class);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("A", rows.get(0).get("NAME"));
    }

    @Test
    void testHeaderQueryOverridesEndpointAndBinds() {
        // Override the endpoint query via header and still bind :#id from headers
        var headers = new java.util.HashMap<String, Object>();
        headers.put("CamelSnowflakeQuery", "SELECT NAME FROM TEST WHERE ID = :#id");
        headers.put("id", 2);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rows = template.requestBodyAndHeaders("direct:epQuery", null, headers, List.class);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("B", rows.get(0).get("NAME"));
    }
}

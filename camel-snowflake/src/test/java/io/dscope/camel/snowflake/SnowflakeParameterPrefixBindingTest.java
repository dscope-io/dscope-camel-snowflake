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

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies binding with a custom parameterPrefix (e.g., snowflake.) where query is defined on endpoint
 * and header key is snowflake.id instead of plain id.
 */
public class SnowflakeParameterPrefixBindingTest {

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
                base.setJdbcUrl("jdbc:h2:mem:paramPrefix;DB_CLOSE_DELAY=-1");
                base.setParameterPrefix("snowflake.");
                base.setQuery("SELECT NAME FROM TEST WHERE ID = :#id");

                SnowflakeComponent comp = new SnowflakeComponent();
                getContext().addComponent("snowflake", comp);
                SnowflakeEndpoint endpoint = (SnowflakeEndpoint) getContext().getEndpoint("snowflake:paramPrefix");
                endpoint.setConfiguration(base);

                from("direct:paramPrefix").to("snowflake:paramPrefix");
            }
        });
        context.start();
        template = context.createProducerTemplate();

        // Seed data
        template.sendBody("direct:paramPrefix", "CREATE TABLE IF NOT EXISTS TEST(ID INT PRIMARY KEY, NAME VARCHAR(20))");
        template.sendBody("direct:paramPrefix", "INSERT INTO TEST(ID, NAME) VALUES(10,'X')");
        template.sendBody("direct:paramPrefix", "INSERT INTO TEST(ID, NAME) VALUES(20,'Y')");
    }

    @AfterAll
    static void teardown() throws Exception {
        if (context != null) context.stop();
    }

    @Test
    void testPrefixedHeaderBinds() {
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rows = template.requestBodyAndHeaders(
                "direct:paramPrefix",
                null,
                Map.of("snowflake.id", 20),
                List.class
        );
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("Y", rows.get(0).get("NAME"));
    }
}

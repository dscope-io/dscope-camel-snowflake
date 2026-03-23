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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SnowflakeOutputFormatTest {

    @BeforeEach
    void setUp() {
        System.setProperty("snowflake.account", "acct.region.azure");
        System.setProperty("snowflake.database", "DB1");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("snowflake.account");
        System.clearProperty("snowflake.database");
        System.clearProperty("snowflake.outputFormat");
    }

    @Test
    void defaultIsJsonFormatForDriver() {
        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
        String url = cfg.buildJdbcUrl();
        assertTrue(url.contains("JDBC_QUERY_RESULT_FORMAT=JSON"), url);
    }

    @Test
    void arrowFormatSwitchesDriverToArrow() {
        System.setProperty("snowflake.outputFormat", "arrow");
        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
        String url = cfg.buildJdbcUrl();
        assertTrue(url.contains("JDBC_QUERY_RESULT_FORMAT=ARROW"), url);
    }
}

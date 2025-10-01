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

package io.dscope.camel.snowflake.sample.groovy;

import io.github.cdimascio.dotenv.Dotenv;

public final class EnvLoader {
    private EnvLoader() {}

    public static void load() {
        boolean useDotenv = Boolean.getBoolean("sample.useDotenv");
        if (useDotenv) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            setIfPresent(dotenv, "SNOWFLAKE_ACCOUNT", "snowflake.account");
            setIfPresent(dotenv, "SNOWFLAKE_DATABASE", "snowflake.database");
            setIfPresent(dotenv, "SNOWFLAKE_SCHEMA", "snowflake.schema");
            setIfPresent(dotenv, "SNOWFLAKE_WAREHOUSE", "snowflake.warehouse");
            setIfPresent(dotenv, "SNOWFLAKE_ROLE", "snowflake.role");
            setIfPresent(dotenv, "SNOWFLAKE_USERNAME", "snowflake.username");
        }
    }

    private static void setIfPresent(Dotenv dotenv, String envKey, String sysProp) {
        String val = dotenv.get(envKey);
        if (val != null && !val.isBlank()) {
            System.setProperty(sysProp, val);
        }
    }
}

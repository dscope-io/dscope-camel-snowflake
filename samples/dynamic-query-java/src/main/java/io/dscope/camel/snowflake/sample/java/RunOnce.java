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

package io.dscope.camel.snowflake.sample.java;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.main.Main;

/**
 * One-shot runner that starts Camel, sends a single exchange to the
 * direct:snowflakeQuery route with sample headers, prints the result, and exits.
 */
public class RunOnce {
    public static void main(String[] args) throws Exception {
        EnvLoader.load();
        Main main = new Main();
        main.configure().addRoutesBuilder(new DynamicQueryRoute());
        main.start();
        try {
            FluentProducerTemplate ftp = main.getCamelContext().createFluentProducerTemplate();
            Object result = ftp
                    .to("direct:snowflakeQuery")
                    .withHeader("user_id", 1)
                    .withHeader("min_date", "2025-09-01")
                    .request();
            System.out.println("Snowflake query result: " + result);
        } finally {
            main.stop();
        }
    }
}

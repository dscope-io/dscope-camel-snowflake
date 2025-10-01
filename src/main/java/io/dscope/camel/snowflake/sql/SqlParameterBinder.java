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

package io.dscope.camel.snowflake.sql;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for SQL parameter binding from Camel message headers.
 * Supports dynamic parameter replacement in SQL queries using :#paramName syntax.
 */
public class SqlParameterBinder {

    private static final Logger LOG = LoggerFactory.getLogger(SqlParameterBinder.class);

    /**
     * Bind parameters in SQL query using values from exchange headers.
     * Skips placeholders that appear inside single-quoted string literals.
     *
     * @param sql The SQL query with :#paramName placeholders
     * @param exchange The Camel exchange containing headers
     * @param parameterPrefix Optional prefix for header names (e.g., "snowflake.")
     * @return ParameterBindingResult containing the processed SQL and parameter info
     */
    public static ParameterBindingResult bindParameters(String sql, Exchange exchange, String parameterPrefix) {
        if (sql == null || sql.trim().isEmpty()) {
            return new ParameterBindingResult(sql == null ? "" : sql, new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        // Use insertion-ordered maps to preserve parameter order for PreparedStatement binding
        Map<String, Object> boundParameters = new LinkedHashMap<>();
        Map<String, Object> unboundParameters = new LinkedHashMap<>();

        StringBuilder out = new StringBuilder(sql.length());
        boolean inString = false;
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);

            if (c == '\'') {
                // Handle single-quoted literals and escaped quotes inside them ('' -> ')
                out.append(c);
                if (inString) {
                    // If next is also a quote, it's an escaped quote inside string literal
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        out.append('\'');
                        i += 2;
                        continue;
                    }
                    inString = false;
                } else {
                    inString = true;
                }
                i++;
                continue;
            }

            if (!inString && c == ':' && (i + 1) < sql.length() && sql.charAt(i + 1) == '#') {
                // Potential parameter placeholder outside string literal
                int start = i;
                i += 2; // skip :#
                int nameStart = i;
                while (i < sql.length()) {
                    char nc = sql.charAt(i);
                    if (Character.isLetterOrDigit(nc) || nc == '_') {
                        i++;
                    } else {
                        break;
                    }
                }
                if (i > nameStart) {
                    String paramName = sql.substring(nameStart, i);
                    String headerKey = buildHeaderKey(paramName, parameterPrefix);
                    Object paramValue = findParameterValue(exchange, paramName, parameterPrefix);
                    if (paramValue != null) {
                        out.append('?');
                        boundParameters.put(paramName, paramValue);
                        LOG.debug("Bound parameter '{}' with value: {}", paramName, paramValue);
                    } else {
                        // No value found, keep original token
                        out.append(sql, start, i);
                        unboundParameters.put(paramName, headerKey);
                        LOG.warn("No value found for parameter '{}' (looked for header '{}')", paramName, headerKey);
                    }
                    continue;
                } else {
                    // Not a valid name after :#, just append what we have and continue
                    out.append(sql, start, i);
                    continue;
                }
            }

            out.append(c);
            i++;
        }

        String finalSql = out.toString();
        LOG.info("SQL parameter binding completed. Original: '{}', Processed: '{}', Bound: {}, Unbound: {}",
                sql, finalSql, boundParameters.keySet(), unboundParameters.keySet());

        return new ParameterBindingResult(finalSql, boundParameters, unboundParameters);
    }

    /**
     * Find parameter value in exchange headers.
     * Tries multiple header key patterns:
     * 1. Direct parameter name (e.g., "id")
     * 2. With parameter prefix (e.g., "snowflake.id")
     * 3. Camel style (e.g., "CamelSnowflakeId")
     */
    private static Object findParameterValue(Exchange exchange, String paramName, String parameterPrefix) {
        Map<String, Object> headers = exchange.getIn().getHeaders();

        // Try direct parameter name first
        Object value = headers.get(paramName);
        if (value != null) {
            return value;
        }

        // Try with parameter prefix
        if (parameterPrefix != null && !parameterPrefix.isEmpty()) {
            String prefixedKey = parameterPrefix + paramName;
            value = headers.get(prefixedKey);
            if (value != null) {
                return value;
            }
        }

        // Try Camel-style header name
        String camelStyleKey = "CamelSnowflake" + capitalize(paramName);
        value = headers.get(camelStyleKey);
        if (value != null) {
            return value;
        }

        // Try case-insensitive search
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            String headerKey = entry.getKey();
            if (headerKey != null &&
                (headerKey.equalsIgnoreCase(paramName) ||
                 (parameterPrefix != null && headerKey.equalsIgnoreCase(parameterPrefix + paramName)))) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Build the expected header key for a parameter.
     */
    private static String buildHeaderKey(String paramName, String parameterPrefix) {
        if (parameterPrefix != null && !parameterPrefix.isEmpty()) {
            return parameterPrefix + paramName;
        }
        return paramName;
    }

    /**
     * Capitalize first letter of a string.
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Result of parameter binding operation.
     */
    public static class ParameterBindingResult {
        private final String processedSql;
        private final Map<String, Object> boundParameters;
        private final Map<String, Object> unboundParameters;

        public ParameterBindingResult(String processedSql, Map<String, Object> boundParameters, Map<String, Object> unboundParameters) {
            this.processedSql = processedSql;
            this.boundParameters = boundParameters;
            this.unboundParameters = unboundParameters;
        }

        public String getProcessedSql() {
            return processedSql;
        }

        public Map<String, Object> getBoundParameters() {
            return boundParameters;
        }

        public Map<String, Object> getUnboundParameters() {
            return unboundParameters;
        }

        public boolean hasUnboundParameters() {
            return !unboundParameters.isEmpty();
        }

        public int getBoundParameterCount() {
            return boundParameters.size();
        }

        /**
         * Get parameter values in order for PreparedStatement binding.
         */
        public Object[] getParameterValues() {
            return boundParameters.values().toArray();
        }
    }
}
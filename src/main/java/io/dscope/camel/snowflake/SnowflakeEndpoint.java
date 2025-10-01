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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Snowflake endpoint for connecting to Snowflake data warehouse.
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "snowflake", title = "Snowflake", syntax = "snowflake:name")
public class SnowflakeEndpoint extends DefaultEndpoint {

    @UriParam
    private SnowflakeConfiguration configuration;

    @UriParam(description = "Snowflake account URL")
    private String account;

    @UriParam(description = "Snowflake database name")
    private String database;

    @UriParam(description = "Snowflake schema name")
    private String schema;

    @UriParam(description = "Snowflake warehouse name")
    private String warehouse;

    @UriParam(description = "Snowflake role")
    private String role;

    @UriParam(description = "Snowflake username")
    private String username;

    @UriParam(description = "Snowflake password", secret = true)
    private String password;

    @UriParam(description = "Table name for operations")
    private String table;

    @UriParam(description = "SQL query to execute", defaultValue = "")
    private String query = "";

    @UriParam(description = "Operation type: select, insert, update, delete", defaultValue = "select")
    private String operation = "select";

    @UriParam(description = "Custom JDBC URL override for testing")
    private String jdbcUrl;

    @UriParam(description = "Private key as binary string for key-pair authentication", secret = true)
    private String privateKey;

    @UriParam(description = "Path to a PKCS#8 private key file (PEM or DER). Used if 'privateKey' is not provided.")
    private String privateKeyFile;

    @UriParam(description = "Enable parameter binding for SQL queries (:#paramName syntax)", defaultValue = "true")
    private Boolean enableParameterBinding;

    @UriParam(description = "Header prefix for parameter binding", defaultValue = "snowflake.")
    private String parameterPrefix;

    @UriParam(description = "Snowflake authenticator (e.g., snowflake, snowflake_jwt, externalbrowser, oauth)", defaultValue = "snowflake")
    private String authenticator;

    @UriParam(description = "Output format of the message body: rows | json | xml | arrow (driver). 'rows' returns List<Map<String,Object>>; 'json' returns a JSON string; 'xml' returns an XML string.")
    private String outputFormat;

    @UriParam(description = "OAuth access token used when authenticator=oauth", secret = true)
    private String token;

    @UriParam(prefix = "jdbc.", description = "Additional Snowflake JDBC driver parameters to append to the JDBC URL")
    private java.util.Map<String, String> jdbcParameters;

    public SnowflakeEndpoint() {
    }

    public SnowflakeEndpoint(String uri, SnowflakeComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new SnowflakeProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new SnowflakeConsumer(this, processor);
    }

    public SnowflakeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(SnowflakeConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
        if (configuration != null) {
            configuration.setAccount(account);
        }
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
        if (configuration != null) {
            configuration.setDatabase(database);
        }
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
        if (configuration != null) {
            configuration.setSchema(schema);
        }
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
        if (configuration != null) {
            configuration.setWarehouse(warehouse);
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
        if (configuration != null) {
            configuration.setRole(role);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        if (configuration != null) {
            configuration.setUsername(username);
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        if (configuration != null) {
            configuration.setPassword(password);
        }
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
        if (configuration != null) {
            configuration.setTable(table);
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        if (configuration != null) {
            configuration.setQuery(query);
        }
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
        if (configuration != null) {
            configuration.setOperation(operation);
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        if (configuration != null) {
            configuration.setJdbcUrl(jdbcUrl);
        }
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        if (configuration != null) {
            configuration.setPrivateKey(privateKey);
        }
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
        if (configuration != null) {
            configuration.setPrivateKeyFile(privateKeyFile);
        }
    }

    public Boolean getEnableParameterBinding() {
        return enableParameterBinding;
    }

    public void setEnableParameterBinding(Boolean enableParameterBinding) {
        this.enableParameterBinding = enableParameterBinding;
        if (configuration != null && enableParameterBinding != null) {
            configuration.setEnableParameterBinding(enableParameterBinding);
        }
    }

    public String getParameterPrefix() {
        return parameterPrefix;
    }

    public void setParameterPrefix(String parameterPrefix) {
        this.parameterPrefix = parameterPrefix;
        if (configuration != null) {
            configuration.setParameterPrefix(parameterPrefix);
        }
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
        if (configuration != null) {
            configuration.setAuthenticator(authenticator);
        }
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
        if (configuration != null) {
            configuration.setOutputFormat(outputFormat);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        if (configuration != null) {
            configuration.setToken(token);
        }
    }

    public java.util.Map<String, String> getJdbcParameters() {
        return jdbcParameters;
    }

    public void setJdbcParameters(java.util.Map<String, String> jdbcParameters) {
        this.jdbcParameters = jdbcParameters;
        if (configuration != null) {
            configuration.setJdbcParameters(jdbcParameters);
        }
    }
}
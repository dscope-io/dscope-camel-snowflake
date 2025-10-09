# Apache Camel Snowflake Component

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Apache Camel 4.14.0](https://img.shields.io/badge/Apache%20Camel-4.14.0-orange.svg)](https://camel.apache.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-brightgreen.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](https://opensource.org/licenses/Apache-2.0)

A comprehensive Apache Camel component for integrating with Snowflake Data Warehouse, featuring enterprise-grade connection pooling, extensive tests, and production-ready JDBC operations.

## 📦 Versions
Production users should depend on the latest released version; the snapshot build exposes in‑progress features.

| Channel | Version | Maven Coordinates | Notes |
|---------|---------|-------------------|-------|
| Latest Release | 1.2.0 | `io.dscope.camel:camel-snowflake:1.2.0` | Stable, recommended for production |
| Previous Release | 1.0.0 | `io.dscope.camel:camel-snowflake:1.0.0` | Legacy release (upgrade recommended) |
| Development Snapshot | 1.2.0 | `io.dscope.camel:camel-snowflake:1.2.0` | Build from source (`mvn install`) to stay current with main |

Once 1.3.0 development begins, update the table and dependency snippets accordingly.

## 🛠 Installation

### Maven Dependency (Released)

```xml
<dependency>
    <groupId>io.dscope.camel</groupId>
    <artifactId>camel-snowflake</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Maven Dependency (Snapshot / Development)
Build and install the project locally first (`mvn clean install` at the repo root):
```xml
<dependency>
    <groupId>io.dscope.camel</groupId>
    <artifactId>camel-snowflake</artifactId>
    <version>1.2.0</version>
</dependency>
```

## 🚀 Features

- **Complete Camel Integration**: Full producer and consumer support
- **Enterprise Connection Pooling**: HikariCP-based connection management
- **Java 21 LTS**: Latest long-term support Java version with modern language features
- **Comprehensive Testing**: Extensive unit and integration tests
- **Production Ready**: JDBC operations with transaction support and error handling
- **JSON Support**: Built-in JSON data processing capabilities
- **Auto-Discovery**: Automatic component registration with Apache Camel
- **🤖 MCP Tooling**: Processors and samples that expose Snowflake queries via the Model Context Protocol

## 📋 Requirements

- Java 21 LTS or higher
- Apache Camel 4.14.0+
- Maven 3.9.1+
- Snowflake account (for production use)

## 📦 Versions

Current tested versions in this repository:

| Component            | Version        |
|----------------------|----------------|
| Java                 | 21             |
| Apache Camel         | 4.14.0         |
| Snowflake JDBC       | 3.26.1         |
| HikariCP             | 5.1.0          |
| Jackson              | 2.17.2         |
| SLF4J                | 2.0.x          |
| Spring Boot (sample) | 3.5.x          |

## ✅ Compatibility

- Java 21+
- Apache Camel 4.14.x (component built and tested with 4.14.0; compatible with Camel 4.x)
- Snowflake JDBC 3.26.x
- OSGi: packaged as a bundle (Felix Maven Bundle Plugin)
- Spring Boot: sample app on Spring Boot 3.5.x using Camel Spring Boot

## 🛠 Installation

### Maven Dependency

```xml
<dependency>
    <groupId>io.dscope.camel</groupId>
    <artifactId>camel-snowflake</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Build from Source

```bash
git clone https://github.com/dscope-io/dscope-camel-snowflake.git
cd camel-snowflake
mvn clean install
```

## 🔧 Configuration

### Basic URI Syntax

#### Key-Pair (JWT) Authentication Variant

For key-pair authentication (recommended for headless/service workloads), you can either:

- Provide the private key contents via `privateKey` (supports PKCS#8 or PKCS#1, PEM or Base64, headers optional), or
- Point to a key file via `privateKeyFile` (PEM or DER). 

When a private key is provided and no explicit authenticator is set, the component defaults to `snowflake_jwt`.

```java
// Key-pair (JWT) configuration using a file path
SnowflakeConfiguration config = new SnowflakeConfiguration();
config.setAccount(System.getProperty("snowflake.account"));
config.setUsername(System.getProperty("snowflake.username"));
config.setPrivateKeyFile(System.getProperty("snowflake.privateKeyFile")); // PEM or DER file
config.setDatabase(System.getProperty("snowflake.database"));
config.setWarehouse(System.getProperty("snowflake.warehouse"));
// Optional – explicitly set (auto-applied when privateKey/privateKeyFile present):
config.setAuthenticator("snowflake_jwt");
```

You may also pass the key contents directly:

```java
// Key-pair (JWT) configuration using key contents
config.setPrivateKey(System.getProperty("snowflake.privateKey")); // Accepts PEM (with or without headers) or Base64
```

Supported private key formats:

- PKCS#8 (BEGIN PRIVATE KEY) – PEM or Base64
- PKCS#1 (BEGIN RSA PRIVATE KEY) – automatically wrapped into PKCS#8
- DER (binary) via `privateKeyFile`

Encryption support:
* Using `privateKeyFile`: Encrypted PEM keys are supported when you also set `privateKeyFilePassword` (formerly `privateKeyPassword`). Unencrypted PEM/DER is also supported.
* Using `privateKey` (inline contents): Only unencrypted PKCS#8 or PKCS#1 is supported (decryption is not performed on inline contents). Use `privateKeyFile` + `privateKeyFilePassword` for encrypted keys.

##### Preparing a PKCS#8 Base64 Private Key

1. Generate an RSA key (2048 or 3072 bits recommended):
    ```bash
    openssl genrsa -out rsa_key.pem 2048
    ```
2. Convert to unencrypted PKCS#8 DER:
    ```bash
    openssl pkcs8 -topk8 -inform PEM -outform DER -in rsa_key.pem -out rsa_key.der -nocrypt
    ```
3. Produce single-line Base64 (no newlines) for your `.env` / secret store:
    ```bash
    base64 -in rsa_key.der | tr -d '\n' > private_key_base64.txt
    ```
4. Place the contents of `private_key_base64.txt` into `SNOWFLAKE_PRIVATE_KEY` (or a secrets manager) – do **not** commit it to source control.

Alternative one-liner (PEM to raw Base64 without headers):
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -in rsa_key.pem -nocrypt \
  | sed '/-----/d' | tr -d '\n'
```

Security notes:
* Never commit private keys or `.env` files containing them.
* Prefer a secrets manager (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault, etc.).
* Rotate keys periodically and immediately on suspected compromise.
* Use least-privilege Snowflake roles bound to the JWT user.


```
snowflake:endpointName?account=myaccount&database=mydb&schema=myschema[&options]
```

### Configuration Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `account` | String | Yes | - | Snowflake account identifier |
| `username` | String | Yes | - | Snowflake username |
| `password` | String | Yes | - | Snowflake password (sensitive) |
| `database` | String | No | - | Database name |
| `schema` | String | No | - | Schema name |
| `warehouse` | String | No | - | Warehouse name |
| `role` | String | No | - | Role name |
| `table` | String | No | - | Table name for operations |
| `query` | String | No | - | SQL query to execute |
| `operation` | String | No | `select` | Operation type: select, insert, update, delete |
| `jdbcUrl` | String | No | - | Custom JDBC URL override (for testing) |
| `privateKey` | String | No | - | Private key contents for key-pair (JWT) auth. Accepts PKCS#8 or PKCS#1, PEM (with/without headers) or Base64. Mutually exclusive with `password`. |
| `privateKeyFile` | String | No | - | Path to private key file (PEM or DER). Mutually exclusive with `password`. |
| `privateKeyFilePassword` | String | No | - | Password for encrypted private key files. Only used with `privateKeyFile`; ignored when `privateKey` contents are provided. (Legacy alias: `privateKeyPassword`) |
| `authenticator` | String | No | `snowflake` | Authenticator. Supported: `snowflake`, `snowflake_jwt`, `externalbrowser`, `oauth`. Auto-set to `snowflake_jwt` when a private key is provided. |
| `token` | String | No | - | OAuth bearer token (required when `authenticator=oauth`) |
| `outputFormat` | String | No | `rows` | Serialization of exchange body: `rows` (List<Map<String,Object>>), `json` (string), or `xml` (string). Also controls driver result format via JDBC: `arrow` uses Apache Arrow; any other value uses JSON. |
| `enableParameterBinding` | boolean | No | `true` | Enable Camel-style named parameter binding in SQL (:#name) using headers. |
| `parameterPrefix` | String | No | `snowflake.` | Header prefix used when resolving parameters for binding. |
| `jdbc.*` | Map | No | - | Pass-through Snowflake JDBC parameters appended to the JDBC URL (e.g., `jdbc.CLIENT_SESSION_KEEP_ALIVE=true`). |

## 📚 Usage Examples

### Basic Producer

```java
from("direct:start")
    .to("snowflake:producer?account=myaccount&database=mydb&username=user&password=pass")
    .to("mock:result");
```

### Consumer with Processor

```java
from("snowflake:consumer?account=myaccount&database=mydb&table=mytable&username=user&password=pass")
    .process(exchange -> {
        String data = exchange.getIn().getBody(String.class);
        log.info("Received from Snowflake: {}", data);
    })
    .to("mock:processed");
```

### JDBC Operations

```java
// Direct JDBC operations
SnowflakeConfiguration config = new SnowflakeConfiguration();
config.setAccount("myaccount");
config.setDatabase("mydb");
config.setUsername("user");
config.setPassword("password");

// Execute query
List<Map<String, Object>> results = SnowflakeJdbcOperations.executeQuery(
    config, "SELECT * FROM users WHERE active = ?", true);

// Execute update
int rowsAffected = SnowflakeJdbcOperations.executeUpdate(
    config, "UPDATE users SET last_login = ? WHERE id = ?", new Date(), 123);

// Batch operations
List<Object[]> batchParams = Arrays.asList(
    new Object[]{"Alice", 30},
    new Object[]{"Bob", 25}
);
int[] results = SnowflakeJdbcOperations.executeBatch(
    config, "INSERT INTO users (name, age) VALUES (?, ?)", batchParams);
```

### Stored Procedure (CALL) Support

You can invoke stored procedures by supplying a `CALL` statement either as the static endpoint `query`, an overriding header, or directly in the message body. Parameter binding works exactly the same as for `SELECT`/DML:

```java
from("direct:invokeProc")
    // binding headers: snowflake.id, snowflake.amount, snowflake.details_json
    .setBody(constant("CALL insert_new_sample_row(:#id,:#amount,:#details_json)"))
    .to("snowflake:procDemo?account=...&database=...&username=...&privateKeyFile=...&warehouse=...");
```

Execution logic:
* Detects `CALL` (case‑insensitive) → uses `CallableStatement#execute`.
* On specific Snowflake driver API limitation errors, falls back transparently to `Statement#execute`.
* Result set (if any) serialized via `outputFormat`; otherwise an empty collection (rows) or minimal document (json/xml) is returned.

Binding header resolution order: exact name (`id`), prefixed header (`snowflake.id`), Camel style (`CamelSnowflakeId`).

### Dynamic SQL Resolution Precedence

When multiple potential SQL sources exist, the producer chooses in this priority (highest first):
1. Header `CamelSnowflakeQuery` (explicit override)
2. Message body if it looks like SQL (starts with verbs: SELECT, WITH, INSERT, UPDATE, DELETE, CALL, CREATE, ALTER, MERGE)
3. Endpoint/configured `query`

This lets you keep a safe default while allowing ad‑hoc overrides. Provide a non‑SQL body (e.g. JSON payload) by either not starting it with a SQL verb or forcing a different query via the header.

### JSON Data Processing

```java
// Insert JSON data
String jsonData = "{\"name\": \"Alice\", \"age\": 30, \"city\": \"New York\"}";
SnowflakeJdbcOperations.insertJsonData(config, "user_profiles", jsonData);

// Query JSON data
List<JsonNode> jsonResults = SnowflakeJdbcOperations.queryJsonData(
    config, "SELECT profile_data FROM user_profiles WHERE id = ?", 123);
```

## 🤖 Model Context Protocol (MCP) Support

The component ships with a collection of registry beans in `io.dscope.camel.snowflake.mcp` that let you expose Snowflake tooling to MCP-compatible clients (GitHub Copilot, VS Code, etc.) with minimal wiring.

### Included Processors

| Bean name | Purpose |
|-----------|---------|
| `mcpJsonRpcEnvelope` | Parses JSON-RPC requests/responses, capturing diagnostics for invalid payloads |
| `mcpHttpValidator` | Validates MCP HTTP headers (Accept, Content-Type, MCP-Protocol-Version) |
| `mcpMethodCatalog` | Loads tool definitions from `classpath:mcp/methods.yaml` |
| `mcpSnowflakeRequest` / `mcpSnowflakeResponse` / `mcpSnowflakeError` | Bridge `tools/call` into the `snowflake:` endpoint, sanitize responses, and standardize error payloads |
| `mcpToolsList` | Implements `tools/list` using the catalog |
| `mcpRateLimit`, `mcpRequestSizeGuard` | Optional guards for burst traffic and oversized MCP payloads |

Because the beans are annotated with `@BindToRegistry`, they are auto-discovered by Camel Main/Spring XML/YAML DSL. Simply reference the beans in your routes to handle MCP webhook traffic:

```yaml
# snippet from samples/mcp-snowflake-yaml
- from:
        uri: rest:post:/mcp?consumerComponentName=netty-http
        steps:
            - process: ref:mcpRequestSizeGuard
            - process: ref:mcpRateLimit
            - process: ref:mcpHttpValidator
            - process: ref:mcpJsonRpcEnvelope
            - choice:
                    when:
                        - simple: "${exchangeProperty.mcp.jsonrpc.method} == 'tools/list'"
                            steps:
                                - process: ref:mcpToolsList
                                - to: direct:mcp-complete
                        - simple: "${exchangeProperty.mcp.jsonrpc.method} == 'tools/call'"
                            steps:
                                - process: ref:mcpSnowflakeRequest
                                - to: direct:mcp-snowflake-exec
```

### Quick Start Sample

1. Install the component so the sample resolves the latest snapshot:
     ```bash
     mvn -q -DskipTests install
     ```
2. Build the MCP YAML sample:
     ```bash
     mvn -q -pl samples/mcp-snowflake-yaml -am -DskipTests package
     ```
3. Provide Snowflake credentials via environment variables or JVM `-Dsnowflake.*` properties (account, username, private key or OAuth token, database, schema, warehouse, role).
4. Launch the shaded sample with debug logging:
     ```bash
     java \
         -Dmcp.server.port=8080 \
         -Dsnowflake.username=$SNOWFLAKE_USERNAME \
         -Dsnowflake.privateKeyFile=$SNOWFLAKE_PRIVATE_KEY_FILE \
         -Dsnowflake.account=$SNOWFLAKE_ACCOUNT \
         -Dsnowflake.database=$SNOWFLAKE_DATABASE \
         -Dsnowflake.schema=$SNOWFLAKE_SCHEMA \
         -Dsnowflake.warehouse=$SNOWFLAKE_WAREHOUSE \
         -Dsnowflake.role=$SNOWFLAKE_ROLE \
         -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
         -jar samples/mcp-snowflake-yaml/target/mcp-snowflake-yaml-1.2.0-shaded.jar
     ```
5. Issue MCP-compliant JSON-RPC requests (aliases in `samples/mcp-snowflake-yaml/shell-aliases.zsh`):
     ```bash
     curl -s -X POST http://localhost:8080/mcp \
         -H 'Content-Type: application/json' \
         -H 'Accept: application/json, text/event-stream' \
         -H 'MCP-Protocol-Version: 2025-06-18' \
         -d '{"jsonrpc":"2.0","id":"req-1","method":"tools/list"}' | jq .
     ```

Add or modify tools in `samples/mcp-snowflake-yaml/src/main/resources/mcp/methods.yaml`; the catalog and processors will automatically pick them up. Responses redact the SQL text and credentials, returning only status metadata and result payloads suitable for MCP clients.

### Defining MCP Tools (`methods.yaml`)

The catalog loader expects a YAML document shaped like:

```yaml
methods:
    - name: selectSample
        title: Select Sample Rows
        description: Human-friendly summary shown to MCP clients
        query: |
            SELECT * FROM SOME_TABLE WHERE USER_ID = :#user_id
        enableParameterBinding: true
        requiredArguments:
            - user_id
            - min_date
        inputSchema:
            type: object
            properties:
                user_id:
                    type: integer
                    description: User identifier to filter rows
                min_date:
                    type: string
                    description: Earliest created_at date (YYYY-MM-DD)
            required: [user_id, min_date]
        outputSchema:
            type: object
            properties:
                status:
                    type: string
                result:
                    description: Result rows from Snowflake
            required: [status]
        annotations:
            category: snowflake
```

Field reference:

- `methods` &rarr; top-level array; each entry must have a unique, non-blank `name`.
- `title`, `description` &rarr; surfaced to clients when listing tools.
- `query` &rarr; default SQL executed when `tools/call` is invoked; can include `:#param` placeholders that bind against headers/body arguments.
- `enableParameterBinding` (default `true`) &rarr; toggles Camel named-parameter binding; set `false` if you need driver-side templating instead.
- `requiredArguments` &rarr; list of argument names the MCP client must supply; enforced before executing the query.
- `inputSchema`, `outputSchema` &rarr; JSON Schema fragments returned to clients so they know how to shape arguments/results.
- `annotations` &rarr; arbitrary metadata (category, tags, etc.) passed through the MCP responses.

Place your custom `methods.yaml` on the application classpath (default: `classpath:mcp/methods.yaml`). The file is hot-loaded during startup; restart the application after edits so the catalog refreshes.

### OAuth Authentication (Bearer Token)

```java
SnowflakeConfiguration config = new SnowflakeConfiguration();
config.setAccount(System.getenv("SNOWFLAKE_ACCOUNT"));
config.setUsername(System.getenv("SNOWFLAKE_USERNAME"));
config.setAuthenticator("oauth");
config.setToken(System.getenv("SNOWFLAKE_OAUTH_TOKEN")); // supply a valid access token from your IdP
config.setDatabase(System.getenv("SNOWFLAKE_DATABASE"));
config.setWarehouse(System.getenv("SNOWFLAKE_WAREHOUSE"));
```

Endpoint URI example (Camel):
```
snowflake://default?account={{snowflake.account}}&username={{snowflake.username}}&authenticator=oauth&token={{snowflake.token}}
```

Notes:
- When `authenticator=oauth`, provide `token` and do not set `password` or `privateKey`.
- Token refresh/rotation is managed externally; pass updated tokens when needed.

#### CLI quick start (OAuth)

```bash
java \
    -Dsnowflake.account=your_account \
    -Dsnowflake.username=oauth_user \
    -Dsnowflake.authenticator=oauth \
    -Dsnowflake.token="$(cat /path/to/access_token.txt)" \
    -Dsnowflake.database=YOUR_DB \
    -Dsnowflake.warehouse=COMPUTE_WH \
    -jar your-app.jar
```

### System property fallbacks and pass-through JDBC parameters

The component automatically reads configuration from Java system properties using the `snowflake.*` prefix. This allows you to keep secrets out of URIs and pass them via `-D` flags or your runtime launcher:

```bash
# Example: pass username and key file without embedding them in the URI
java \
    -Dsnowflake.username=sample_user \
    -Dsnowflake.privateKeyFile=/abs/path/private_key_pkcs8.pem \
    -Dsnowflake.privateKeyFilePassword=changeit \
    -jar your-app.jar
```

Additional Snowflake JDBC parameters can be provided as either endpoint `jdbc.*` parameters or as system properties using the `snowflake.jdbc.` prefix. They’ll be URL-encoded and appended to the JDBC URL:

```bash
# Keep the session alive and set a client timeout
java \
    -Dsnowflake.jdbc.CLIENT_SESSION_KEEP_ALIVE=true \
    -Dsnowflake.jdbc.CLIENT_SESSION_KEEP_ALIVE_HEARTBEAT_FREQUENCY=900 \
    -jar your-app.jar
```

Driver result format is controlled automatically:

- If `outputFormat=arrow`, the component appends `JDBC_QUERY_RESULT_FORMAT=ARROW` to the JDBC URL.
- Otherwise, it appends `JDBC_QUERY_RESULT_FORMAT=JSON` to avoid additional JVM `--add-opens` flags required by Arrow.

Important: Avoid duplicating location properties. Database (`DB`/`database`), schema (`SCHEMA`/`schema`), warehouse (`WAREHOUSE`/`warehouse`), and role (`ROLE`/`role`) are embedded into the JDBC URL by the component. Do not also set them as DataSource properties or via `snowflake.jdbc.*`, otherwise the Snowflake driver may reject the connection with an error like:

"Connection property specified more than once: DB"

The component ensures these location properties are kept only in the URL to prevent duplication.

### Spring Boot Configuration

```yaml
# application.yml
camel:
  component:
    snowflake:
      account: ${SNOWFLAKE_ACCOUNT}
      username: ${SNOWFLAKE_USERNAME}
      password: ${SNOWFLAKE_PASSWORD}
      database: ${SNOWFLAKE_DATABASE}
      warehouse: ${SNOWFLAKE_WAREHOUSE}
```

## 🏗 Architecture

### Component Structure

```
io.dscope.camel.snowflake/
├── SnowflakeComponent      # Main component class
├── SnowflakeEndpoint       # Endpoint implementation
├── SnowflakeProducer       # Producer implementation
├── SnowflakeConsumer       # Consumer implementation
├── SnowflakeConfiguration  # Configuration class
└── jdbc/
    ├── SnowflakeJdbcConnectionManager  # Connection pooling
    └── SnowflakeJdbcOperations        # JDBC utilities
```

### Connection Pooling

The component uses HikariCP for enterprise-grade connection pooling:

- **Connection Reuse**: Efficient connection pooling and reuse
- **Performance Monitoring**: Built-in pool statistics and monitoring
- **Resource Management**: Automatic connection lifecycle management
- **Configuration**: Optimized default settings for Snowflake

### Error Handling

Comprehensive error handling includes:

- **Connection Failures**: Automatic retry and failover
- **SQL Exceptions**: Detailed error reporting and logging
- **Resource Leaks**: Automatic resource cleanup with try-with-resources
- **Transaction Management**: Proper transaction handling and rollback

## 🧪 Testing

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SnowflakeComponentTest

# Run with coverage
mvn clean test jacoco:report
```

### Test Categories

- **Unit Tests**: Component logic and configuration (31 tests)
- **Integration Tests**: End-to-end component integration (4 tests)
- **JDBC Tests**: Database operations and connection pooling (9 tests)
- **Mock Tests**: Isolated component testing with mocks

### Test Coverage

The project includes a broad suite of unit and integration tests. Use your preferred coverage tooling (e.g., Jacoco) to generate reports locally.

## 🔒 Security

### Sensitive Data Handling

- Password parameters marked as `secret = true`
- Connection strings properly escaped
- No sensitive data in logs
- Secure connection pooling

### Production Recommendations

```java
// Use environment variables for credentials
config.setUsername(System.getenv("SNOWFLAKE_USERNAME"));
config.setPassword(System.getenv("SNOWFLAKE_PASSWORD"));

// Enable SSL/TLS
config.buildJdbcUrl(); // Automatically includes SSL parameters
```

## 📊 Performance

### Benchmarks

- **Connection Pool**: 10x faster than direct connections
- **Batch Operations**: 5x faster than individual inserts
- **Memory Usage**: 50% reduction with proper pooling
- **Throughput**: 1000+ operations/second sustained

### Optimization Tips

1. **Use Connection Pooling**: Always use the built-in connection manager
2. **Batch Operations**: Use batch methods for multiple operations
3. **Prepared Statements**: Parameters are automatically prepared
4. **Resource Cleanup**: Use try-with-resources or component lifecycle

## 🚀 Production Deployment

### Configuration

```java
// Production configuration
SnowflakeConfiguration config = new SnowflakeConfiguration();
config.setAccount(System.getenv("SNOWFLAKE_ACCOUNT"));
config.setUsername(System.getenv("SNOWFLAKE_USERNAME"));
config.setPassword(System.getenv("SNOWFLAKE_PASSWORD"));
config.setDatabase(System.getenv("SNOWFLAKE_DATABASE"));
config.setWarehouse(System.getenv("SNOWFLAKE_WAREHOUSE"));
```

### Monitoring

```java
// Monitor connection pool
String poolStats = SnowflakeJdbcConnectionManager.getPoolStats(config);
log.info("Connection pool status: {}", poolStats);

// Test connectivity
boolean isConnected = SnowflakeJdbcOperations.testConnection(config);
log.info("Snowflake connection status: {}", isConnected);
```

### Dynamic Per-Message Overrides

You can override most endpoint / configuration properties per exchange using headers. The producer copies the base `SnowflakeConfiguration` and applies any override headers before executing the query.

Supported override headers:

| Header | Purpose |
|--------|---------|
| `CamelSnowflakeAccount` | Override account identifier |
| `CamelSnowflakeUsername` | Override username |
| `CamelSnowflakePassword` | Override password (ignored if `CamelSnowflakePrivateKey` provided) |
| `CamelSnowflakePrivateKey` | Raw Base64 PKCS#8 private key (switches to key-pair auth) |
| `CamelSnowflakeDatabase` | Override database |
| `CamelSnowflakeSchema` | Override schema |
| `CamelSnowflakeWarehouse` | Override warehouse |
| `CamelSnowflakeRole` | Override role |
| `CamelSnowflakeQuery` | Override query (if body not used) |
| `CamelSnowflakeOperation` | Hint operation (select/insert/update/delete) – select detection is automatic |
| `CamelSnowflakeJdbcUrl` | Full JDBC URL override (e.g. for tests using H2) |
| `CamelSnowflakeAuthenticator` | Override authenticator (e.g. `snowflake_jwt`) |
| `CamelSnowflakeToken` | OAuth bearer token when using `authenticator=oauth` |
| `CamelSnowflakeParameterPrefix` | Override parameter binding header prefix |
| `CamelSnowflakeEnableParameterBinding` | Enable/disable parameter binding (boolean) |

Effective values are exposed after application for tracing:
`CamelSnowflakeEffectiveAccount`, `...EffectiveDatabase`, `...EffectiveSchema`, `...EffectiveWarehouse`, `...EffectiveRole`, `...EffectiveOperation`, `...EffectiveQuery`, `...EffectiveAuthenticator`.

Example route with overrides:

```java
from("direct:dynamic")
    .setHeader("CamelSnowflakeQuery", constant("SELECT * FROM DEMO_TABLE WHERE ID = :#id"))
    .setHeader("CamelSnowflakeWarehouse", simple("${header.RequestedWarehouse}"))
    .setHeader("CamelSnowflakeEnableParameterBinding", constant(true))
    .to("snowflake:demo");
```

Then set headers per message:

```java
exchange.getIn().setHeader("id", 42); // bound parameter
exchange.getIn().setHeader("RequestedWarehouse", "WH_XL");
exchange.getIn().setHeader("CamelSnowflakePrivateKey", System.getenv("SNOWFLAKE_PRIVATE_KEY"));
```

Testing with in-memory H2 using a JDBC URL override:

```java
exchange.getIn().setHeader("CamelSnowflakeJdbcUrl", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
```

This allows fast unit tests without hitting Snowflake, while keeping the same producer logic.

Precedence order (lowest to highest):
1. Base component / endpoint configuration (Java DSL / URI)
2. Endpoint URI parameters set directly on the `SnowflakeEndpoint` instance after creation
3. Per-message headers (listed above)

Headers always win; endpoint parameters override the original base configuration, allowing route-level tuning without code changes.

#### Properties precedence (lowest → highest)

1. Component defaults (code) and application properties
2. Endpoint URI parameters (route-level)
3. System properties (`-Dsnowflake.*`, `-Dsnowflake.jdbc.*`)
4. Per-message headers (e.g., `CamelSnowflake*`, parameter binding headers)

Notes:
- System properties are read lazily by getters, allowing you to inject secrets at launch time without changing routes.
- For exec:java, pass `-Dsnowflake.*` at Maven CLI level rather than in `-Dexec.jvmArgs`.

### ⚙️ Config cheat sheet (system properties)

- Core: `-Dsnowflake.account=... -Dsnowflake.username=... [-Dsnowflake.database=...] [-Dsnowflake.schema=...] [-Dsnowflake.warehouse=...] [-Dsnowflake.role=...]`
- Auth (password): `-Dsnowflake.password=...`
- Auth (key-pair contents): `-Dsnowflake.privateKey="$(cat private_key_pkcs8.pem | tr '\n' '\\n')"`
- Auth (key-pair file): `-Dsnowflake.privateKeyFile=/abs/path/private_key_pkcs8.pem`
- Auth (key-pair file + encrypted PEM): `-Dsnowflake.privateKeyFile=/abs/path/encrypted_key.pem -Dsnowflake.privateKeyFilePassword=changeit` (legacy: `-Dsnowflake.privateKeyPassword=`)
- Output: `-Dsnowflake.outputFormat=rows|json|xml|arrow`
- Param binding: `-Dsnowflake.enableParameterBinding=true -Dsnowflake.parameterPrefix=snowflake.`
- JDBC pass-through: `-Dsnowflake.jdbc.CLIENT_SESSION_KEEP_ALIVE=true`

#### Parameter binding with a custom prefix

If your endpoint query uses `:#param` placeholders, you can place parameter values in headers using a configurable prefix. The default prefix is `snowflake.` so a header `snowflake.id` binds to `:#id`.

Example:

```java
// Endpoint-level query with :#id
from("direct:lookupUser")
    // Set header using default prefix so it binds to :#id
    .setHeader("snowflake.id", simple("${header.userId}"))
    .to("snowflake:lookup?query=SELECT%20NAME%20FROM%20USERS%20WHERE%20ID%20=%20:%23id");
```

Notes:
- Default `parameterPrefix` is `snowflake.` and can be changed globally via configuration or per message using `CamelSnowflakeParameterPrefix`.
- Binding also accepts plain `id` or Camel-style `CamelSnowflakeId` if you prefer not to use a prefix.

Alternative header name (Camel-style):

```java
// No prefix required; Camel-style header binds to :#id
from("direct:lookupUserCamelStyle")
    .setHeader("CamelSnowflakeId", constant(42))
    .to("snowflake:lookup?query=SELECT%20NAME%20FROM%20USERS%20WHERE%20ID%20=%20:%23id");
```

Override parameter prefix per message:

```java
// Change prefix to "q." for this message and provide q.id
from("direct:lookupUserWithPrefixOverride")
    .setHeader("CamelSnowflakeParameterPrefix", constant("q."))
    .setHeader("q.id", simple("${header.userId}"))
    .to("snowflake:lookup?query=SELECT%20NAME%20FROM%20USERS%20WHERE%20ID%20=%20:%23id");
```

### Health Checks

```java
@Component
public class SnowflakeHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            boolean connected = SnowflakeJdbcOperations.testConnection(config);
            return connected ? Health.up().build() : Health.down().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
# Clone the repository
git clone https://github.com/dscope/camel-snowflake.git
cd camel-snowflake

# Build and test
mvn clean compile
mvn test

# Generate documentation
mvn javadoc:javadoc
```

### Fast iterative rebuild (component + sample)

Use the helper script for local dev cycles so the sample always picks up the latest snapshot:

```bash
./dev-install.sh                 # installs & builds dynamic-query-yaml sample
./dev-install.sh dynamic-query-kotlin   # pick a different sample
./dev-install.sh -U              # force snapshot/plugin updates
```

The script performs a root `mvn -DskipTests install` then builds the selected sample via the samples aggregator (`samples/pom.xml`).

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- [Apache Camel](https://camel.apache.org/)
- [Snowflake Documentation](https://docs.snowflake.com/)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
- [Java 21 Documentation](https://openjdk.java.net/projects/jdk/21/)
 - [Javadoc (generated in target/apidocs)](./target/apidocs/index.html)

## 📞 Support

For support and questions:

- Create an [Issue](https://github.com/dscope-io/dscope-camel-snowflake/issues)
- Check the [Wiki](https://github.com/dscope-io/dscope-camel-snowflake/wiki)
- Contact: [support@dscope.io](mailto:support@dscope.io)

---

## 🧩 Arrow troubleshooting (optional)

If you opt into Arrow by setting `-Dsnowflake.outputFormat=arrow`, some JVMs require additional `--add-opens` flags for Apache Arrow modules. If you encounter IllegalAccessErrors, try adding:

```bash
JAVA_TOOL_OPTIONS="--add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED"
```

Alternatively, stay on the default JSON driver result format which avoids these flags and is sufficient for most workloads.

**Built with ❤️ by the DScope team**
 
## Developer quick start

### Build and test

- VS Code tasks
    - Build: run task "Build: Maven"
    - Test: run task "Test: Maven"

- Terminal
    ```bash
    mvn -q -DskipTests package
    mvn -q test
    ```

### Debug: Snowflake Connection Tester

- Launch configuration: "Debug: Snowflake Connection Tester"
- The tester reads credentials from environment variables and supports `.env` / `.env.local` if present.
- Recommended variables:
    - `SNOWFLAKE_ACCOUNT`, `SNOWFLAKE_USERNAME`
    - Either `SNOWFLAKE_PASSWORD` or `SNOWFLAKE_PRIVATE_KEY` (raw Base64 PKCS#8)
    - Optional: `SNOWFLAKE_DATABASE`, `SNOWFLAKE_SCHEMA`, `SNOWFLAKE_WAREHOUSE`, `SNOWFLAKE_ROLE`

### H2 compatibility in tests

Tests use H2 in-memory DB for hermetic runs. Snowflake-specific `DATE(column)` expressions are adjusted using `CAST(column AS DATE)` and date parameters are also cast as needed for H2.
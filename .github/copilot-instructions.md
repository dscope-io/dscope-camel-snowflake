# Apache Camel Snowflake Component - AI Assistant Instructions

This workspace contains a production-ready Apache Camel component for Snowflake integration with enterprise features and an MCP extension module.

## 🏗️ Architecture Overview

**Module Structure**:
- `camel-snowflake/` → Core Snowflake Camel component artifact (`io.dscope.camel:camel-snowflake`)
- `camel-snowflake-mcp/` → Snowflake MCP integration artifact (`io.dscope.camel:camel-snowflake-mcp`)
- `samples/` → Standalone sample applications that exercise one or both modules

**Core Component Structure** (package: `io.dscope.camel.snowflake`):
- `SnowflakeComponent` → Main Camel component registration point
- `SnowflakeEndpoint` → URI configuration and lifecycle management
- `SnowflakeProducer/Consumer` → Message processing logic
- `SnowflakeConfiguration` → Centralized config with authentication modes
- `jdbc/` → HikariCP connection pooling + JDBC operations utilities
- `sql/` → SQL templating and parameter binding

**MCP Extension Structure** (package: `io.dscope.camel.snowflake.mcp`):
- `McpSnowflakeRequestProcessor` → Translates MCP tool calls into Snowflake headers/query execution
- `McpSnowflakeResponseProcessor` → Normalizes Snowflake execution results into MCP JSON-RPC responses
- `McpSnowflakeErrorProcessor` → Formats Snowflake/MCP execution failures for clients
- Compatibility wrapper processors remain in this module when route wiring benefits from Snowflake-specific names

**Multi-Sample Architecture** (`samples/`):
- Each sample is a standalone Maven project with its own dependencies
- `samples/shared/` contains common SQL scripts and helper utilities
- MCP samples (`mcp-snowflake-yaml`) expose Snowflake as MCP tools for AI clients
- Non-MCP samples use `io.dscope.camel:camel-snowflake`; MCP samples depend on `io.dscope.camel:camel-snowflake-mcp` and `io.dscope.camel:camel-mcp:1.4.1`

## 🛠️ Developer Workflows

### Essential Build Commands
```bash
# Fast iterative development (component + sample)
./dev-install.sh                          # Builds component + dynamic-query-yaml sample
./dev-install.sh dynamic-query-kotlin     # Pick different sample module
./dev-install.sh -U                       # Force snapshot updates

# Standard Maven workflows
mvn -q -DskipTests package               # Quick build (Task: "Build: Maven")
mvn -q test                              # Full test suite (Task: "Test: Maven")
mvn -q -f samples/pom.xml package        # Build all samples
```

### Authentication Patterns
**Key-pair (JWT) - Production recommended**:
- `privateKey`: Raw PKCS8/PKCS1 content (Base64 or PEM)
- `privateKeyFile`: Path to PEM/DER file + optional `privateKeyFilePassword`
- Auto-applies `authenticator=snowflake_jwt` when key detected

**OAuth Bearer Token**:
- `authenticator=oauth` + `token` parameter
- External token refresh management required

**System Properties Override**: Use `-Dsnowflake.*` to inject secrets without embedding in URIs

### Testing Strategy
- **H2 In-Memory**: Unit tests use H2 with Snowflake SQL compatibility adjustments
- **Integration Tests**: Marked `@Disabled` by default, enable with real Snowflake credentials
- **Test Coverage**: 40+ tests across component, JDBC, and MCP processors
- **CamelTestSupport**: Standard pattern for route testing with mock endpoints

## 🔧 Component-Specific Patterns

### Dynamic Configuration Overrides
Header-based per-message configuration precedence (highest wins):
1. `CamelSnowflake*` headers (per-exchange overrides)
2. System properties (`-Dsnowflake.*`)
3. Endpoint URI parameters
4. Component defaults

### Parameter Binding (named parameter syntax)
```java
// SQL with named parameters
.setBody("SELECT * FROM users WHERE id = :userId AND status = :status")
.setHeader("snowflake.userId", 123)        // Default prefix
.setHeader("snowflake.status", "active")
.to("snowflake:query")
```

### SQL Resolution Precedence
1. Header `CamelSnowflakeQuery` (explicit override)
2. Message body (if starts with SQL keywords: SELECT, INSERT, CALL, etc.)
3. Endpoint `query` parameter

### Output Format Control
- `outputFormat=rows` → `List<Map<String,Object>>` (default)
- `outputFormat=json` → JSON string
- `outputFormat=arrow` → Apache Arrow (requires `--add-opens` JVM flags)

## 🤖 MCP Integration Specifics

**Registry Beans** (`camel-snowflake-mcp`, package `io.dscope.camel.snowflake.mcp`):
- `mcpSnowflakeRequest` → Bridges MCP `tools/call` to Snowflake endpoints
- `mcpSnowflakeResponse` → Normalizes results to MCP JSON-RPC format
- Combines with `camel-mcp` catalog processors for full MCP protocol support

**Tool Definition** (`src/main/resources/mcp/methods.yaml`):
- YAML-based tool catalog with JSON Schema validation
- SQL templates injected via `bean:` URI from `application.properties`
- Hot-reloaded on application startup

## 📁 Key Files for Code Understanding

- `camel-snowflake/src/main/java/io/dscope/camel/snowflake/SnowflakeProducer.java` → Core message processing and override logic
- `camel-snowflake/src/main/java/io/dscope/camel/snowflake/jdbc/SnowflakeJdbcOperations.java` → Low-level database operations patterns
- `camel-snowflake-mcp/src/main/java/io/dscope/camel/snowflake/mcp/McpSnowflakeRequestProcessor.java` → Snowflake-specific MCP request translation
- `samples/dynamic-query-yaml/src/main/resources/routes/snowflake-route.yaml` → YAML DSL route examples
- `samples/mcp-snowflake-yaml/` → Complete MCP server implementation
- `camel-snowflake/src/test/java/io/dscope/camel/snowflake/` → Core component testing patterns and mock setup
- `camel-snowflake-mcp/src/test/java/io/dscope/camel/snowflake/mcp/` → MCP integration tests

## 🚀 Development Conventions

- **OSGi Bundle**: Uses Felix Maven Bundle Plugin with explicit export packages
- **Java 21 LTS**: Leverages modern language features (pattern matching, switch expressions)
- **Sensitive Data**: Mark password/key parameters with `secret = true` in configuration
- **Connection Pooling**: Always use `SnowflakeJdbcConnectionManager` for production code
- **Error Handling**: Comprehensive SQLException handling with resource cleanup
- **Version Strategy**: Snapshot development with release profile for Maven Central

## 💡 Testing & Debugging Tips

- **Quick Sample Test**: Run `samples/dynamic-query-yaml` with `-Dsnowflake.*` system properties
- **H2 JDBC Override**: Use `CamelSnowflakeJdbcUrl=jdbc:h2:mem:test` header for unit testing
- **MCP Tool Testing**: Use `curl` with JSON-RPC payloads (see `shell-aliases.zsh`)
- **Connection Debug**: Check HikariCP pool stats via `SnowflakeJdbcConnectionManager.getPoolStats()`
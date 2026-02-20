# Apache Camel Snowflake Component - AI Assistant Instructions

This workspace contains a production-ready Apache Camel component for Snowflake integration with enterprise features and MCP tooling support.

## 🏗️ Architecture Overview

**Core Component Structure** (package: `io.dscope.camel.snowflake`):
- `SnowflakeComponent` → Main Camel component registration point
- `SnowflakeEndpoint` → URI configuration and lifecycle management
- `SnowflakeProducer/Consumer` → Message processing logic
- `SnowflakeConfiguration` → Centralized config with authentication modes
- `jdbc/` → HikariCP connection pooling + JDBC operations utilities
- `mcp/` → Model Context Protocol processors for AI integration
- `sql/` → SQL templating and parameter binding

**Multi-Sample Architecture** (`samples/`):
- Each sample is a standalone Maven project with its own dependencies
- `samples/shared/` contains common SQL scripts and helper utilities
- MCP samples (`mcp-snowflake-yaml`) expose Snowflake as MCP tools for AI clients
- All samples use the shaded component dependency (`io.dscope.camel:camel-snowflake:1.3.0`)

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

**Registry Beans** (`io.dscope.camel.snowflake.mcp`):
- `mcpSnowflakeRequest` → Bridges MCP `tools/call` to Snowflake endpoints
- `mcpSnowflakeResponse` → Normalizes results to MCP JSON-RPC format
- Combines with `camel-mcp` catalog processors for full MCP protocol support

**Tool Definition** (`src/main/resources/mcp/methods.yaml`):
- YAML-based tool catalog with JSON Schema validation
- SQL templates injected via `bean:` URI from `application.properties`
- Hot-reloaded on application startup

## 📁 Key Files for Code Understanding

- `SnowflakeProducer.java` → Core message processing and override logic
- `SnowflakeJdbcOperations.java` → Low-level database operations patterns
- `samples/dynamic-query-yaml/src/main/resources/routes/snowflake-route.yaml` → YAML DSL route examples
- `samples/mcp-snowflake-yaml/` → Complete MCP server implementation
- `src/test/java/io/dscope/camel/snowflake/` → Testing patterns and mock setup

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
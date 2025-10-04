# Changelog

All notable changes to the Camel Snowflake Component will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-SNAPSHOT] - 2025-09-25

### Added
- Initial release of Apache Camel Snowflake component
- Snowflake database connectivity with JDBC driver integration
- Support for key-pair authentication with private key
- Password-based authentication support
- SQL parameter binding with `:#paramName` syntax
- Header-based parameter resolution with configurable prefix
- Connection pooling with HikariCP
- Comprehensive configuration options:
  - Account, database, schema, warehouse, role
  - Custom JDBC URL override for testing
  - Parameter binding enable/disable
  - Parameter prefix customization
- OSGi bundle packaging for enterprise deployment
- Standalone JAR with all dependencies included
- Generic sample query examples and tests
- Comprehensive test suite with:
  - Unit tests for all components
  - Integration tests with real Snowflake connectivity
  - Parameter binding validation tests
  - Sample use case tests against SOME_TABLE
- Demo applications showing practical usage
- Environment variable configuration via .env files
- Complete documentation and usage examples

### Security Features
- SQL injection prevention through PreparedStatement parameter binding
- Private key authentication support (PKCS#8 format)
- Secure credential handling through configuration abstraction
- Connection pooling with proper resource management

### Dependencies
- Apache Camel 4.14.0
- Snowflake JDBC Driver 3.26.1
- HikariCP 5.1.0 for connection pooling
- Jackson 2.17.2 for JSON processing
- SLF4J 2.0.9 for logging
- Java 21 LTS requirement

### Build and Packaging
- Maven-based build system
- OSGi bundle generation with Felix Maven Bundle Plugin
- Source and Javadoc JAR generation
- Standalone JAR with shaded dependencies
- Distribution packages (ZIP/TAR.GZ) with examples
- Release profile with GPG signing support
- Development profile for faster builds

### Documentation
- Comprehensive README with setup instructions
- Sample usage examples and best practices
- API documentation with Javadoc
- Configuration reference
- Security guidelines for private key authentication

### Testing
- JUnit 5 test framework
- Camel Test Support integration
- Environment-based testing with .env configuration
- Mock and integration test scenarios
- Sample query validation
- Parameter binding functionality verification

## [Unreleased]

### Added
- (placeholder)

### Changed
- (placeholder)

### Fixed
- (placeholder)

### Documentation
- (placeholder)

## [1.0.0] - 2025-10-03

### Added
- `privateKeyFile` configuration for loading PKCS#8/PKCS#1 keys from PEM/DER files (PKCS#1 auto-wrapped into PKCS#8).
- System property fallbacks for `snowflake.*` configuration and pass-through `snowflake.jdbc.*` parameters appended to the JDBC URL.
- `outputFormat` also controls JDBC driver result format: JSON by default; Arrow when `arrow` is selected.
- OAuth authentication (`authenticator=oauth`) with `token` property.
- Safer DataSource property handling and explicit username validation in the connection manager.
- Stored procedure invocation (`CALL ...`) with automatic detection, `CallableStatement` execution, and fallback to plain `Statement` on driver API limitation.
- Parameter binding support for `CALL` statements identical to DML/SELECT (`:#param` → positional `?`).
- Dynamic SQL resolution precedence (header `CamelSnowflakeQuery` > SQL-looking body > endpoint configured `query`).
- One-shot sample routes (`snowflake-insert-once`, `snowflake-select-once`, `snowflake-proc-insert-once`) showcasing insert/select/procedure usage.
- Developer convenience script `dev-install.sh` to install the component snapshot and rebuild an individual sample module quickly.

### Changed
- Use typed `SnowflakeBasicDataSource` for key-pair authentication and set the `PrivateKey`/`privateKeyFile` directly (instead of relying on DriverManager string properties).
- Default authenticator to `SNOWFLAKE_JWT` when a private key is provided, unless explicitly overridden.

### Fixed
- Prevent Snowflake JDBC error "Connection property specified more than once: DB" by ensuring database/schema/warehouse/role are only included in the JDBC URL and not set again on the DataSource.
- Clarified documentation around `snowflake.jdbc.*` pass-through: location properties (DB/SCHEMA/WAREHOUSE/ROLE) should not be duplicated.
- Resolved stale shaded sample class issue by documenting fast rebuild script and recommending avoiding duplicate shaded classes in future.

### Documentation
- Component README: expanded with private key formats, `privateKeyFile`, parameter binding controls, system properties, and JDBC pass-through examples.
  - Added `privateKeyPassword` docs (encrypted PEM with `privateKeyFile`)
- Sample README: `.env` opt-in (`-Dsample.useDotenv=true`), exec:java property passing (top-level `-D`), config cheat sheet, OAuth example, and Arrow notes.
- Removed legacy domain-specific mentions in docs/tests for neutral sample naming.
- Fixed README heading characters, trimmed unverifiable coverage/benchmark claims, and updated repository links to dscope-io/dscope-camel-snowflake.
- Added sections on stored procedure (CALL) support, dynamic SQL precedence, one-shot sample routes, and dev-install fast iteration script.
# Changelog

All notable changes to the Camel Snowflake Component will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

- No unreleased changes yet.

## [1.5.0] - 2026-03-22

### Added
- Added a dedicated `io.dscope.camel:camel-snowflake-mcp` module for Snowflake-specific MCP integration.
- Added a reactor build that publishes separate core and MCP artifacts from the same repository.

### Changed
- Bumped the project version to `1.5.0` across the reactor, samples, packaging metadata, and documentation.
- Upgraded `io.dscope.camel:camel-mcp` to `1.4.1`.
- Updated the MCP sample to depend on `io.dscope.camel:camel-snowflake-mcp` instead of bundling MCP support into the core component.
- Refreshed build scripts, VS Code tasks, acceptance commands, and sample launchers for the new artifact layout and version.
- Reduced sample shaded-jar noise by filtering common duplicate metadata resources during shading.

### Removed
- Snowflake-specific MCP routing is no longer shipped inside the core `camel-snowflake` artifact.

## [1.2.0] - 2025-10-08

### Added
- Procedure CALL detection with `CallableStatement` execution and fallback.
- Deterministic SQL source precedence (header > SQL-like body > endpoint `query`).
- One-shot sample routes (insert/select/procedure) replacing earlier dev timers.
- Enhanced debug logging clarifying chosen SQL source, binding results, and execution path.

### Changed
- Samples and docs now default to the 1.2.0 release artifact across all modules.

### Fixed
- Shading guidance and exclusions prevent stale component class drift inside sample shaded jars.

### Documentation
- README and sample docs refreshed with 1.2.0 coordinates and dependency snippets.
- Added notes on stored procedure invocation and dynamic SQL precedence.

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
  - Added `privateKeyFilePassword` (renamed from `privateKeyPassword`; old name still accepted as alias)
- Sample README: `.env` opt-in (`-Dsample.useDotenv=true`), exec:java property passing (top-level `-D`), config cheat sheet, OAuth example, and Arrow notes.
- Removed legacy domain-specific mentions in docs/tests for neutral sample naming.
- Fixed README heading characters, trimmed unverifiable coverage/benchmark claims, and updated repository links to dscope-io/dscope-camel-snowflake.
- Added sections on stored procedure (CALL) support, dynamic SQL precedence, one-shot sample routes, and dev-install fast iteration script.
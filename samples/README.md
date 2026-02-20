# Samples for Camel Snowflake Component

This folder contains runnable example projects demonstrating how to use the custom `snowflake://` Camel component.
Each sample is a standalone Maven project with its own dependencies, configuration, and README.

## Quick Run (YAML sample)
```bash
cd samples/dynamic-query-yaml
mvn -q -DskipTests package
java -jar target/dynamic-query-yaml-1.4.0.jar
```

## Dependency
```xml
<dependency>
  <groupId>io.dscope.camel</groupId>
  <artifactId>camel-snowflake</artifactId>
  <version>1.4.0</version>
</dependency>
```

## Prerequisites
- JDK 21+
- Maven 3.9+
- A Snowflake account and role with permissions to create database/schema/table for testing
- A PKCS#8 RSA private key for key‑pair authentication (stored outside of the repo)

Before running any sample, build and install the component library once from the repo root:
```bash
mvn -q -DskipTests install
```

To build all samples at once:
```bash
cd samples
mvn -q -DskipTests package
```

## Samples catalog

1) dynamic-query-yaml
- Path: `samples/dynamic-query-yaml`
- Highlights:
  - YAML DSL route
  - Dynamic SQL via `sql` header
  - Named parameter binding from headers with prefix `snowflake.`
  - Key‑pair authentication (private key) using `snowflake_jwt`
  - Loads non‑secret properties from `.env`, private key from OS environment
  - Shared setup SQL and helper scripts under `samples/shared`
    - One-shot demo routes (insert/select/procedure) that run once at startup (`snowflake-insert-once`, `snowflake-select-once`, `snowflake-proc-insert-once`)
- Quick start:
  ```bash
  cd samples/dynamic-query-yaml
  # Create .env with connection values (no secrets)
  cat > .env << 'EOF'
  SNOWFLAKE_ACCOUNT=your_account_name
  SNOWFLAKE_DATABASE=YOUR_DB
  SNOWFLAKE_SCHEMA=PUBLIC
  SNOWFLAKE_WAREHOUSE=COMPUTE_WH
  SNOWFLAKE_ROLE=ACCOUNTADMIN
  SNOWFLAKE_USERNAME=your_username
  EOF

  # Provide private key via environment (must be PKCS#8 PEM/Base64)
  export SNOWFLAKE_PRIVATE_KEY="$(cat /path/to/private_key_pkcs8.pem)"

  # (Optional) Create DB/Schema/Table and seed data
  # Review and run shared setup SQL
  snowsql -a <account> -u <username> -r <role> -w <warehouse> -f ../shared/snowflake/setup.sql

  # Build and run
  mvn -q -DskipTests package
  java -jar target/dynamic-query-yaml-1.4.0.jar
  ```
- Details: see `samples/dynamic-query-yaml/README.md`

2) spring-boot-snowflake
- Path: `samples/spring-boot-snowflake`
- Highlights:
  - Spring Boot 3.5 application using Camel Spring Boot
  - Loads routes from `src/main/resources/routes/*.yaml`
  - Uses the `snowflake://` component and maps defaults from `application.yml`
  - Simple timer route runs a query on startup and logs the result
- Quick start:
  ```bash
  cd samples/spring-boot-snowflake
  export SNOWFLAKE_ACCOUNT=your_account
  export SNOWFLAKE_USERNAME=your_user
  export SNOWFLAKE_PRIVATE_KEY_FILE=/abs/path/private_key_pkcs8.pem
  mvn -q spring-boot:run
  ```
- Details: see `samples/spring-boot-snowflake/README.md`

3) dynamic-query-java
- Path: `samples/dynamic-query-java`
- Java RouteBuilder variant of the YAML sample. See module README for usage.

4) dynamic-query-groovy
- Path: `samples/dynamic-query-groovy`
- Groovy RouteBuilder variant (compiled to Java 17 for JDK 21 compatibility). See module README for build/run.

5) dynamic-query-kotlin
- Path: `samples/dynamic-query-kotlin`
- Kotlin RouteBuilder variant. Includes camel-bean for Simple/Bean languages. See module README for build/run.

## Conventions for new samples
When adding a new sample, follow this structure:
- `<sample-name>/pom.xml` — standalone Maven module
- `src/main/java/.../SampleMain.java` — boots Camel Main
- `src/main/resources/routes/*.yaml` — routes in YAML DSL (or Java DSL if needed)
- `src/main/resources/application.properties` — Camel Main and sample config
- `snowflake/` — optional SQL scripts (DDL + seed data)
- `README.md` — purpose, config, and run instructions

Dependency on the component:
```xml
<dependency>
  <groupId>io.dscope.camel</groupId>
  <artifactId>camel-snowflake</artifactId>
  <version>1.4.0</version>
</dependency>
```

If you’d like, we can convert `samples` into a Maven aggregator so all samples build with a single command (keeping them independent from the library release cycle).

### Fast iterative rebuild
From the repo root you can install the component and build a specific sample with:
```bash
./dev-install.sh                # builds component + dynamic-query-yaml
./dev-install.sh dynamic-query-kotlin
```
Add `-U` to force snapshot/plugin updates.
# Spring Boot 3.5 + Camel Snowflake Sample

This sample shows how to use the custom `snowflake://` Camel component in a Spring Boot 3.5 application.

## Build

From the repo root, first install the component:

```bash
mvn -q -DskipTests install
```

Then build this sample:

```bash
cd samples/spring-boot-snowflake
mvn -q -DskipTests package
```

## Run

Provide Snowflake credentials via JVM `-D` properties using the `snowflake.*` naming (see `application.yml`). You can also export environment variables with the exact same names and Spring will map them automatically.

Example using JVM properties:
```bash
mvn -q spring-boot:run \
  -Dspring-boot.run.jvmArguments="\
    -Dsnowflake.account=your_account \
    -Dsnowflake.username=your_user \
    -Dsnowflake.privateKeyFile=/abs/path/private_key_pkcs8.pem \
    -Dsnowflake.database=SAMPLES \
    -Dsnowflake.schema=PUBLIC \
    -Dsnowflake.warehouse=DATAFEED_WH \
    -Dsnowflake.role=sample_role \
    -Dcamel.main.durationMaxSeconds=20 \
    -Dcamel.main.durationHitExitCode=0"
```

Or export environment variables in lowercase with dots replaced by underscores (Spring Boot relaxed binding supports both):
```bash
export snowflake_account=your_account
export snowflake_username=your_user
export snowflake_privateKeyFile=/abs/path/private_key_pkcs8.pem
mvn -q spring-boot:run
```

On startup a simple timer route runs one query and logs the result.

## Notes
- For key-pair auth, prefer `-Dsnowflake.privateKeyFile=/abs/path/private_key_pkcs8.pem` (PEM or DER). The component will set `authenticator=snowflake_jwt` automatically when a private key is supplied unless you override it.
- Do not duplicate location properties across different channels. Database/schema/warehouse/role are included in the JDBC URL. Avoid also passing them as DataSource or `snowflake.jdbc.*` properties to prevent the Snowflake driver error: "Connection property specified more than once: DB".

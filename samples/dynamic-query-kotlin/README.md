# Camel Snowflake Sample — Dynamic Query (Kotlin DSL)

Kotlin DSL route equivalent to the YAML/Java samples. Uses key‑pair auth and `snowflake.*` system properties.

## Build

```bash
mvn -q -DskipTests install   # from repo root (once)
cd samples/dynamic-query-kotlin
mvn -q -DskipTests package
```

## Run (one-shot)

```bash
mvn -q -DskipTests \
  -Dsnowflake.account="$SNOWFLAKE_ACCOUNT" \
  -Dsnowflake.database="$SNOWFLAKE_DATABASE" \
  -Dsnowflake.schema="$SNOWFLAKE_SCHEMA" \
  -Dsnowflake.warehouse="$SNOWFLAKE_WAREHOUSE" \
  -Dsnowflake.role="$SNOWFLAKE_ROLE" \
  -Dsnowflake.username="$SNOWFLAKE_USERNAME" \
  -Dsnowflake.privateKeyFile="/absolute/path/to/private_key_pkcs8.pem" \
  -Dsnowflake.authenticator=snowflake_jwt \
  -Dexec.cleanupDaemonThreads=false \
  exec:java@run-once
```

Notes:
- Headers used to parameterize the query: `user_id`, `min_date`. Defaults come from `application.properties`.
- Includes `camel-bean` to satisfy bean/simple language requirements.
- Route avoids Simple interpolation clashes by using header() + choice() with Kotlin-safe backticked `when`.
- Avoid duplicating DB/SCHEMA/WAREHOUSE/ROLE outside the JDBC URL.
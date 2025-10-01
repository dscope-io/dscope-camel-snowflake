# Camel Snowflake Sample — Dynamic Query (Groovy DSL)

Groovy DSL route equivalent to the YAML/Java samples. Uses key‑pair auth and `snowflake.*` system properties.

## Build

```bash
mvn -q -DskipTests install   # from repo root (once)
cd samples/dynamic-query-groovy
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
- Avoid duplicating DB/SCHEMA/WAREHOUSE/ROLE outside the JDBC URL.
- Authenticator defaults to `snowflake_jwt` when a private key is provided.
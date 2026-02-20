# Camel Snowflake Sample — Dynamic Query (Java DSL)

This sample mirrors the YAML dynamic query sample but defines the route in Java using RouteBuilder.
It uses key‑pair authentication and reads connection defaults from `application.properties`, with secrets provided via `-Dsnowflake.*`.

## Build

From the repo root (install component):

```bash
mvn -q -DskipTests install
```

Then build this sample:

```bash
cd samples/dynamic-query-java
mvn -q -DskipTests package
```

## Run (one-shot)

```bash
mvn -q -DskipTests \
  -Dsnowflake.username="$SNOWFLAKE_USERNAME" \
  -Dsnowflake.privateKeyFile="/absolute/path/to/private_key_pkcs8.pem" \
  exec:java -Dexec.mainClass=io.dscope.camel.snowflake.sample.java.RunOnce
```

Or run the shaded jar and send a message to `direct:snowflakeQuery` from your own code.

## Notes
- Prefer `snowflake.privateKeyFile` for key‑pair auth; the component will use SNOWFLAKE_JWT authenticator automatically.
- Do not duplicate DB/SCHEMA/WAREHOUSE/ROLE outside the JDBC URL.

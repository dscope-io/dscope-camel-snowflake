# Samples Acceptance Test Plan (Version 1.4.0)

## 1. Purpose
Validate that all sample applications in `samples/` build, start, authenticate, and execute their primary Snowflake workflow successfully for release acceptance.

## 2. Scope
In scope:
- `dynamic-query-yaml`
- `dynamic-query-java`
- `dynamic-query-groovy`
- `dynamic-query-kotlin`
- `spring-boot-snowflake`
- `mcp-snowflake-yaml`

Out of scope:
- Performance/load testing
- Security penetration testing
- Production HA/failover behavior

## 3. Test Environment
Required:
- JDK 21+
- Maven 3.8+ (or explicit Maven binary path)
- Snowflake account with permissions for DB/schema/table/procedure setup
- PKCS#8 private key for key-pair auth

Suggested environment variables:
- `SNOWFLAKE_ACCOUNT`
- `SNOWFLAKE_DATABASE`
- `SNOWFLAKE_SCHEMA`
- `SNOWFLAKE_WAREHOUSE`
- `SNOWFLAKE_ROLE`
- `SNOWFLAKE_USERNAME`
- `SNOWFLAKE_PRIVATE_KEY_FILE`

## 4. Test Data Setup (Precondition)
1. From repo root, install component artifacts:
   - `mvn -q -DskipTests install`
2. Seed Snowflake objects used by samples:
   - Run `samples/shared/snowflake/setup.sql` with SnowSQL or Snowflake UI worksheet.
3. Confirm target table and sample procedure exist:
   - `YOUR_DB.PUBLIC.SOME_TABLE`
   - `insert_new_sample_row(...)`

Pass criteria:
- Component installs to local Maven cache without errors.
- Setup script completes successfully.

## 5. Acceptance Matrix

| ID | Sample | Build Test | Run Test | Functional Assertion | Pass/Fail |
|---|---|---|---|---|---|
| AT-01 | dynamic-query-yaml | `cd samples/dynamic-query-yaml && mvn -q -DskipTests package` | `java -Dsnowflake.username=$SNOWFLAKE_USERNAME -Dsnowflake.privateKeyFile=$SNOWFLAKE_PRIVATE_KEY_FILE -jar target/dynamic-query-yaml-1.4.0.jar` | Logs show query executed and rows returned (or expected JSON output) |  |
| AT-02 | dynamic-query-java | `cd samples/dynamic-query-java && mvn -q -DskipTests package` | `mvn -q -DskipTests -Dsnowflake.username=$SNOWFLAKE_USERNAME -Dsnowflake.privateKeyFile=$SNOWFLAKE_PRIVATE_KEY_FILE exec:java -Dexec.mainClass=io.dscope.camel.snowflake.sample.java.RunOnce` | One-shot route completes without exception |  |
| AT-03 | dynamic-query-groovy | `cd samples/dynamic-query-groovy && mvn -q -DskipTests package` | `mvn -q -DskipTests -Dsnowflake.account=$SNOWFLAKE_ACCOUNT -Dsnowflake.database=$SNOWFLAKE_DATABASE -Dsnowflake.schema=$SNOWFLAKE_SCHEMA -Dsnowflake.warehouse=$SNOWFLAKE_WAREHOUSE -Dsnowflake.role=$SNOWFLAKE_ROLE -Dsnowflake.username=$SNOWFLAKE_USERNAME -Dsnowflake.privateKeyFile=$SNOWFLAKE_PRIVATE_KEY_FILE -Dsnowflake.authenticator=snowflake_jwt -Dexec.cleanupDaemonThreads=false exec:java@run-once` | One-shot route completes and logs Snowflake response |  |
| AT-04 | dynamic-query-kotlin | `cd samples/dynamic-query-kotlin && mvn -q -DskipTests package` | `mvn -q -DskipTests -Dsnowflake.account=$SNOWFLAKE_ACCOUNT -Dsnowflake.database=$SNOWFLAKE_DATABASE -Dsnowflake.schema=$SNOWFLAKE_SCHEMA -Dsnowflake.warehouse=$SNOWFLAKE_WAREHOUSE -Dsnowflake.role=$SNOWFLAKE_ROLE -Dsnowflake.username=$SNOWFLAKE_USERNAME -Dsnowflake.privateKeyFile=$SNOWFLAKE_PRIVATE_KEY_FILE -Dsnowflake.authenticator=snowflake_jwt -Dexec.cleanupDaemonThreads=false exec:java@run-once` | One-shot route completes and logs Snowflake response |  |
| AT-05 | spring-boot-snowflake | `cd samples/spring-boot-snowflake && mvn -q -DskipTests package` | `mvn -q spring-boot:run -Dspring-boot.run.jvmArguments="-Dsnowflake.account=$SNOWFLAKE_ACCOUNT -Dsnowflake.username=$SNOWFLAKE_USERNAME -Dsnowflake.privateKeyFile=$SNOWFLAKE_PRIVATE_KEY_FILE -Dsnowflake.database=$SNOWFLAKE_DATABASE -Dsnowflake.schema=$SNOWFLAKE_SCHEMA -Dsnowflake.warehouse=$SNOWFLAKE_WAREHOUSE -Dsnowflake.role=$SNOWFLAKE_ROLE -Dcamel.main.durationMaxSeconds=20 -Dcamel.main.durationHitExitCode=0"` | App starts, timer route executes query, process exits cleanly |  |
| AT-06 | mcp-snowflake-yaml | `cd samples/mcp-snowflake-yaml && mvn -q -DskipTests package` | `java -Dsnowflake.account=$SNOWFLAKE_ACCOUNT -Dsnowflake.username=$SNOWFLAKE_USERNAME -Dsnowflake.privateKeyFile=$SNOWFLAKE_PRIVATE_KEY_FILE -Dsnowflake.database=$SNOWFLAKE_DATABASE -Dsnowflake.schema=$SNOWFLAKE_SCHEMA -Dsnowflake.warehouse=$SNOWFLAKE_WAREHOUSE -Dsnowflake.role=$SNOWFLAKE_ROLE -jar target/mcp-snowflake-yaml-1.4.0.jar` | `tools/list` returns tools; `tools/call` returns success payload |  |

## 6. MCP API Acceptance Steps (AT-06 detail)
After the MCP sample starts:
1. Health check:
   - `curl -s http://localhost:8080/mcp/health`
2. Tools list:
   - `curl -s -X POST http://localhost:8080/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -H 'MCP-Protocol-Version: 2025-06-18' -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'`
3. Tool call:
   - `curl -s -X POST http://localhost:8080/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -H 'MCP-Protocol-Version: 2025-06-18' -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"selectSample","arguments":{"user_id":1,"min_date":"1970-01-01"}}}'`

Pass criteria:
- Health endpoint responds successfully.
- `tools/list` includes configured tools.
- `tools/call` returns a valid JSON-RPC result and no server exception.

## 7. Negative Acceptance Checks
Run at least these two checks:
1. Invalid key file path
   - Use non-existent `snowflake.privateKeyFile` and verify startup fails with clear auth/config error.
2. Missing required account/user
   - Omit `snowflake.account` or `snowflake.username` and verify startup fails fast with clear validation error.

Pass criteria:
- Failures are explicit, actionable, and do not hang indefinitely.

## 8. Evidence Collection
For each AT case, capture:
- Command executed
- Exit code
- Start/end timestamp
- Relevant log excerpt (success or failure)
- Screenshot or terminal capture (optional)

## 9. Exit Criteria
Acceptance is complete when:
- All six AT cases pass in the same release candidate context.
- Negative checks produce expected controlled failures.
- No unresolved blocker remains for sample startup, auth, or main route execution.

## 10. Execution Order (Recommended)
1. Precondition setup (Section 4)
2. Dynamic query samples: YAML -> Java -> Groovy -> Kotlin
3. Spring Boot sample
4. MCP sample and MCP API checks
5. Negative checks
6. Final acceptance sign-off

## 11. Optional Automation Runner
Use the helper script to execute this plan with logs and summary output:

- Build-only verification:
   - `./samples/run-acceptance-tests.sh --build-only`
- Full acceptance flow (build + runtime checks):
   - `./samples/run-acceptance-tests.sh --full`
- Full flow loading variables from an env file:
   - `./samples/run-acceptance-tests.sh --full --env-file samples/mcp-snowflake-yaml/.env`
- Key material for runner full mode can be provided as either:
   - `SNOWFLAKE_PRIVATE_KEY_FILE=/abs/path/private_key_pkcs8.pem`, or
   - `SNOWFLAKE_PRIVATE_KEY=<base64-or-pem-content>`

Result artifacts are written under:
- `samples/acceptance-results/<timestamp>/`

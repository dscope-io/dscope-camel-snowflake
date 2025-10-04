# Camel Snowflake Sample — Dynamic Query (YAML)

This sample shows how to use the custom `snowflake://` Camel component with a YAML route and dynamic SQL/parameters.
It uses key‑pair authentication (private key). Connection settings come from `application.properties` and System properties.
Username and private key are intentionally not stored in `application.properties`; they are provided at runtime via System properties.
The sample defaults to JSON output for results (driver result format also defaults to JSON; set `-Dsnowflake.outputFormat=arrow` to opt into Arrow).

## What it does
- Loads core routes from `src/main/resources/routes/snowflake-dynamic.yaml`
  and `src/main/resources/routes/snowflake-insert.yaml`
- Includes two sample one-shot timer routes:
  - `snowflake-insert-once` (performs a single INSERT at startup)
  - `snowflake-select-once` (selects rows after a small delay)
  These live in separate YAML files (`snowflake-insert-once.yaml`, `snowflake-select-once.yaml` after renaming) and can be removed for production.
- Adds an optional one-shot stored procedure route: `snowflake-proc-insert-once` (invokes `CALL insert_new_sample_row(...)`) demonstrating parameter binding with a stored procedure.
- Uses `application.properties` for defaults and picks up System property overrides
- Reads `.env` at startup (if enabled) and maps env vars to System properties (see EnvLoader). Enable with `-Dsample.useDotenv=true`.
- Executes a query using named parameter binding with headers prefixed by `snowflake.` (e.g., `snowflake.user_id`)
- Supports driver pass-through options via `jdbc.*` URI parameters (they are appended to the JDBC URL)

The default query is:
```
SELECT * FROM YOUR_DB.PUBLIC.SOME_TABLE WHERE USER_ID = :#user_id AND CREATED_AT >= :#min_date
```
You can override the SQL per message via the `sql` header.

### SQL Resolution Precedence (sample runtime)
The sample (and component) determine which SQL to run using this order (highest wins):
1. `sql` header (if present and non-empty)
2. Message body if it appears to start with a SQL verb (`SELECT`, `WITH`, `INSERT`, `UPDATE`, `DELETE`, `CALL`, `CREATE`, `ALTER`, `MERGE`)
3. Default query configured in `application.properties` (`snowflake.query.default` or route-level constant)

This lets you keep a safe baseline query while overriding ad‑hoc without modifying the route files.

---

## Prerequisites
- JDK 21+
- Maven 3.8+
- Snowflake account with a warehouse and a role that can create DB/Schema/Table
- A PKCS#8 RSA private key for key‑pair authentication
- The component library installed to your local Maven repo (built from the repo root)

Optional:
- `snowsql` CLI to run the setup script
 - Shared helper scripts and keys: see `samples/shared/{scripts,keys}`

### Install snowsql
- Official docs: https://docs.snowflake.com/en/user-guide/snowsql-install-config
- macOS (Homebrew):
  ```bash
  brew install --cask snowflake-snowsql
  # or legacy formula:
  # brew install snowsql
  ```
- Linux: download the installer from Snowflake docs (RPM/DEB or tarball) and follow platform steps.
- Windows: download the MSI from the docs page and run the installer.

Verify installation:
```bash
snowsql --version
```

---

## 1) Build and install the component library (once)
From the repository root:

```bash
mvn -q -DskipTests install
```

This places `io.dscope.camel:camel-snowflake:1.0.0-SNAPSHOT` into your local Maven repository so the sample can depend on it.

For rapid iterations you can use the root helper script (creates fresh snapshot then builds this sample):
```bash
./dev-install.sh              # builds component + dynamic-query-yaml
./dev-install.sh -U           # force snapshot & plugin updates
```

---

## 2) Create the Snowflake objects and seed data
Run the script aligned with the sample’s default query:

File: `samples/shared/snowflake/setup.sql`
- Creates DB `YOUR_DB`, schema `PUBLIC`, table `SOME_TABLE`
- Inserts several rows for `USER_ID` 1–3 with recent timestamps
- (Also contains an example stored procedure `insert_new_sample_row` returning a message.)

Options to run it:
- Snowflake Web UI worksheet: open the script, adjust names if needed, run.
- snowsql (example):
```bash
snowsql -a <account> -u <username> -r <role> -w COMPUTE_WH -f samples/shared/snowflake/setup.sql
```

Make sure `YOUR_DB`, `PUBLIC`, and `COMPUTE_WH` match what you configure below.

---

## 3) Configure credentials and connection
This sample uses a `.env` for non‑secret values (load it with `-Dsample.useDotenv=true`). Provide username and private key at runtime via System properties (e.g., with our helper aliases) or export them in your environment and the alias will pass them through. You can provide the private key content or a file path:

- Content: `-Dsnowflake.privateKey=...` (PKCS#8 PEM or Base64; escape newlines as `\n`)
- File: `-Dsnowflake.privateKeyFile=/path/to/private_key_pkcs8.pem` (PEM or DER)

Create `samples/dynamic-query-yaml/.env` with:
```
SNOWFLAKE_ACCOUNT=your_account_name
SNOWFLAKE_DATABASE=YOUR_DB
SNOWFLAKE_SCHEMA=PUBLIC
SNOWFLAKE_WAREHOUSE=COMPUTE_WH
SNOWFLAKE_ROLE=ACCOUNTADMIN
SNOWFLAKE_USERNAME=your_username
# Do NOT put the private key here.
```

Export the private key in your shell (macOS zsh):
```bash
# Private key must be PKCS#8 PEM or Base64 (PEM with header/footer is fine)
export SNOWFLAKE_PRIVATE_KEY="$(cat /path/to/private_key_pkcs8.pem)"
export SNOWFLAKE_USERNAME="your_username"
```

If your key is PKCS#1, convert to PKCS#8 first:
```bash
openssl pkcs8 -topk8 -inform PEM -in rsa_private_key.pem -outform PEM -nocrypt -out private_key_pkcs8.pem
```

Or export a key file path to avoid newline escaping:
```bash
export SNOWFLAKE_PRIVATE_KEY_FILE="/absolute/path/to/private_key_pkcs8.pem"
```
Authenticator: the sample defaults to `snowflake_jwt` for key‑pair auth when a private key is used. You can override via `application.properties` or `-Dsnowflake.authenticator=...`.

---

### 3b) Register the public key for your Snowflake user (required for key‑pair auth)
Snowflake must have your user’s public key to verify JWTs signed with your private key.

After generating keys with the shared scripts, you’ll have:
- `samples/shared/keys/private_key_pkcs8.pem` — your private key (keep secret)
- `samples/shared/keys/public_key.b64` — the Base64 (DER) public key

Set the public key on the Snowflake user (requires a role like `SECURITYADMIN`):

Option A: Using snowsql

```bash
# Load the Base64 public key into a shell variable
KEY_B64="$(cat samples/shared/keys/public_key.b64)"

# Run ALTER USER with a security admin user/role
snowsql \
  -a <account> \
  -u <admin_username> \
  -r SECURITYADMIN \
  -q "ALTER USER <app_username> SET RSA_PUBLIC_KEY='${KEY_B64}';"
```

Option B: Using the Snowflake UI
- Go to Admin > Users > select your `<app_username>`
- Edit and set `RSA_PUBLIC_KEY` to the contents of `public_key.b64`

Notes
- For key rotation you can use `RSA_PUBLIC_KEY_2` as a secondary key.
- Ensure there are no newlines/whitespace; `public_key.b64` is already a single line.

---

## 4) Build the sample
From the sample folder:
```bash
cd samples/dynamic-query-yaml
mvn -q -DskipTests package
```

---

## 5) Run the sample
You can run the shaded jar directly or use the provided helper functions which pass System properties for `snowflake.username` and `snowflake.privateKey`. Prefer using the `snowflake.*` naming. If you have legacy `SNOWFLAKE_*` env vars, set `-Dsample.useDotenv=true` and the sample will map supported variables from a `.env` file to `snowflake.*` system properties (except the private key for safety).

Option A: Run shaded jar directly

```bash
java \
  -Dsnowflake.username="$SNOWFLAKE_USERNAME" \
  -Dsnowflake.privateKey="${SNOWFLAKE_PRIVATE_KEY//$'\n'/\\n}" \
  -jar target/dynamic-query-yaml-1.1.0-SNAPSHOT.jar
```

Option A2: Using a key file instead of embedding the key

```bash
java \
  -Dsnowflake.username="$SNOWFLAKE_USERNAME" \
  -Dsnowflake.privateKeyFile="/absolute/path/to/private_key_pkcs8.pem" \
  -jar target/dynamic-query-yaml-1.1.0-SNAPSHOT.jar
```

Option A3: One‑shot runner via Maven (exec:java)

When using `mvn exec:java`, pass `-Dsnowflake.*` at the Maven CLI level (do not put them in `-Dexec.jvmArgs`).

```bash
mvn -q -DskipTests \
  -Dsnowflake.username="$SNOWFLAKE_USERNAME" \
  -Dsnowflake.privateKeyFile="/absolute/path/to/private_key_pkcs8.pem" \
  exec:java -Dexec.mainClass=io.dscope.camel.snowflake.sample.RunOnce
```
```

Option B: Use helper alias (recommended)

```bash
source samples/dynamic-query-yaml/shell-aliases.zsh
sf-sample-run
```

### Config cheat sheet (system properties)

- Core: `-Dsnowflake.account=... -Dsnowflake.username=... [-Dsnowflake.database=...] [-Dsnowflake.schema=...] [-Dsnowflake.warehouse=...] [-Dsnowflake.role=...]`
- Key-pair (contents): `-Dsnowflake.privateKey="${SNOWFLAKE_PRIVATE_KEY//$'\n'/\\n}"`
- Key-pair (file): `-Dsnowflake.privateKeyFile=/abs/path/private_key_pkcs8.pem`
- Output: `-Dsnowflake.outputFormat=rows|json|xml|arrow`
- JDBC extras: `-Dsnowflake.jdbc.CLIENT_SESSION_KEEP_ALIVE=true`

### OAuth quick example (alternative auth mode)

```bash
mvn -q -DskipTests \
  -Dsnowflake.account="$SNOWFLAKE_ACCOUNT" \
  -Dsnowflake.username="oauth_user" \
  -Dsnowflake.authenticator=oauth \
  -Dsnowflake.token="$(cat /path/to/access_token.txt)" \
  exec:java -Dexec.mainClass=io.dscope.camel.snowflake.sample.RunOnce
```

The route listens on `direct:snowflakeQuery`. To actually execute a query you need to send a message to that endpoint.

### Triggering queries from your own routes
If you have another Camel route in your environment, produce to `direct:snowflakeQuery` and set these headers:
- `sql` (optional): overrides the default query
- `snowflake.user_id`: binds to `:#user_id`
- `snowflake.min_date`: binds to `:#min_date`

---

## How parameter binding works
- Named parameters in SQL use `:#name` syntax
- Headers with prefix `snowflake.` bind to those names
  - `snowflake.user_id` -> `:#user_id`
  - `snowflake.min_date` -> `:#min_date`
- Prefix can be changed via `snowflake.parameterPrefix` in `application.properties`

---

## Insert route

This sample also includes an insert route listening on `direct:snowflakeInsert`.

- Default SQL (overridable via `sql` header):
  `INSERT INTO PUBLIC.SOME_TABLE (ID, USER_ID, CREATED_AT, AMOUNT, DETAILS)
   VALUES (:#id, :#user_id, to_timestamp_ntz(:#created_at), :#amount, parse_json(:#details))`
- Provide values via headers (mapped using the `snowflake.` prefix):
  - `id` → header `snowflake.id`
  - `user_id` → header `snowflake.user_id`
  - `created_at` → header `snowflake.created_at` (e.g., `2025-10-22 12:34:56`)
  - `amount` → header `snowflake.amount` (e.g., `123.45`)
  - `details` → header `snowflake.details` (JSON string, e.g., `{"status":"new"}`)

Quick dev trigger snippet you can temporarily add to a route file during testing:
```yaml
- route:
  id: snowflake-insert-dev-once
The sample already includes ready-to-use one-shot routes:

| File | Route ID | Purpose |
|------|----------|---------|
| `routes/snowflake-insert-once.yaml` | `snowflake-insert-once` | Performs a single parameterized INSERT at startup |
| `routes/snowflake-select-once.yaml` | `snowflake-select-once` | Runs a one-time SELECT after a short delay (3s) |
| `routes/snowflake-proc-once.yaml` | `snowflake-proc-insert-once` | Invokes stored procedure `insert_new_sample_row` demonstrating parameter binding |

Disable them by deleting those files or (after migrating to Java DSL) guarding route creation with the `sample.enableOnce` system property (see `application.properties`).

To produce inserts manually from another route instead of using the one-shot file, set these headers then send to `direct:snowflakeInsert`:
`snowflake.id`, `snowflake.user_id`, `snowflake.amount`, `snowflake.details_json` (JSON string).

---

## Stored procedure route

The one-shot stored procedure route (`snowflake-proc-insert-once`) calls:
```
CALL insert_new_sample_row(:#id,:#amount,:#details_json)
```
Headers provided:
- `snowflake.id`
- `snowflake.amount`
- `snowflake.details_json` (JSON string)

You can override the call by setting an `sql` header before execution. The default template comes from `application.properties`: `snowflake.proc.insertSample`.

To invoke a procedure from another route interactively, you could add a `direct:snowflakeProc` route and set the same headers, then set `sql` to your `CALL ...` statement.

Procedure binding notes:
* Parameter header resolution follows: exact name (`id`) → prefixed (`snowflake.id`) → Camel style (`CamelSnowflakeId`).
* All `:#name` placeholders in the CALL are converted to positional `?` parameters before execution.
* The component first uses `CallableStatement`; if the Snowflake driver rejects the API for that procedure form, it falls back to a plain `Statement` transparently.

---

## Files of interest
- `src/main/resources/routes/snowflake-dynamic.yaml` — dynamic SELECT route
- `src/main/resources/routes/snowflake-insert.yaml` — core INSERT route
- `src/main/resources/routes/snowflake-proc-once.yaml` — stored procedure one-shot demo
- `src/main/resources/application.properties` — Camel Main + sample defaults
- `src/main/java/.../SampleMain.java` — Boots Camel Main and loads .env
- `samples/shared/snowflake/setup.sql` — Creates DB/Schema/Table + seed data (shared across samples)

---

## Troubleshooting
- Auth errors: ensure `snowflake.authenticator=snowflake_jwt` and the private key is PKCS#8.
- Warehouse/DB/Schema not found: run `setup.sql` and verify names match `.env`.
- "Either password or private key" error: private key must be provided via `SNOWFLAKE_PRIVATE_KEY`.
- Key parsing failures: convert the private key to PKCS#8 PEM as shown above.

### Output formats
- `rows` — the body is `List<Map<String,Object>>`
- `json` — JSON string (sample default; requires `jackson-databind`, already included)
- `xml` — XML string
- `arrow` — requests Arrow result format at the JDBC driver level and typically requires JVM `--add-opens` flags

Change the default via `snowflake.outputFormat` in `application.properties`.

Driver-level result format:
- If `outputFormat=arrow`, the sample appends `JDBC_QUERY_RESULT_FORMAT=ARROW` to the JDBC URL.
- Otherwise, it uses `JDBC_QUERY_RESULT_FORMAT=JSON` to avoid extra JVM `--add-opens` flags.

### Passing extra Snowflake JDBC parameters
You can add `jdbc.*` parameters on the endpoint URI and they’ll be appended to the JDBC URL, or pass them as system properties with the `snowflake.jdbc.` prefix. Examples:
- `jdbc.CLIENT_SESSION_KEEP_ALIVE=true`
- `jdbc.OCSP_FAIL_OPEN=true`

System property form:
```bash
-Dsnowflake.jdbc.CLIENT_SESSION_KEEP_ALIVE=true \
-Dsnowflake.jdbc.CLIENT_SESSION_KEEP_ALIVE_HEARTBEAT_FREQUENCY=900
```

These are URL-encoded automatically. Only use parameters supported by the Snowflake JDBC driver.

Note on duplication: Database/schema/warehouse/role are already included in the JDBC URL the component builds. Do not also provide these via `snowflake.jdbc.*` (or DataSource properties), or the Snowflake driver may fail with "Connection property specified more than once: DB". Keep location values only in the URL.

---

## Security notes
- Keep private keys out of version control and `.env`.
- Prefer a dedicated Snowflake role/warehouse for testing.
- Rotate keys regularly and use least‑privileged roles.

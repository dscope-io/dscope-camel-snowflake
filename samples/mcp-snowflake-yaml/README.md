# Camel Snowflake Sample — MCP over HTTP (YAML)

This sample demonstrates exposing Snowflake queries through a minimal Model Context Protocol (MCP)-style HTTP endpoint using the custom `snowflake:` Camel component and the MCP processors registered in the component module (e.g. `mcpJsonRpcEnvelope`, `mcpHttpValidator`, `mcpSnowflakeRequest`, `mcpSnowflakeResponse`, `mcpSnowflakeError`, `mcpToolsList`).

## What it provides
- An HTTP endpoint (`/mcp`) that accepts JSON-RPC 2.0 style MCP tool requests
- Tool discovery via `tools/list`
- Tool invocation via `tools/call` with multiple tools:
  - `selectSample` (SELECT with bound parameters)
  - `insertSample` (INSERT with bound parameters)
- Error responses wrapped in JSON-RPC error envelopes
- Protocol version negotiation headers (basic) handled by `mcpHttpValidator`
- Server-Sent Events stream at `/mcp/stream` for MCP stream transport handshakes
- Basic request protection:
  - Size guard (default 32 KiB, configure with `-Dmcp.maxRequestBytes=65536`)
  - Fixed-window rate limiting (default 50 req/sec, configure with `-Dmcp.rate.maxRequests` & `-Dmcp.rate.windowMillis`)
  - Simple health endpoint at `/mcp/health`

The sample reuses the demo query pattern from the other dynamic samples:
```sql
SELECT * FROM YOUR_DB.PUBLIC.SOME_TABLE WHERE USER_ID = :#user_id AND CREATED_AT >= :#min_date
```

## Prerequisites
- Java 17 or newer on your `PATH`
- Apache Maven 3.9+
- A Snowflake account with a warehouse, database, schema, and role you can target
- A service user with either asymmetric key authentication (recommended) or OAuth/session token access
- Network ingress allowed from the host running this sample to the Snowflake account region

Populate the Snowflake objects referenced in the sample queries (for example `SOME_TABLE`) before running the service. See `samples/dynamic-query-yaml/README.md` if you need guidance creating demo tables.

### Quick config check
Review `src/main/resources/application.properties` and update the defaults for `snowflake.account`, `snowflake.database`, `snowflake.schema`, `snowflake.warehouse`, and `snowflake.role`. Leave the `username` and auth secrets unset in this file—pass them at runtime via JVM properties or environment variables.

You can optionally create a `.env` file (not committed) alongside this README and source it before running the commands below, for example:

```bash
export SNOWFLAKE_ACCOUNT="acme-org.us-east-1"
export SNOWFLAKE_USERNAME="svc_mcp"
export SNOWFLAKE_PRIVATE_KEY_FILE="$HOME/.snowflake/keys/mcp_private_key_pkcs8.pem"
```

## Running
Ensure you have built and installed the parent and component first:
```bash
mvn -q -DskipTests install
```

Then build this sample:
```bash
cd samples/mcp-snowflake-yaml
mvn -q -DskipTests package
```

Run with key-pair authentication (preferred):
```bash
java \
  -Dsnowflake.account="$SNOWFLAKE_ACCOUNT" \
  -Dsnowflake.username="$SNOWFLAKE_USERNAME" \
  -Dsnowflake.privateKeyFile="/absolute/path/to/private_key_pkcs8.pem" \
  -Dsnowflake.database=YOUR_DB \
  -Dsnowflake.schema=PUBLIC \
  -Dsnowflake.warehouse=COMPUTE_WH \
  -Dsnowflake.role=ACCOUNTADMIN \
  -jar target/mcp-snowflake-yaml-1.2.0.jar
```

Once running it will bind (by default) to `http://0.0.0.0:8080/mcp`.

### Quickstart script
To build and launch in a single step, populate a local `.env` (see examples above) and run:

```bash
chmod +x quickstart.sh
./quickstart.sh
```

Key behaviour:
- Sources `.env`, validates mandatory variables, and ensures the private key file exists.
- Runs `mvn -q -DskipTests package` unless you pass `--no-build`.
- Launches the service with the configured JVM system properties. Override defaults with `SNOWFLAKE_APP_PORT`, `SNOWFLAKE_AUTHENTICATOR`, or `--extra-jvm "-Dfoo=bar"`.

### Alternative authentication flows
- **Inline private key string**: replace `-Dsnowflake.privateKeyFile=…` with `-Dsnowflake.privateKey="$(cat key.pem | awk 'NF {sub(/\r$/, ""); printf "%s\\n", $0}')"`. Remember to escape newlines as `\n`.
- **Password + authenticator**: supply `-Dsnowflake.authenticator=snowflake` and add `-Dsnowflake.password=…` (only for testing; prefer key or OAuth).
- **OAuth bearer**: set `-Dsnowflake.oauthToken=…` and change `snowflake.authenticator=oauth`.

### Adjusting server behaviour
- Override the listen port with `-Dmcp.server.port=9090` (see `routes/mcp-snowflake.yaml`).
- Tune the rate limiter and payload guard: `-Dmcp.rate.maxRequests=100`, `-Dmcp.rate.windowMillis=1000`, `-Dmcp.maxRequestBytes=131072`.
- Enable verbose logs by adding `-Dlogging.level.io.dscope.camel.snowflake.mcp=DEBUG`.

### Subscribe to MCP stream
```bash
curl -Ns http://localhost:8080/mcp/stream \
  -H 'Accept: text/event-stream'
```

The sample currently emits an initial heartbeat event so MCP clients can keep the connection open. Extend `McpStreamProcessor` if you need periodic heartbeats or to forward Snowflake-derived notifications.

### List tools
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | jq
```

#### Sample response
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "selectSample",
        "title": "Select Sample Rows",
        "description": "Run a parameterized SELECT filtering by user_id and minimum created_at date.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "user_id": {
              "type": "integer",
              "description": "User identifier to filter rows"
            },
            "min_date": {
              "type": "string",
              "description": "Earliest created_at date (YYYY-MM-DD)"
            }
          },
          "required": [
            "user_id",
            "min_date"
          ]
        },
        "outputSchema": {
          "type": "object",
          "properties": {
            "status": {
              "type": "string"
            },
            "result": {
              "description": "Result rows from Snowflake"
            }
          },
          "required": [
            "status"
          ]
        },
        "annotations": {
          "category": "snowflake"
        }
      },
      {
        "name": "insertSample",
        "title": "Insert Sample Row",
        "description": "Insert a demo row into SOME_TABLE with bound parameters.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "user_id": {
              "type": "integer",
              "description": "User identifier"
            },
            "amount": {
              "type": "number",
              "description": "Monetary amount"
            },
            "details": {
              "type": "string",
              "description": "Details or description text"
            }
          },
          "required": [
            "user_id",
            "amount",
            "details"
          ]
        },
        "outputSchema": {
          "type": "object",
          "properties": {
            "status": {
              "type": "string"
            },
            "result": {
              "description": "Insert confirmation / update count"
            }
          },
          "required": [
            "status"
          ]
        },
        "annotations": {
          "category": "snowflake"
        }
      }
    ]
  }
}
```

### Call tool (run select)
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"selectSample","arguments":{"user_id":7,"min_date":"1970-01-01"}}}' | jq
```

#### Sample response
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Select Sample Rows.\nRow count: 2"
      }
    ],
    "structuredContent": {
      "status": "ok",
      "result": [
        {
          "USER_ID": 7,
          "AMOUNT": 42.55,
          "DETAILS": "Inserted via MCP",
          "CREATED_AT": "2024-06-01T12:00:00Z"
        },
        {
          "USER_ID": 7,
          "AMOUNT": 12.34,
          "DETAILS": "Seed row",
          "CREATED_AT": "2024-05-31T08:15:00Z"
        }
      ],
      "method": "selectSample",
      "rowCount": 2
    },
    "isError": false
  }
}
```

### Call tool (run insert)
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"insertSample","arguments":{"user_id":7,"amount":42.55,"details":"Inserted via MCP"}}}' | jq
```

#### Sample response
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Insert Sample Row.\nUpdate count: 1"
      }
    ],
    "structuredContent": {
      "status": "ok",
      "result": 1,
      "method": "insertSample",
      "updateCount": 1
    },
    "isError": false
  }
}
```

### Call tool (validation error)
```bash
curl -s -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-06-18' \
  -d '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"selectSample","arguments":{"min_date":"1970-01-01"}}}' | jq
```

#### Sample response
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "error": {
    "code": -32602,
    "message": "Missing required argument(s): user_id",
    "data": {
      "status": "error",
      "httpStatus": 400,
      "exception": "IllegalArgumentException",
      "method": "selectSample"
    }
  }
}
```

If validation fails before the request snapshot is captured, the `data.request` object may be omitted as shown above.

If you omit required arguments you will receive a structured JSON-RPC error envelope.

### Connection overrides in a request
You can override Snowflake connection/auth properties per call inside the `connection` object or at the top level:
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "name": "selectSample",
    "arguments": {"user_id": 7, "min_date": "1970-01-01"},
    "connection": {
      "warehouse": "COMPUTE_WH",
      "role": "ACCOUNTADMIN",
      "privateKeyFile": "/abs/path/private_key_pkcs8.pem"
    }
  }
}
```
Supported keys (snake_case variants accepted): `account`, `username`, `password`, `privateKey`, `privateKeyFile`, `privateKeyFilePassword`, `oauthToken`, `database`, `schema`, `warehouse`, `role`, `authenticator`, `parameterPrefix`.
Sensitive values are masked (`***`) in the echoed snapshot stored under `mcp.snowflake.request`.

Auth conflict rules:
- Do not combine `password` with any of (`privateKey`, `privateKeyFile`).
- Do not combine `oauthToken` with `password` or any private key option.
Conflicting combinations raise a 400-style error (IllegalArgumentException propagated by the route) before any Snowflake call is attempted.

On conflict you receive a JSON-RPC error with code `-32010` (custom application error) and message describing the invalid combination.
Example response snippet:
```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "error": {
    "code": -32010,
    "message": "Conflicting authentication overrides: oauthToken cannot be combined with password or private key authentication",
    "data": {
      "status": "error",
      "httpStatus": 400,
      "exception": "IllegalArgumentException",
      "method": "selectSample"
    }
  }
}
```
### Health
```bash
curl -s http://localhost:8080/mcp/health | jq
```

## Tool definitions
Tool metadata comes from an internal YAML (`mcp/methods.yaml`) expected on the classpath by `McpMethodCatalog`. For this sample we provide a minimal copy under `src/main/resources/mcp/methods.yaml`.

### Layout refresher
```text
src/main/resources/
├── application.properties   # Camel Main + Snowflake defaults
├── mcp/
│   └── methods.yaml         # Tool catalogue consumed by McpMethodCatalog
└── routes/
  └── mcp-snowflake.yaml   # Camel YAML dsl wiring the MCP processors
```

### Editing `methods.yaml`
- `name`: Unique identifier surfaced to MCP clients (`tools/call` → `params.name`).
- `title`/`description`: Friendly metadata returned by `tools/list`.
- `query`: The SQL executed by the Snowflake endpoint. Use Camel simple placeholders (`:#arg`) to opt into parameter binding.
- `enableParameterBinding`: When `true`, arguments are bound safely instead of concatenated.
- `requiredArguments`: Hard guard enforced before execution.
- `inputSchema`/`outputSchema`: JSON Schema fragments echoed in discovery responses and used for validation.
- `annotations`: Arbitrary map for categorisation/labels.

Add new tools by appending entries to the `methods:` array. Each entry can also include optional keys:

- `connectionOverrides`: Default Snowflake properties (e.g. `warehouse`, `role`) applied if the caller omits them.
- `allowedOverrides`: Restrict which connection fields a caller may change (`["warehouse", "role"]`).
- `outputFormat`: Overrides the component default (`json`, `jsonl`, `table`, `stream`).
- `maxRows`: Cap the number of rows returned for `SELECT` queries.

After editing YAML, rebuild the sample JAR to pick up the changes: `mvn -q -DskipTests package`.

### Verifying tool registration
1. Start the application.
2. Call `tools/list` and confirm your new method appears with the expected metadata.
3. Invoke `tools/call` with sample arguments; if validation fails you’ll get a `-32602` JSON-RPC error citing the missing fields.

### Route overview
The Camel route in `routes/mcp-snowflake.yaml` wires incoming HTTP requests through the MCP processors:

1. `mcpRequestSizeGuard` and `mcpRateLimit` enforce basic protections.
2. `mcpHttpValidator` checks headers and protocol negotiation.
3. `mcpJsonRpcEnvelope` interprets the JSON-RPC structure.
4. `mcpSnowflakeRequest` binds arguments, merges connection overrides, and sends to `snowflake://`.
5. Responses funnel through `mcpSnowflakeResponse`, adding JSON-RPC result envelopes.

Understanding this flow helps when you add custom processors (e.g., auditing) before or after Snowflake execution.

## Cleaning up
Stop the process with Ctrl+C. No persistent state is written by this sample.

---

Refer to `samples/dynamic-query-yaml` for details on setting up Snowflake objects and key-pair auth.

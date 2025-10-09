# Helper aliases for MCP Snowflake sample
# Source this file:  source samples/mcp-snowflake-yaml/shell-aliases.zsh

mcp_sf_run() {
  if [ -z "$SNOWFLAKE_USERNAME" ]; then echo "SNOWFLAKE_USERNAME not set" >&2; return 1; fi
  if [ -z "$SNOWFLAKE_PRIVATE_KEY_FILE" ] && [ -z "$SNOWFLAKE_PRIVATE_KEY" ]; then
    echo "Provide SNOWFLAKE_PRIVATE_KEY_FILE or SNOWFLAKE_PRIVATE_KEY" >&2; return 1;
  fi
  local KEY_ARG
  if [ -n "$SNOWFLAKE_PRIVATE_KEY_FILE" ]; then
    KEY_ARG="-Dsnowflake.privateKeyFile=$SNOWFLAKE_PRIVATE_KEY_FILE"
  else
    # Escape newlines for JVM property passing
    local ESCAPED="${SNOWFLAKE_PRIVATE_KEY//$'\n'/\\n}"
    KEY_ARG="-Dsnowflake.privateKey=$ESCAPED"
  fi
  java \
    -Dsnowflake.account="${SNOWFLAKE_ACCOUNT}" \
    -Dsnowflake.username="${SNOWFLAKE_USERNAME}" \
    ${KEY_ARG} \
    -Dsnowflake.database="${SNOWFLAKE_DATABASE:-YOUR_DB}" \
    -Dsnowflake.schema="${SNOWFLAKE_SCHEMA:-PUBLIC}" \
    -Dsnowflake.warehouse="${SNOWFLAKE_WAREHOUSE:-COMPUTE_WH}" \
    -Dsnowflake.role="${SNOWFLAKE_ROLE:-ACCOUNTADMIN}" \
  -jar samples/mcp-snowflake-yaml/target/mcp-snowflake-yaml-1.2.0.jar "$@"
}

mcp_sf_list_tools() {
  curl -s -X POST http://localhost:8080/mcp \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json, text/event-stream' \
    -H 'MCP-Protocol-Version: 2025-06-18' \
    -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | jq .
}

mcp_sf_call_select() {
  local USER_ID=${1:-7}
  local MIN_DATE=${2:-1970-01-01}
  curl -s -X POST http://localhost:8080/mcp \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json, text/event-stream' \
    -H 'MCP-Protocol-Version: 2025-06-18' \
    -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"selectSample","arguments":{"user_id":'"$USER_ID"',"min_date":"'$MIN_DATE'"}}}' | jq .
}

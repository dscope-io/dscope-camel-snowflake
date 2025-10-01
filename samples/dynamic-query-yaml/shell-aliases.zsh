# Zsh aliases and helper functions for the Camel Snowflake samples
# Source this file in your shell or add to ~/.zshrc:
#   source "$(pwd)/samples/dynamic-query-yaml/shell-aliases.zsh"
# Adjust defaults to your environment as needed.

# --- snowsql PATH bootstrap --------------------------------------------------
# If snowsql isn't on PATH, try to locate common install paths and add them.
if ! command -v snowsql >/dev/null 2>&1; then
  typeset -a _snsql_candidates
  _snsql_candidates=(
    /opt/homebrew/bin/snowsql
    /usr/local/bin/snowsql
    /Applications/SnowSQL.app/Contents/MacOS/snowsql
    "$HOME"/Applications/SnowSQL.app/Contents/MacOS/snowsql
  )
  # Homebrew cask app bundle locations (glob); (N) ignores if no match
  for p in /opt/homebrew/Caskroom/snowflake-snowsql/*/SnowSQL.app/Contents/MacOS/snowsql(N); do
    _snsql_candidates+=("$p")
  done
  for p in "${_snsql_candidates[@]}"; do
    if [[ -x "$p" ]]; then
      local dir="${p:A:h}"
      path=("$dir" $path)
      break
    fi
  done
  if ! command -v snowsql >/dev/null 2>&1; then
    echo "[shell-aliases] snowsql not found on PATH. See README to install or adjust PATH." >&2
  fi
fi

# --- snowsql helper ---
# Usage: snsql            -> opens snowsql with defaults from env
#        snsql DB SCHEMA  -> opens snowsql and sets database/schema context
snsql() {
  local DB="$1"
  local SCHEMA="$2"

  if ! command -v snowsql >/dev/null 2>&1; then
    echo "snowsql not found. Install it first (see README)." >&2
    return 127
  fi

  local ARGS=(
    -a "${SNOWFLAKE_ACCOUNT:?Set SNOWFLAKE_ACCOUNT}"
    -u "${SNOWFLAKE_USERNAME:?Set SNOWFLAKE_USERNAME}"
  )

  # Optional role/warehouse
  [[ -n "${SNOWFLAKE_ROLE}" ]] && ARGS+=( -r "${SNOWFLAKE_ROLE}" )
  [[ -n "${SNOWFLAKE_WAREHOUSE}" ]] && ARGS+=( -w "${SNOWFLAKE_WAREHOUSE}" )

  if [[ -n "$DB" ]]; then
    ARGS+=( -d "$DB" )
  fi
  if [[ -n "$SCHEMA" ]]; then
    ARGS+=( -s "$SCHEMA" )
  fi

  SNOWSQL_PAGER=cat snowsql "${ARGS[@]}"
}

# Quick context wrappers (edit defaults to taste)
alias snsql-public='snsql "${SNOWFLAKE_DATABASE:-YOUR_DB}" "${SNOWFLAKE_SCHEMA:-PUBLIC}"'

# --- Private key helper ---
# Export SNOWFLAKE_PRIVATE_KEY from a PKCS#8 PEM file without putting the key in shell history.
# Usage: sfkey /path/to/private_key_pkcs8.pem
sfkey() {
  local KEYFILE="$1"
  if [[ -z "$KEYFILE" || ! -f "$KEYFILE" ]]; then
    echo "Usage: sfkey /path/to/private_key_pkcs8.pem" >&2
    return 1
  fi
  export SNOWFLAKE_PRIVATE_KEY="$(cat "$KEYFILE")"
  echo "SNOWFLAKE_PRIVATE_KEY exported from: $KEYFILE"
}

# --- Sample helpers ---
# Initialize the sample database objects using the shared SQL script
sf-sample-setup() {
  local SCRIPT_DIR="$(cd "$(dirname "${(%):-%N}")" && pwd)"
  local SQL_FILE="$SCRIPT_DIR/../shared/snowflake/setup.sql"
  if [[ ! -f "$SQL_FILE" ]]; then
    echo "Setup SQL not found: $SQL_FILE" >&2
    return 1
  fi
  # Use env defaults for DB/SCHEMA/WAREHOUSE if set in .env
  snsql "${SNOWFLAKE_DATABASE:-YOUR_DB}" "${SNOWFLAKE_SCHEMA:-PUBLIC}" -o exit_on_error=true -f "$SQL_FILE"
}

# Build and run the dynamic-query-yaml sample
sf-sample-run() {
  local ROOT_DIR="$(cd "$(dirname "${(%):-%N}")"/../.. && pwd)"
  # Build
  (cd "$ROOT_DIR/samples/dynamic-query-yaml" && mvn -q -DskipTests package) || return $?
  # Prepare -D props for username and privateKey from env if available
  local JAVA_PROPS=()
  if [[ -n "$SNOWFLAKE_USERNAME" ]]; then
    JAVA_PROPS+=( -Dsnowflake.username="$SNOWFLAKE_USERNAME" )
  fi
  if [[ -n "$SNOWFLAKE_PRIVATE_KEY" ]]; then
    # Escape newlines for Java system property consumption
    local PK_ESCAPED
    PK_ESCAPED="${SNOWFLAKE_PRIVATE_KEY//$'\n'/\\n}"
    JAVA_PROPS+=( -Dsnowflake.privateKey="$PK_ESCAPED" )
  fi
  if [[ -n "$SNOWFLAKE_PRIVATE_KEY_FILE" ]]; then
    JAVA_PROPS+=( -Dsnowflake.privateKeyFile="$SNOWFLAKE_PRIVATE_KEY_FILE" )
  fi
  # Run
  (cd "$ROOT_DIR/samples/dynamic-query-yaml" \
    && java "${JAVA_PROPS[@]}" -jar target/dynamic-query-yaml-1.0.0-SNAPSHOT.jar)
}

# Run the one-shot runner (RunOnce) which sends a test exchange to direct:snowflakeQuery and exits
sf-sample-run-once() {
  local ROOT_DIR="$(cd "$(dirname "${(%):-%N}")"/../.. && pwd)"
  # Build
  (cd "$ROOT_DIR/samples/dynamic-query-yaml" && mvn -q -DskipTests package) || return $?
  # Prepare -D props for username and privateKey from env if available
  local JAVA_PROPS=()
  if [[ -n "$SNOWFLAKE_USERNAME" ]]; then
    JAVA_PROPS+=( -Dsnowflake.username="$SNOWFLAKE_USERNAME" )
  fi
  if [[ -n "$SNOWFLAKE_PRIVATE_KEY" ]]; then
    local PK_ESCAPED
    PK_ESCAPED="${SNOWFLAKE_PRIVATE_KEY//$'\n'/\\n}"
    JAVA_PROPS+=( -Dsnowflake.privateKey="$PK_ESCAPED" )
  fi
  if [[ -n "$SNOWFLAKE_PRIVATE_KEY_FILE" ]]; then
    JAVA_PROPS+=( -Dsnowflake.privateKeyFile="$SNOWFLAKE_PRIVATE_KEY_FILE" )
  fi
  (cd "$ROOT_DIR/samples/dynamic-query-yaml" \
    && mvn -q -DskipTests ${(j: :)JAVA_PROPS} exec:java -Dexec.mainClass=io.dscope.camel.snowflake.sample.RunOnce)
}

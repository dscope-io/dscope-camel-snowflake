#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}"
ENV_FILE="${SCRIPT_DIR}/.env"

usage() {
  cat <<'EOF'
Usage: ./quickstart.sh [--no-build] [--extra-jvm "-Dkey=value ..."]

Loads Snowflake credentials from .env, builds the sample, and launches the MCP service.

Options:
  --no-build           Skip running Maven (assumes target JAR already exists)
  --extra-jvm ARGS     Additional JVM arguments (quoted string)
  -h, --help           Show this message

Expected .env variables:
  SNOWFLAKE_ACCOUNT            Snowflake account locator or URL friendly name
  SNOWFLAKE_USERNAME           Snowflake user to authenticate as
  SNOWFLAKE_PRIVATE_KEY_FILE   Absolute path to PKCS#8 private key (PEM)
  SNOWFLAKE_DATABASE           Database to use
  SNOWFLAKE_SCHEMA             Schema to use
  SNOWFLAKE_WAREHOUSE          Warehouse to resume/use
  SNOWFLAKE_ROLE               Role to activate

Optional variables:
  SNOWFLAKE_AUTHENTICATOR      Defaults to snowflake_jwt
  SNOWFLAKE_JAVA_OPTS          Extra JVM opts (appended)
  SNOWFLAKE_APP_PORT           MCP HTTP port (defaults to 8080)
EOF
}

run_build=true
extra_jvm=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build)
      run_build=false
      shift
      ;;
    --extra-jvm)
      extra_jvm="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a
  source "$ENV_FILE"
  set +a
else
  echo "Missing .env file at $ENV_FILE" >&2
  exit 1
fi

required_vars=(
  SNOWFLAKE_ACCOUNT
  SNOWFLAKE_USERNAME
  SNOWFLAKE_PRIVATE_KEY_FILE
  SNOWFLAKE_DATABASE
  SNOWFLAKE_SCHEMA
  SNOWFLAKE_WAREHOUSE
  SNOWFLAKE_ROLE
)

for var in "${required_vars[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "Environment variable $var must be set in .env" >&2
    exit 1
  fi
done

if [[ ! -f "$SNOWFLAKE_PRIVATE_KEY_FILE" ]]; then
  echo "Private key file not found: $SNOWFLAKE_PRIVATE_KEY_FILE" >&2
  exit 1
fi

if $run_build; then
  (cd "$PROJECT_ROOT" && mvn -q -DskipTests package)
fi

app_port="${SNOWFLAKE_APP_PORT:-8080}"
authenticator="${SNOWFLAKE_AUTHENTICATOR:-snowflake_jwt}"
jar_path="${PROJECT_ROOT}/target/mcp-snowflake-yaml-1.4.0.jar"
if [[ ! -f "$jar_path" ]]; then
  echo "JAR not found at $jar_path. Run Maven build first or omit --no-build." >&2
  exit 1
fi

echo "Launching MCP Snowflake service on port $app_port"

java ${SNOWFLAKE_JAVA_OPTS:-} ${extra_jvm} \
  -Dmcp.server.port="$app_port" \
  -Dsnowflake.account="$SNOWFLAKE_ACCOUNT" \
  -Dsnowflake.username="$SNOWFLAKE_USERNAME" \
  -Dsnowflake.privateKeyFile="$SNOWFLAKE_PRIVATE_KEY_FILE" \
  -Dsnowflake.database="$SNOWFLAKE_DATABASE" \
  -Dsnowflake.schema="$SNOWFLAKE_SCHEMA" \
  -Dsnowflake.warehouse="$SNOWFLAKE_WAREHOUSE" \
  -Dsnowflake.role="$SNOWFLAKE_ROLE" \
  -Dsnowflake.authenticator="$authenticator" \
  -jar "$jar_path"

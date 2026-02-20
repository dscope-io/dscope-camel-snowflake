#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SAMPLES_DIR="${ROOT_DIR}/samples"
RESULTS_DIR="${SAMPLES_DIR}/acceptance-results"
RUN_ID="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="${RESULTS_DIR}/${RUN_ID}"
MODE="full"
ENV_FILE=""

usage() {
  cat <<'EOF'
Usage: samples/run-acceptance-tests.sh [--build-only] [--full] [--env-file PATH]

Options:
  --build-only   Run build checks only (AT build column)
  --full         Run full acceptance flow (build + runtime assertions) [default]
  --env-file     Load environment variables from file before running checks
  -h, --help     Show this help

Environment (required for --full):
  SNOWFLAKE_ACCOUNT
  SNOWFLAKE_DATABASE
  SNOWFLAKE_SCHEMA
  SNOWFLAKE_WAREHOUSE
  SNOWFLAKE_ROLE
  SNOWFLAKE_USERNAME
  One of:
    SNOWFLAKE_PRIVATE_KEY_FILE
    SNOWFLAKE_PRIVATE_KEY

Optional:
  MVN_BIN        Override Maven binary path
  JAVA_BIN       Override Java binary path (default: java)
  MCP_PORT       MCP sample port (default: 8080)
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build-only)
      MODE="build-only"
      shift
      ;;
    --full)
      MODE="full"
      shift
      ;;
    --env-file)
      if [[ $# -lt 2 ]]; then
        echo "--env-file requires a file path" >&2
        exit 1
      fi
      ENV_FILE="$2"
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

load_env_file() {
  local file_path="$1"
  if [[ ! -f "$file_path" ]]; then
    echo "Environment file not found: ${file_path}" >&2
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "$file_path"
  set +a
}

if [[ -n "${ENV_FILE}" ]]; then
  load_env_file "${ENV_FILE}"
fi

detect_mvn() {
  if [[ -n "${MVN_BIN:-}" && -x "${MVN_BIN}" ]]; then
    echo "${MVN_BIN}"
    return
  fi
  if command -v mvn >/dev/null 2>&1; then
    command -v mvn
    return
  fi
  if [[ -x "${HOME}/.maven/maven-3.9.12/bin/mvn" ]]; then
    echo "${HOME}/.maven/maven-3.9.12/bin/mvn"
    return
  fi
  echo ""
}

MVN="$(detect_mvn)"
JAVA_BIN="${JAVA_BIN:-java}"
MCP_PORT="${MCP_PORT:-8080}"

if [[ -z "${MVN}" ]]; then
  echo "Maven not found. Set MVN_BIN or install Maven." >&2
  exit 1
fi

mkdir -p "${OUT_DIR}"
SUMMARY_FILE="${OUT_DIR}/summary.txt"

declare -a TEST_IDS=()
declare -a TEST_STATUS=()
declare -a SNOWFLAKE_KEY_JVM_ARG=()

require_full_env() {
  local vars=(
    SNOWFLAKE_ACCOUNT
    SNOWFLAKE_DATABASE
    SNOWFLAKE_SCHEMA
    SNOWFLAKE_WAREHOUSE
    SNOWFLAKE_ROLE
    SNOWFLAKE_USERNAME
  )
  for var in "${vars[@]}"; do
    if [[ -z "${!var:-}" ]]; then
      echo "Missing required environment variable for --full: ${var}" >&2
      exit 1
    fi
  done
  if [[ -n "${SNOWFLAKE_PRIVATE_KEY_FILE:-}" ]]; then
    if [[ ! -f "${SNOWFLAKE_PRIVATE_KEY_FILE}" ]]; then
      echo "Private key file does not exist: ${SNOWFLAKE_PRIVATE_KEY_FILE}" >&2
      exit 1
    fi
    SNOWFLAKE_KEY_JVM_ARG=("-Dsnowflake.privateKeyFile=${SNOWFLAKE_PRIVATE_KEY_FILE}")
    return
  fi

  if [[ -n "${SNOWFLAKE_PRIVATE_KEY:-}" ]]; then
    local escaped_key
    escaped_key="${SNOWFLAKE_PRIVATE_KEY//$'\n'/\\n}"
    SNOWFLAKE_KEY_JVM_ARG=("-Dsnowflake.privateKey=${escaped_key}")
    return
  fi

  echo "Missing key material for --full: set SNOWFLAKE_PRIVATE_KEY_FILE or SNOWFLAKE_PRIVATE_KEY" >&2
  exit 1
}

run_step() {
  local test_id="$1"
  local description="$2"
  local logfile="$3"
  shift 3

  TEST_IDS+=("${test_id}")
  echo "=== ${test_id}: ${description} ==="
  echo "Command: $*" > "${logfile}"
  echo "Started: $(date -u +'%Y-%m-%dT%H:%M:%SZ')" >> "${logfile}"

  if "$@" >> "${logfile}" 2>&1; then
    echo "Ended: $(date -u +'%Y-%m-%dT%H:%M:%SZ')" >> "${logfile}"
    echo "Result: PASS" >> "${logfile}"
    TEST_STATUS+=("PASS")
    echo "PASS: ${test_id}"
  else
    echo "Ended: $(date -u +'%Y-%m-%dT%H:%M:%SZ')" >> "${logfile}"
    echo "Result: FAIL" >> "${logfile}"
    TEST_STATUS+=("FAIL")
    echo "FAIL: ${test_id} (see ${logfile})"
  fi
}

build_cmd() {
  local sample_dir="$1"
  "${MVN}" -q -DskipTests -f "${sample_dir}/pom.xml" package
}

run_yaml_once() {
  "${MVN}" -q -DskipTests -f "${SAMPLES_DIR}/dynamic-query-yaml/pom.xml" \
    -Dsnowflake.account="${SNOWFLAKE_ACCOUNT}" \
    -Dsnowflake.database="${SNOWFLAKE_DATABASE}" \
    -Dsnowflake.schema="${SNOWFLAKE_SCHEMA}" \
    -Dsnowflake.warehouse="${SNOWFLAKE_WAREHOUSE}" \
    -Dsnowflake.role="${SNOWFLAKE_ROLE}" \
    -Dsnowflake.username="${SNOWFLAKE_USERNAME}" \
    "${SNOWFLAKE_KEY_JVM_ARG[@]}" \
    exec:java -Dexec.mainClass=io.dscope.camel.snowflake.sample.RunOnce
}

run_java_once() {
  "${MVN}" -q -DskipTests -f "${SAMPLES_DIR}/dynamic-query-java/pom.xml" \
    -Dsnowflake.account="${SNOWFLAKE_ACCOUNT}" \
    -Dsnowflake.database="${SNOWFLAKE_DATABASE}" \
    -Dsnowflake.schema="${SNOWFLAKE_SCHEMA}" \
    -Dsnowflake.warehouse="${SNOWFLAKE_WAREHOUSE}" \
    -Dsnowflake.role="${SNOWFLAKE_ROLE}" \
    -Dsnowflake.username="${SNOWFLAKE_USERNAME}" \
    "${SNOWFLAKE_KEY_JVM_ARG[@]}" \
    exec:java -Dexec.mainClass=io.dscope.camel.snowflake.sample.java.RunOnce
}

run_groovy_once() {
  "${MVN}" -q -DskipTests -f "${SAMPLES_DIR}/dynamic-query-groovy/pom.xml" \
    -Dsnowflake.account="${SNOWFLAKE_ACCOUNT}" \
    -Dsnowflake.database="${SNOWFLAKE_DATABASE}" \
    -Dsnowflake.schema="${SNOWFLAKE_SCHEMA}" \
    -Dsnowflake.warehouse="${SNOWFLAKE_WAREHOUSE}" \
    -Dsnowflake.role="${SNOWFLAKE_ROLE}" \
    -Dsnowflake.username="${SNOWFLAKE_USERNAME}" \
    "${SNOWFLAKE_KEY_JVM_ARG[@]}" \
    -Dsnowflake.authenticator=snowflake_jwt \
    -Dexec.cleanupDaemonThreads=false \
    exec:java@run-once
}

run_kotlin_once() {
  "${MVN}" -q -DskipTests -f "${SAMPLES_DIR}/dynamic-query-kotlin/pom.xml" \
    -Dsnowflake.account="${SNOWFLAKE_ACCOUNT}" \
    -Dsnowflake.database="${SNOWFLAKE_DATABASE}" \
    -Dsnowflake.schema="${SNOWFLAKE_SCHEMA}" \
    -Dsnowflake.warehouse="${SNOWFLAKE_WAREHOUSE}" \
    -Dsnowflake.role="${SNOWFLAKE_ROLE}" \
    -Dsnowflake.username="${SNOWFLAKE_USERNAME}" \
    "${SNOWFLAKE_KEY_JVM_ARG[@]}" \
    -Dsnowflake.authenticator=snowflake_jwt \
    -Dexec.cleanupDaemonThreads=false \
    exec:java@run-once
}

run_spring_boot_once() {
  local spring_key_arg
  if [[ -n "${SNOWFLAKE_PRIVATE_KEY_FILE:-}" ]]; then
    spring_key_arg="-Dsnowflake.privateKeyFile=${SNOWFLAKE_PRIVATE_KEY_FILE}"
  else
    local escaped_key
    escaped_key="${SNOWFLAKE_PRIVATE_KEY//$'\n'/\\n}"
    spring_key_arg="-Dsnowflake.privateKey=${escaped_key}"
  fi

  "${MVN}" -q -f "${SAMPLES_DIR}/spring-boot-snowflake/pom.xml" spring-boot:run \
    "-Dspring-boot.run.jvmArguments=-Dsnowflake.account=${SNOWFLAKE_ACCOUNT} -Dsnowflake.username=${SNOWFLAKE_USERNAME} ${spring_key_arg} -Dsnowflake.database=${SNOWFLAKE_DATABASE} -Dsnowflake.schema=${SNOWFLAKE_SCHEMA} -Dsnowflake.warehouse=${SNOWFLAKE_WAREHOUSE} -Dsnowflake.role=${SNOWFLAKE_ROLE} -Dcamel.main.durationMaxSeconds=20 -Dcamel.main.durationHitExitCode=0"
}

_mcp_server_pid=""

_kill_mcp_server() {
  if [[ -n "${_mcp_server_pid}" ]] && ps -p "${_mcp_server_pid}" >/dev/null 2>&1; then
    kill "${_mcp_server_pid}" >/dev/null 2>&1 || true
    wait "${_mcp_server_pid}" >/dev/null 2>&1 || true
  fi
  _mcp_server_pid=""
}

run_mcp_acceptance() {
  local sample_dir="${SAMPLES_DIR}/mcp-snowflake-yaml"
  local jar="${sample_dir}/target/mcp-snowflake-yaml-1.4.0-shaded.jar"
  local server_log="${OUT_DIR}/AT-06-server.log"

  if [[ ! -f "${jar}" ]]; then
    echo "Missing jar: ${jar}" >&2
    return 1
  fi

  trap '_kill_mcp_server' EXIT

  "${JAVA_BIN}" \
    -Dmcp.server.port="${MCP_PORT}" \
    -Dsnowflake.account="${SNOWFLAKE_ACCOUNT}" \
    -Dsnowflake.username="${SNOWFLAKE_USERNAME}" \
    "${SNOWFLAKE_KEY_JVM_ARG[@]}" \
    -Dsnowflake.database="${SNOWFLAKE_DATABASE}" \
    -Dsnowflake.schema="${SNOWFLAKE_SCHEMA}" \
    -Dsnowflake.warehouse="${SNOWFLAKE_WAREHOUSE}" \
    -Dsnowflake.role="${SNOWFLAKE_ROLE}" \
    -jar "${jar}" > "${server_log}" 2>&1 &
  _mcp_server_pid=$!

  local ready=false
  for _ in {1..30}; do
    if curl -fsS "http://localhost:${MCP_PORT}/mcp/health" >/dev/null 2>&1; then
      ready=true
      break
    fi
    sleep 1
  done

  if [[ "${ready}" != true ]]; then
    echo "MCP health endpoint did not become ready" >&2
    _kill_mcp_server
    trap - EXIT
    return 1
  fi

  local list_response
  list_response="$(curl -fsS -X POST "http://localhost:${MCP_PORT}/mcp" \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json, text/event-stream' \
    -H 'MCP-Protocol-Version: 2025-06-18' \
    -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}')"

  local call_response
  call_response="$(curl -fsS -X POST "http://localhost:${MCP_PORT}/mcp" \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json, text/event-stream' \
    -H 'MCP-Protocol-Version: 2025-06-18' \
    -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"selectSample","arguments":{"user_id":1,"min_date":"1970-01-01"}}}')"

  if [[ "${list_response}" != *"tools"* ]]; then
    echo "tools/list response missing tools payload" >&2
    _kill_mcp_server
    trap - EXIT
    return 1
  fi
  if [[ "${call_response}" != *"result"* ]]; then
    echo "tools/call response missing result payload" >&2
    _kill_mcp_server
    trap - EXIT
    return 1
  fi

  echo "tools/list response:" > "${OUT_DIR}/AT-06-tools-list.json"
  echo "${list_response}" >> "${OUT_DIR}/AT-06-tools-list.json"
  echo "tools/call response:" > "${OUT_DIR}/AT-06-tools-call.json"
  echo "${call_response}" >> "${OUT_DIR}/AT-06-tools-call.json"

  _kill_mcp_server
  trap - EXIT
}

if [[ "${MODE}" == "full" ]]; then
  require_full_env
fi

echo "Acceptance run directory: ${OUT_DIR}"
echo "Mode: ${MODE}"
echo "Maven binary: ${MVN}"

run_step "AT-01-BUILD" "Build dynamic-query-yaml" "${OUT_DIR}/AT-01-build.log" build_cmd "${SAMPLES_DIR}/dynamic-query-yaml"
run_step "AT-02-BUILD" "Build dynamic-query-java" "${OUT_DIR}/AT-02-build.log" build_cmd "${SAMPLES_DIR}/dynamic-query-java"
run_step "AT-03-BUILD" "Build dynamic-query-groovy" "${OUT_DIR}/AT-03-build.log" build_cmd "${SAMPLES_DIR}/dynamic-query-groovy"
run_step "AT-04-BUILD" "Build dynamic-query-kotlin" "${OUT_DIR}/AT-04-build.log" build_cmd "${SAMPLES_DIR}/dynamic-query-kotlin"
run_step "AT-05-BUILD" "Build spring-boot-snowflake" "${OUT_DIR}/AT-05-build.log" build_cmd "${SAMPLES_DIR}/spring-boot-snowflake"
run_step "AT-06-BUILD" "Build mcp-snowflake-yaml" "${OUT_DIR}/AT-06-build.log" build_cmd "${SAMPLES_DIR}/mcp-snowflake-yaml"

if [[ "${MODE}" == "full" ]]; then
  run_step "AT-01-RUN" "Run dynamic-query-yaml one-shot" "${OUT_DIR}/AT-01-run.log" run_yaml_once
  run_step "AT-02-RUN" "Run dynamic-query-java one-shot" "${OUT_DIR}/AT-02-run.log" run_java_once
  run_step "AT-03-RUN" "Run dynamic-query-groovy one-shot" "${OUT_DIR}/AT-03-run.log" run_groovy_once
  run_step "AT-04-RUN" "Run dynamic-query-kotlin one-shot" "${OUT_DIR}/AT-04-run.log" run_kotlin_once
  run_step "AT-05-RUN" "Run spring-boot-snowflake" "${OUT_DIR}/AT-05-run.log" run_spring_boot_once
  run_step "AT-06-RUN" "Run MCP API acceptance checks" "${OUT_DIR}/AT-06-run.log" run_mcp_acceptance
fi

{
  echo "Acceptance Summary"
  echo "Run ID: ${RUN_ID}"
  echo "Mode: ${MODE}"
  echo "Output Directory: ${OUT_DIR}"
  echo
  for i in "${!TEST_IDS[@]}"; do
    printf "%-12s %s\n" "${TEST_IDS[$i]}" "${TEST_STATUS[$i]}"
  done
} | tee "${SUMMARY_FILE}"

if printf '%s\n' "${TEST_STATUS[@]}" | grep -q '^FAIL$'; then
  echo "Acceptance run completed with failures. See ${OUT_DIR}." >&2
  exit 1
fi

echo "Acceptance run completed successfully."

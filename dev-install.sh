#!/usr/bin/env bash
# Dev convenience script: install updated component SNAPSHOT to local repo, then build a chosen sample.
# Usage:
#   ./dev-install.sh                 # builds component + default sample (dynamic-query-yaml)
#   ./dev-install.sh dynamic-query-java   # builds component + specified sample module
#   ./dev-install.sh -U               # force snapshot updates while building default sample
#   ./dev-install.sh -U dynamic-query-kotlin
#
# Flags:
#   -U  Pass through to Maven to force snapshot & plugin updates.
#
set -euo pipefail

FORCE_UPDATE_FLAG=""
SAMPLE_MODULE="dynamic-query-yaml"

args=()
for a in "$@"; do
  case "$a" in
    -U)
      FORCE_UPDATE_FLAG="-U"
      ;;
    dynamic-query-*)
      SAMPLE_MODULE="$a"
      ;;
    *)
      echo "Unknown argument: $a" >&2
      echo "Supported: -U or a sample module name (dynamic-query-yaml|dynamic-query-java|dynamic-query-groovy|dynamic-query-kotlin|spring-boot-snowflake)" >&2
      exit 2
      ;;
  esac
  shift || true
done

echo "== 1/2 Installing component snapshot into local Maven repo =="
# Build & install the root component (skipping tests for speed; remove -DskipTests if you want full test run)
mvn ${FORCE_UPDATE_FLAG} -q -DskipTests install

echo "== 2/2 Building sample module: ${SAMPLE_MODULE} =="
mvn ${FORCE_UPDATE_FLAG} -q -f samples/pom.xml -pl "${SAMPLE_MODULE}" -am package

JAR_PATH="samples/${SAMPLE_MODULE}/target/${SAMPLE_MODULE}-1.0.0-SNAPSHOT.jar"
if [ -f "${JAR_PATH}" ]; then
  echo "Built sample jar: ${JAR_PATH}"
else
  echo "ERROR: Expected jar not found at ${JAR_PATH}" >&2
  exit 3
fi

echo "Verification: ensuring updated SnowflakeProducer is present in local repo jar..."
COMP_JAR="${HOME}/.m2/repository/io/dscope/camel/camel-snowflake/1.0.0-SNAPSHOT/camel-snowflake-1.0.0-SNAPSHOT.jar"
if [ -f "${COMP_JAR}" ]; then
  if unzip -p "${COMP_JAR}" io/dscope/camel/snowflake/SnowflakeProducer.class >/dev/null 2>&1; then
    echo "Component jar present: ${COMP_JAR}";
  else
    echo "WARNING: SnowflakeProducer.class not found inside component jar (unexpected)" >&2
  fi
else
  echo "WARNING: Component jar not found at ${COMP_JAR}" >&2
fi

echo "Done. Run the sample, e.g.:"
echo "  java -Dsnowflake.account=... -jar ${JAR_PATH}"
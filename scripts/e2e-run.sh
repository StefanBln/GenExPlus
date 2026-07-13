#!/bin/bash
# Run GenExPlus E2E scenarios manually (same recipes as mvn -Pe2e test).
# Usage:
#   ./scripts/e2e-run.sh              # all tier-A scenarios
#   ./scripts/e2e-run.sh --list
#   ./scripts/e2e-run.sh 01-pdf-static
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SETTINGS="${MAVEN_SETTINGS:-.mvn/settings-ci.xml}"
SCENARIOS_ROOT="src/test/resources/e2e/scenarios"
OUTPUT_ROOT="e2e-output"
PROPS="src/test/resources/e2e/application.properties"

TIER_A=(
  01-pdf-static
  02-pdf-parameters
  03-xlsx-export
  04-csv-pipe-delimiter
  05-text-export
  06-timestamp-filename
  07-edge-invalid-format
)

list_scenarios() {
  echo "Tier-A scenarios (no external services):"
  for id in "${TIER_A[@]}"; do
    echo "  $id"
  done
  echo ""
  echo "Additional scenarios (run via mvn -Pe2e test):"
  echo "  08-signed-pdf  09-email-delivery  10-edge-email-fail"
  echo "  11-edge-signing-non-pdf  12-db-postgres"
}

resolve_jar() {
  if [ -f "$ROOT/genexplus.jar" ]; then
    echo "$ROOT/genexplus.jar"
    return
  fi
  for candidate in "$ROOT"/target/genexplus-*.jar; do
    if [ -f "$candidate" ]; then
      echo "$candidate"
      return
    fi
  done
  echo ""
}

run_scenario() {
  local id="$1"
  local conf_src="$SCENARIOS_ROOT/$id/report.conf"
  if [ ! -f "$conf_src" ]; then
    echo "ERROR: unknown scenario or missing report.conf: $id"
    exit 1
  fi

  local out_dir="$OUTPUT_ROOT/$id"
  mkdir -p "$out_dir"
  sed "s|__OUTPUT_DIR__|$out_dir|g" "$conf_src" > "$out_dir/report.conf"

  echo "==> $id"
  java -cp "$LIB_DIR/*:$JAR:$TEST_CLASSES:$RESOURCES_DIR" \
    io.github.stefanbln.genexplus.report.Main \
    --config "$out_dir/report.conf" \
    --properties "$PROPS"
  echo "    output: $out_dir"
}

if [ "${1:-}" = "--list" ] || [ "${1:-}" = "-l" ]; then
  list_scenarios
  exit 0
fi

MVN_ARGS=(-B -q -DskipTests package test-compile)
if [ -f "$SETTINGS" ]; then
  MVN_ARGS+=(--settings "$SETTINGS")
fi
echo "Building GenExPlus (package + test-compile for scenario templates)..."
mvn "${MVN_ARGS[@]}"

JAR="$(resolve_jar)"
LIB_DIR="$ROOT/target/lib"
TEST_CLASSES="$ROOT/target/test-classes"
RESOURCES_DIR="$ROOT/target/additional_resources"

if [ -z "$JAR" ] || [ ! -d "$LIB_DIR" ]; then
  echo "ERROR: packaged layout missing. Run mvn package first."
  exit 1
fi

if [ ! -f "$PROPS" ]; then
  echo "ERROR: missing $PROPS"
  exit 1
fi

if [ $# -eq 0 ]; then
  for id in "${TIER_A[@]}"; do
    run_scenario "$id"
  done
  echo "All tier-A scenarios completed. See $OUTPUT_ROOT/"
else
  run_scenario "$1"
fi

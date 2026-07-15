#!/bin/bash
# Copyright 2026 Stefan Schuetz - Locivera - Berlin
# SPDX-License-Identifier: Apache-2.0

# Deploy to a local test directory (no sudo). Usage: ./scripts/deploy-test.sh [target-dir]

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="${1:-/tmp/genexplus-test}"

cd "$ROOT"

echo "Building GenExPlus..."
mvn -q -DskipTests package

JAR_FILE="$(ls target/genexplus-*.jar | head -1)"
if [ -z "$JAR_FILE" ]; then
  echo "Error: no JAR found in target/."
  exit 1
fi

rm -rf "$TARGET_DIR"
mkdir -p "$TARGET_DIR"

cp "$JAR_FILE" "$TARGET_DIR/genexplus.jar"
cp -r target/lib "$TARGET_DIR/"
cp -r target/additional_resources "$TARGET_DIR/"
cp start.sh "$TARGET_DIR/"
chmod +x "$TARGET_DIR/start.sh"

echo "=== Test deployment ready ==="
echo "Run: $TARGET_DIR/start.sh $ROOT/report.conf.example"

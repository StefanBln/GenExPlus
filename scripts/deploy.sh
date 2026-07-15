#!/bin/bash
# Copyright 2026 Stefan Schuetz - Locivera - Berlin
# SPDX-License-Identifier: Apache-2.0

# Deploy packaged GenExPlus artifacts to a target directory.
# Usage: ./scripts/deploy.sh [target-dir]
#
# Requires: mvn package (run automatically if target/ is missing or stale).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="${1:-/opt/genexplus}"

cd "$ROOT"

JAR_FILE="$(ls target/genexplus-*.jar 2>/dev/null | head -1 || true)"
if [ -z "$JAR_FILE" ]; then
  echo "Building GenExPlus..."
  mvn -q -DskipTests package
  JAR_FILE="$(ls target/genexplus-*.jar | head -1)"
fi

echo "=== GenExPlus deployment ==="
echo "Source:  $ROOT"
echo "Target:  $TARGET_DIR"
echo "JAR:     $JAR_FILE"

sudo mkdir -p "$TARGET_DIR"
sudo cp "$JAR_FILE" "$TARGET_DIR/genexplus.jar"
sudo cp -r target/lib "$TARGET_DIR/"
sudo cp -r target/additional_resources "$TARGET_DIR/"
sudo cp start.sh "$TARGET_DIR/"
sudo chmod +x "$TARGET_DIR/start.sh"

echo "=== Deployment complete ==="
echo "Run: cd $TARGET_DIR && ./start.sh /path/to/report.conf"

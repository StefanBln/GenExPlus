#!/bin/bash
# Copyright 2026 Stefan Schuetz - Locivera - Berlin
# SPDX-License-Identifier: Apache-2.0

# Verifies the packaged GenExPlus layout (JAR + lib/ + additional_resources/) end-to-end.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SETTINGS="${MAVEN_SETTINGS:-.mvn/settings-ci.xml}"
MVN_ARGS=(-B package -DskipTests)
if [ -f "$SETTINGS" ]; then
    MVN_ARGS+=(--settings "$SETTINGS")
fi

echo "Building GenExPlus..."
mvn -q "${MVN_ARGS[@]}"

echo "Running packaged smoke test..."
rm -rf genexplus-output
./start.sh report.conf.example

OUTPUT="genexplus-output/sample-report.pdf"
if [ ! -f "$OUTPUT" ]; then
    echo "ERROR: expected output file not found: $OUTPUT"
    exit 1
fi

if ! head -c 4 "$OUTPUT" | grep -q '%PDF'; then
    echo "ERROR: output file is not a valid PDF: $OUTPUT"
    exit 1
fi

echo "Packaged smoke test passed: $OUTPUT"

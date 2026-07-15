#!/bin/bash
# Copyright 2026 Stefan Schuetz - Locivera - Berlin
# SPDX-License-Identifier: Apache-2.0

# Stop local test databases started by test-db-up.sh.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.test.yml}"

docker compose -f "$COMPOSE_FILE" down

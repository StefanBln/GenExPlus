#!/bin/bash
# Copyright 2026 Stefan Schuetz - Locivera - Berlin
# SPDX-License-Identifier: Apache-2.0

# Start PostgreSQL + MySQL for local integration/E2E database tests.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.test.yml}"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker is not installed or not on PATH"
  exit 1
fi

echo "Starting test databases (postgres:${POSTGRES_TEST_PORT:-5433}, mysql:3306)..."
docker compose -f "$COMPOSE_FILE" up -d --wait

cat <<'EOF'

Test databases are ready. Export these (or add to your shell profile for the session):

export REPORT_DB1_URL=jdbc:postgresql://localhost:${POSTGRES_TEST_PORT:-5433}/postgres
export REPORT_DB1_USERNAME=postgres
export REPORT_DB1_PASSWORD=postgres
export REPORT_DB1_DRIVER=org.postgresql.Driver

export REPORT_DB2_URL=jdbc:mysql://localhost:3306/genexplus_test
export REPORT_DB2_USERNAME=genexplus
export REPORT_DB2_PASSWORD=genexplus
export REPORT_DB2_DRIVER=com.mysql.cj.jdbc.Driver

Run database tests:
  mvn -Pintegration-tests test -Dtest=RendererTest#testRenderWithTestTemplate
  mvn -Pe2e test -Dgroups=e2e-db

Stop databases:
  ./scripts/test-db-down.sh
EOF

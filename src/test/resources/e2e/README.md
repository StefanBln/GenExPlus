# GenExPlus E2E scenarios

Runnable recipes that double as **integration tests** and **copy-paste quickstarts**.

Each folder under `scenarios/` contains a `report.conf` you can run as-is after substituting the output directory (tests do this automatically).

## Run as tests

```bash
mvn -Pe2e test                              # all e2e scenarios
mvn -Pe2e test -Dtest=E2eScenariosTest      # tier-A CLI scenarios only
```

**Generated reports persist** under `target/e2e-reports/<scenario-id>/` after tests complete.
Browse PDFs, XLSX, CSV, and materialized `report.conf` files there. Override the root with
`-Dgenexplus.e2e.outputDir=/path/to/dir`.

Requires Docker test databases for `@Tag("e2e-db")` scenarios (`./scripts/test-db-up.sh`).

```bash
./scripts/test-db-up.sh
mvn -Pe2e test -Dgroups=e2e-db
```

## Run manually (demo / quickstart)

```bash
mvn package -DskipTests
./scripts/e2e-run.sh                        # all tier-A scenarios
./scripts/e2e-run.sh 01-pdf-static          # one scenario
./scripts/e2e-run.sh --list
```

Outputs land in `e2e-output/<scenario-id>/` (shell script) or `target/e2e-reports/<scenario-id>/` (Maven tests).

## Copy as quickstart

```bash
cp -r src/test/resources/e2e/scenarios/01-pdf-static ./my-first-report
# Edit report.conf, then:
./start.sh my-first-report/report.conf
```

## Scenario index

| ID | Folder | Proves |
|----|--------|--------|
| 01 | `01-pdf-static` | Minimal DB-less PDF (`simple.jrxml`) |
| 02 | `02-pdf-parameters` | Jasper parameters on classpath template |
| 03 | `03-xlsx-export` | XLSX export |
| 04 | `04-csv-pipe-delimiter` | Custom CSV delimiter (`sample-report.jrxml`) |
| 05 | `05-text-export` | TEXT layout export |
| 06 | `06-timestamp-filename` | Auto timestamp in output filename |
| 07 | `07-edge-invalid-format` | Validation failure (exit 1) |
| 08 | `08-signed-pdf` | PDF CMS signing (PKCS#7 detached + DocMDP certification) |
| 09 | `09-email-delivery` | Full CLI email + GreenMail |
| 10 | `10-edge-email-fail` | Email failure exit 5, file on disk |
| 11 | `11-edge-signing-non-pdf` | Signing enabled + XLSX → validation error |
| 12 | `12-db-postgres` | PostgreSQL JDBC fill (`database.id=db1`) |
| 13 | `13-db-mysql` | MySQL JDBC fill (`database.id=db2`) |
| 14 | `14-db-missing-config` | Unknown `database.id` → exit 1 |
| 15 | `15-db-sql-without-id` | SQL template with no profile name → exit 1 |
| 16 | `16-db-adapter-mismatch` | Studio adapter ≠ `database.id` → warning |
| 17 | `17-db-inferred-from-template` | Omits `database.id`; infers from JRXML |
| 18 | `18-db-jrdax-merge` | Classpath `db1.jrdax` + `db1.*` property override |

Placeholders in `report.conf`:

- `__OUTPUT_DIR__` — replaced by the test runner or `e2e-run.sh` with a writable directory.

Shared JDBC profiles for DB scenarios: `e2e/databases.properties` (overridden by `REPORT_DB1_*` / `REPORT_DB2_*` env vars).
Shared SMTP/signing defaults: `e2e/application.properties` in this folder (tests may override ports dynamically).

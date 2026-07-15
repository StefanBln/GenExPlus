# GenExPlus Documentation

GenExPlus is a batch-oriented JasperReports CLI: one invocation loads configuration, renders a template, writes an output file, and optionally emails the result. There is no long-running server.

Use this index to find what you need.

## Start here

| Document | Audience | Contents |
|----------|----------|----------|
| [README](../README.md) | Everyone | Quick start, build, run, links into this folder |
| [E2E scenarios](../src/test/resources/e2e/README.md) | Everyone | Runnable CLI recipes that double as quickstarts |
| [HOWTO.md](HOWTO.md) | Operators | Step-by-step guides for common tasks |
| [DATABASE.md](DATABASE.md) | Operators & report authors | JDBC profiles, Studio alignment, multi-DB |
| [FAQ.md](FAQ.md) | Operators & integrators | Answers to frequent questions |
| [CONFIGURATION.md](CONFIGURATION.md) | Operators | Complete property reference for `.conf` and `application.properties` |
| [PDF_SIGNING.md](PDF_SIGNING.md) | Operators & security | Cryptographic PDF signing, DocMDP certification, keystores, trust |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Contributors | PR workflow, tests, verified commits |
| [SECURITY.md](../SECURITY.md) | Security | Private vulnerability reporting |
| [CODE_OF_CONDUCT.md](../CODE_OF_CONDUCT.md) | Everyone | Community standards |

## How-to guides (quick links)

1. [Generate your first report](HOWTO.md#1-generate-your-first-report)
2. [Connect a database](HOWTO.md#2-connect-a-database) — see also [DATABASE.md](DATABASE.md) for Studio alignment and multi-DB
3. [Deliver reports by email](HOWTO.md#3-deliver-reports-by-email)
4. [Sign PDF output](HOWTO.md#4-sign-pdf-output) — see [PDF_SIGNING.md](PDF_SIGNING.md) for certification, keystores, and viewer trust
5. [Tune CSV and TEXT export](HOWTO.md#5-tune-csv-and-text-export)
6. [Schedule with cron](HOWTO.md#6-schedule-with-cron)
7. [Run in Docker](HOWTO.md#7-run-in-docker)
8. [Override secrets with environment variables](HOWTO.md#8-override-secrets-with-environment-variables)

## API documentation (JavaDoc)

Public types live under `io.github.stefanbln.genexplus.report`. Generate HTML reference from the project root:

```bash
mvn javadoc:javadoc
open target/site/apidocs/index.html
```

Package overview pages (`package-info.java`) describe each layer:

| Package | Responsibility |
|---------|----------------|
| `io.github.stefanbln.genexplus.report` | CLI entry, orchestration, rendering |
| `io.github.stefanbln.genexplus.report.config` | `application.properties` and `.conf` loading |
| `io.github.stefanbln.genexplus.report.delivery` | SMTP email with attachments |
| `io.github.stefanbln.genexplus.report.signing` | Post-export PDF CMS signatures |
| `io.github.stefanbln.genexplus.report.rendering.exporters` | PDF, Excel, CSV, TEXT export |
| `io.github.stefanbln.genexplus.report.rendering.datasources` | JDBC connections for fill phase |

## Architecture at a glance

```
report.conf  ──┐
               ├──► ReportExecutor ──► Renderer ──► Exporter ──► output file
application.properties ──┘              │                    │
                                        │                    └──► optional PDF signing
                                        └──► DataSourceProvider (JDBC)
                                             EmailDeliveryService (optional)
```

Each run is independent. Configuration merges from file, JVM system properties, and `REPORT_*` environment variables (see [CONFIGURATION.md](CONFIGURATION.md)).

## Getting help

1. Check [FAQ.md](FAQ.md) for your symptom or question.
2. Read the [troubleshooting table](../README.md#troubleshooting) in the README.
3. Run with `verbose=true` in the report job file or `--verbose` on the CLI for fine-grained logs.
4. Inspect exit codes in [CONFIGURATION.md](CONFIGURATION.md#exit-codes).

---

*Last updated: July 2026*

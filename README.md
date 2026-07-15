<div align="center">

# GenExPlus

**Generic Export Plus** — batch JasperReports from the command line.

*Design your reports in Jasper Studio. Run them anywhere with a single config file — no magic wands required.*

<br>

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![JasperReports](https://img.shields.io/badge/JasperReports-7.0-2ea44f.svg)](https://community.jaspersoft.com/)

</div>

---

## What is GenExPlus?

**GenExPlus** (short for **Generic Export Plus**) is a focused CLI that turns JasperReports templates into finished exports — PDF, Excel, CSV, or plain text — then quietly exits with a code your scheduler understands. *(Spoiler: exit code 0 means you can go back to sleep.)*

No web server. No report designer. No long-running process. Just: load a job, render, write the file, optionally sign it or send it by email, and move on.

```
  .jrxml template  ──┐
  report.conf        ├──►  GenExPlus  ──►  your-report.pdf
  application.properties ┘         └──►  (optional) inbox · signature
```

Perfect for the 3 AM sales summary, the warehouse snapshot before breakfast, the compliance PDF legal keeps asking for — anything that belongs on a cron tab rather than in a browser tab.

---

## Why teams reach for GenExPlus

| | |
|---|---|
| **One job, one file** | Each report gets its own `report.conf` — template, format, parameters, output path. Shared settings (databases, SMTP, signing) live in one `application.properties` per environment. |
| **Scheduler-friendly** | Distinct exit codes for validation, database, render, and email failures. Your automation knows exactly what went wrong. |
| **Production-minded** | Atomic file writes, credential redaction in logs, page and time governors for runaway reports. |
| **Delivery when you need it** | SMTP with retries, attachment limits, HTML and plain-text bodies. Or skip email entirely and write to disk. |
| **Trust on paper** | Optional cryptographic PDF signing with visible stamps and document certification — see [PDF signing guide](docs/PDF_SIGNING.md). |

---

## Quick start

No database required. Coffee optional. Under a minute from clone to PDF:

```bash
git clone https://github.com/StefanBln/GenExPlus.git && cd GenExPlus
mvn package -q
./start.sh report.conf.example
open genexplus-output/sample-report.pdf   # macOS — or xdg-open / start on Linux/Windows
```

`start.sh` wires up the built JAR, runtime libraries, and bundled sample resources automatically.

---

## How it fits together

```
  ┌─────────────────────── YOUR INPUTS ───────────────────────┐
  │                                                           │
  │   Jasper template (.jrxml)                                │
  │   report.conf              ← this job                     │
  │   application.properties   ← shared settings (DB, mail)   │
  │                                                           │
  └─────────────────────────────┬─────────────────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │    GenExPlus    │
                       │       CLI       │
                       └────────┬────────┘
                                │
            ┌───────────────────┼───────────────────┐
            │                   │                   │
            ▼                   ▼                   ▼
   ┌────────────────┐  ┌───────────────┐  ┌───────────────┐
   │  PDF · XLSX    │  │     Email     │  │  Signed PDF   │
   │  XLS · CSV     │  │  (optional)   │  │  (optional)   │
   │  · TEXT        │  │               │  │               │
   └────────────────┘  └───────────────┘  └───────────────┘
```

**Two files, one run.** The job file describes *what* to export; the properties file describes *where* it connects (JDBC, SMTP, keystores). Secrets can be injected via environment variables — `REPORT_DB1_PASSWORD`, `REPORT_MAIL_PASSWORD`, `REPORT_KEYSTORE_PASSWORD` — so nothing sensitive needs to sit in plain text on disk.

Walkthrough: **[How-To guides](docs/HOWTO.md)** · Every property: **[Configuration reference](docs/CONFIGURATION.md)** · Sample job: **[report.conf.example](report.conf.example)**

---

## Capabilities at a glance

- **Formats** — PDF, Excel (XLSX/XLS), CSV with configurable delimiter, TEXT with layout tuning
- **Data** — JDBC against PostgreSQL out of the box; Oracle and DB2 via optional Maven profiles
- **Email** — multipart HTML + plain text, inline logo support, size limits and retry
- **Signing** — detached PKCS#7 CMS signatures, DocMDP certification, optional on-screen stamp
- **Safety nets** — global and per-job page/time limits; virtualizer support for large fills

---

## Documentation

| Guide | For |
|-------|-----|
| **[Documentation hub](docs/README.md)** | Overview and architecture |
| **[How-To guides](docs/HOWTO.md)** | First report, database, email, signing, cron, Docker |
| **[Database guide](docs/DATABASE.md)** | Studio adapters, `database.id`, multi-DB setups |
| **[PDF signing](docs/PDF_SIGNING.md)** | Keystores, certification, viewer trust |
| **[FAQ](docs/FAQ.md)** | Common questions and troubleshooting |
| **[Configuration](docs/CONFIGURATION.md)** | Complete property reference and exit codes |
| **[E2E scenarios](src/test/resources/e2e/README.md)** | Runnable recipes you can copy |
| **[Contributing](CONTRIBUTING.md)** | PR workflow, tests, verified commits |
| **[Security](SECURITY.md)** | Report vulnerabilities privately |

Generate Java API docs: `mvn javadoc:javadoc` → `target/site/apidocs/`

---

## Requirements

- **Java 21+**
- **Maven 3.9+** to build from source
- **JDBC database** only when your template queries one

---

## Build & run

```bash
mvn clean package
./start.sh report.conf.example
```

Optional JDBC drivers at build time: `mvn package -Poracle-jdbc` or `-Pdb2-jdbc`.

<details>
<summary><strong>Docker</strong></summary>

```bash
mvn -DskipTests package && docker build -t genexplus:local .
docker run --rm genexplus:local \
  --config /path/to/report.conf \
  --properties /path/to/application.properties
```

See **[How-To § Docker](docs/HOWTO.md#7-run-in-docker)** for details.

</details>

<details>
<summary><strong>Tests & CI</strong></summary>

```bash
mvn test                         # unit tests
mvn -Pintegration-tests test     # renderer integration
mvn -Pe2e test                   # end-to-end CLI scenarios
./scripts/e2e-run.sh               # same scenarios, artifacts in target/e2e-reports/
```

GitHub Actions runs unit tests, integration tests, optional JDBC compile checks, and smoke packaging on every push.

</details>

---

## Something went wrong?

It happens — even to cron jobs. Run with `verbose=true` in your job file or `--verbose` on the CLI for detailed logs. The **[FAQ](docs/FAQ.md)** covers the usual suspects — missing template, database not configured, SMTP failures, classpath layout. Exit codes are documented in **[CONFIGURATION.md](docs/CONFIGURATION.md#exit-codes)**.

---

## Get in touch & contribute

GenExPlus is open source and open to collaboration. Whether you spotted a bug, have an idea for a new export knob, or want to improve the docs — you're welcome here.

| Channel | Use it for |
|---------|------------|
| **[GitHub Issues](https://github.com/StefanBln/GenExPlus/issues)** | Bug reports, feature ideas, questions |
| **[Pull requests](https://github.com/StefanBln/GenExPlus/pulls)** | Code fixes, new features, doc improvements |
| **[@StefanBln](https://github.com/StefanBln)** | Maintainer profile — feel free to reach out via GitHub |

**Before you open an issue:** check the [FAQ](docs/FAQ.md) — your answer might already be there.

**Before you open a PR:** `mvn test` should pass; for larger changes, an issue first helps align on direction.

Not sure where to start? Browse [good first issues](https://github.com/StefanBln/GenExPlus/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) or pick a scenario from the [E2E recipes](src/test/resources/e2e/README.md) and see if you can make it better. All skill levels welcome — thoughtful feedback counts as much as code. See **[CONTRIBUTING.md](CONTRIBUTING.md)** for setup, tests, and signed commits.

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).

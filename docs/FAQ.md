# Frequently Asked Questions

Answers to common questions about installing, configuring, and operating GenExPlus.

---

## General

### What is GenExPlus?

GenExPlus is a command-line tool that turns JasperReports templates (`.jrxml` or precompiled `.jasper`) into export files — PDF, Excel, CSV, or plain text. You describe each job in a `.conf` file; shared settings such as database URLs and SMTP live in `application.properties`.

Each invocation is a single batch run: load config, render, write output, optionally email, exit. There is no daemon or web UI.

### Do I need a database?

Only if your template queries JDBC data. Static or parameter-only reports can run with `report.database.optional=true` and no `database.id`. The bundled [report.conf.example](../report.conf.example) works without any database.

### Which Java version do I need?

Java **21 or later**. The project targets `--release 21`. GenExPlus configures headless AWT defaults at startup so JasperReports runs reliably on server JDKs (including JDK 26).

### Can I run GenExPlus without Maven?

Yes, after a build or release:

- Use `./start.sh` with the packaged layout (`target/genexplus-*.jar`, `target/lib/`, `target/additional_resources/`), or
- Download a release zip (JAR + `lib/`) from a tagged build, or
- Run the [Docker image](HOWTO.md#7-run-in-docker).

You need Maven only to build from source.

---

## Installation and build

### Why does `java -jar genexplus.jar` fail with ClassNotFoundException?

The application JAR is **thin**: dependencies sit in a sibling `lib/` directory and are referenced from the manifest `Class-Path`. Either:

```bash
./start.sh report.conf.example
```

or use an explicit classpath (see [README](../README.md#run)).

### What does `mvn package` produce?

| Output | Purpose |
|--------|---------|
| `target/genexplus-1.0.0-SNAPSHOT.jar` | Main application JAR |
| `target/lib/` | Runtime dependencies (JasperReports, JDBC, mail, PDFBox, …) |
| `target/additional_resources/` | Default `application.properties`, sample template, assets |

`start.sh` discovers these paths automatically after a local build.

### How do I add Oracle or DB2 JDBC drivers?

Use Maven profiles at package time:

```bash
mvn package -Poracle-jdbc   # Oracle ojdbc11
mvn package -Pdb2-jdbc      # IBM DB2 jcc
```

Drivers are copied into `target/lib/` like other runtime dependencies.

---

**Optional second database:** Add `db2.url`, `db2.username`, … and set `database.id=db2` in the job that needs it. Copy-paste pack: [`examples/databases/`](../examples/databases/README.md).

---

## Databases & JasperReports Studio

### Does GenExPlus read Jasper data adapters?

**Yes — JDBC `.jrdax` files**, with a clear merge model:

- Profile **name** from `database.id` or JRXML `defaultdataadapter`
- JDBC **settings** from `dbN.*` in `application.properties`, with optional `.jrdax` defaults

Studio JSON/Excel/HTTP adapters are not supported. See [DATABASE.md](DATABASE.md).

### Why does Studio preview work but the CLI fails?

Studio uses its own data adapter settings (host, user, password) configured in the IDE. The CLI reads `application.properties`. Configure `db1.*` (or whichever `database.id` you use) for the deployment environment.

### Can I use `db1.user` instead of `db1.username`?

Prefer `db1.username`. Recent versions accept `db1.user` as an alias, but `db1.username` is the documented key.

### How do I run two databases in one deployment?

Define `db1.*` and `db2.*` in one `application.properties`, then set `database.id=db1` or `database.id=db2` per job file. Example: [examples/databases/](../examples/databases/README.md).

---

## Configuration

### What is the difference between `report.conf` and `application.properties`?

| File | Scope | Typical contents |
|------|--------|------------------|
| **report.conf** | One report job | Template path, output path, format, parameters, per-job email |
| **application.properties** | Whole deployment | Database pools, SMTP, signing keystore, global defaults |

Think of `application.properties` as infrastructure and `report.conf` as the job definition cron or your scheduler passes each run.

### Can I omit `database.id`?

Yes, when the JRXML declares `defaultdataadapter=dbN` and JDBC settings are available via `dbN.*` or `dbN.jrdax`. For DB-less templates, set `report.database.optional=true`. SQL templates always need a resolvable profile — optional mode does not bypass that.

### How do environment variables override properties?

Any property key can be overridden with `REPORT_` + the key in uppercase, dots replaced by underscores:

| Property | Environment variable |
|----------|----------------------|
| `db1.password` | `REPORT_DB1_PASSWORD` |
| `mail.smtp.password` | `REPORT_MAIL_PASSWORD` |
| `signing.keystore.password` | `REPORT_KEYSTORE_PASSWORD` |

See [CONFIGURATION.md](CONFIGURATION.md) for the full merge order.

### Where do templates load from?

In order:

1. **Classpath** — e.g. `sample-report.jrxml` from `additional_resources/`
2. **Filesystem** — absolute or normalized relative path

`.jrxml` files are compiled on every run (no compile cache). `.jasper` files are loaded as precompiled binaries.

---

## Rendering and export

### Which export formats are supported?

`PDF`, `XLSX`, `XLS`, `CSV`, and `TEXT`. Format names are case-insensitive. Default format is `PDF`, or `report.default.format` from `application.properties` when `report.format` is omitted in the job file.

### How do I change the CSV delimiter?

Set `report.csv.delimiter` in the job file or globally in `application.properties`. Default is semicolon (`;`). Single-character delimiters including tab work; blank values fall back to the default.

### Why is my TEXT export layout wrong?

TEXT export uses fixed character grid dimensions. Tune:

- `report.text.pageWidthChars` / `report.text.pageHeightChars`
- `report.text.charWidth` / `report.text.charHeight`

Per-job values in `.conf` override application defaults. See [HOWTO.md §5](HOWTO.md#5-tune-csv-and-text-export).

### What does the report governor do?

JasperReports governor limits protect against runaway reports:

- `report.governor.maxPages` — stop after N pages
- `report.governor.timeoutSeconds` — stop after N seconds

Set globally or per job. GenExPlus restores previous JVM governor properties after each render so concurrent runs do not leak settings.

### What is the virtualizer for?

Large fills can exhaust heap. When `report.virtualizer.enabled=true`, JasperReports spills page data to disk during fill. Configure `report.virtualizer.maxPages` and optionally `report.virtualizer.directory`.

---

## Email

### When does email failure fail the whole run?

By default, if `report.email.enabled=true` and SMTP delivery fails after retries, the process exits **5** even though the report file was written. Set `GENEXPLUS_STRICT_EMAIL=false` for legacy behaviour (exit 0 when only email fails).

### How do I embed a logo in HTML email?

1. Set `mail.smtp.logo.resource` to a classpath PNG path in `application.properties`.
2. Reference it in HTML body: `<img src="cid:logoGenExPlusReport">`.

GenExPlus embeds the image with Content-ID `logoGenExPlusReport`. See [HOWTO.md §3](HOWTO.md#3-deliver-reports-by-email).

### How do retries and attachment limits work?

| Property | Default | Meaning |
|----------|---------|---------|
| `mail.smtp.retry.count` | 2 | Retries after first failure |
| `mail.smtp.retry.delayMs` | 2000 | Base delay; doubled each retry |
| `mail.smtp.maxAttachmentBytes` | 25 MiB | Reject oversized attachments |

Use SMTPS (`mail.smtp.ssl.enable=true`) for port 465; STARTTLS is disabled automatically when SSL is on.

---

## PDF signing

### When is signing applied?

After PDF export, when `signing.enabled=true` and `report.format=PDF`. GenExPlus embeds a **detached PKCS#7 CMS signature** (cryptographic, not a decorative stamp). By default it also **certifies** the document with DocMDP (`signing.certify=true`, `signing.permissions=1`) so compliant viewers restrict editing.

Full guide: **[PDF_SIGNING.md](PDF_SIGNING.md)** (keystores, certificate requirements, trust, PDF Inspector, troubleshooting).

Signing non-PDF formats is rejected at validation time.

### Why does PDF Inspector show “no security”?

Common causes: approval-only signing (`signing.certify=false`), an untrusted self-signed certificate (signature may still be present), or a file not produced by the current pipeline. Certified GenExPlus PDFs include `/Perms` / `/DocMDP`. See [PDF_SIGNING.md § What PDF Inspector should show](PDF_SIGNING.md#what-pdf-inspector-should-show).

### Where should I store the keystore password?

Prefer `REPORT_KEYSTORE_PASSWORD` over a plain property in the file. Never commit keystores or passwords to version control.

---

## Operations and scheduling

### What do exit codes mean for cron?

| Code | Meaning | Report on disk? |
|------|---------|-----------------|
| 0 | Success | Yes |
| 1 | Validation (bad conf, template, args) | Usually no |
| 2 | Missing/unreadable `application.properties` | No |
| 3 | Database error | Usually no |
| 4 | Jasper compile/fill/export error | Usually no |
| 5 | Email failed | **Yes** |

Branch in scripts: `if [ $? -eq 5 ]; then ... notify ops ... fi`.

### Can I run multiple reports in parallel?

Yes. Each process is independent. Avoid sharing the same output filename without timestamps. JDBC uses direct `DriverManager` connections (no pool) — one connection per process.

### Does GenExPlus work in Docker and CI?

Yes. The [Dockerfile](../Dockerfile) expects JAR + `lib/`. GitHub Actions runs unit tests, integration tests (PostgreSQL), and a packaged smoke test on every push. Use `mvn test --settings .mvn/settings-ci.xml` if you mirror CI locally.

---

## Troubleshooting

### `properties file not found` (exit 2)

Run `mvn package` first, or pass `--properties target/additional_resources/application.properties`. `start.sh` resolves defaults after a build.

### `Template file not found` (exit 1)

Check `report.template` — classpath name vs filesystem path. After packaging, `sample-report.jrxml` lives under `additional_resources/`.

### `Compilation failed` (exit 4)

Fix `.jrxml` syntax. stderr includes a summarized first line; run with `--verbose` for full JasperReports stack traces.

### Tests crash with Abort trap on macOS / JDK 26

GenExPlus sets headless defaults in `Main` and Surefire uses `-Djava.awt.headless=true`. If you embed the renderer in another app, call `JasperRuntimeSupport.configureHeadlessDefaults()` before the first render.

### Log4j2 “could not find a logging implementation”

Runtime includes `log4j-core` to pair with `log4j-api` from JasperReports. Rebuild if you see this on an old artifact.

---

## Development

### How do I run integration tests?

PostgreSQL must be reachable (local or CI service):

```bash
export REPORT_DB1_URL=jdbc:postgresql://localhost:5432/postgres
export REPORT_DB1_USERNAME=postgres
export REPORT_DB1_PASSWORD=postgres
mvn -Pintegration-tests test
```


*Did not find your question? Open an issue or extend this file — keep answers factual and link to [CONFIGURATION.md](CONFIGURATION.md) for property details.*

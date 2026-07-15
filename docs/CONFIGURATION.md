# Configuration Reference

GenExPlus reads settings from two files plus runtime overrides. This document lists every supported key.

## Files and precedence

| Source | File / mechanism | Scope |
|--------|------------------|--------|
| Report job | `*.conf` | One scheduled or ad-hoc run |
| Application | `application.properties` | Deployment-wide |
| JVM | `-Dkey=value` | Process-wide override |
| Environment | `REPORT_KEY=value` | Process-wide override |

**Application property merge order:** file → system properties → `REPORT_*` environment variables.

Report job files do not merge with each other; one conf file per invocation.

---

## Report job file (`*.conf`)

Path passed to `--config`. Java properties format (`key=value`).

### Required keys

| Key | Description |
|-----|-------------|
| `report.template` | Classpath or filesystem path to `.jrxml` / `.jasper` |
| `report.output.dir` | Output directory (created if missing) |
| `report.output.filename` | Output filename including extension |

### Database

| Key | Default | Description |
|-----|---------|-------------|
| `database.id` | *(empty)* | References `db1`, `db2`, … in `application.properties` |
| `report.database.optional` | `false` | When `true`, omit `database.id` for DB-less reports. If ID is set, JDBC config must exist |

### Output format

| Key | Default | Description |
|-----|---------|-------------|
| `report.format` | `report.default.format` or `PDF` | `PDF`, `XLSX`, `XLS`, `CSV`, `TEXT` (case-insensitive) |
| `report.csv.delimiter` | `;` (or app default) | CSV field separator; single-char delimiters including tab supported |
| `report.text.pageWidthChars` | `120` | TEXT export width in characters |
| `report.text.pageHeightChars` | `60` | TEXT export height in characters |
| `report.text.charWidth` | `6` | TEXT character width (pixels) |
| `report.text.charHeight` | `12` | TEXT character height (pixels) |

### Parameters

| Key pattern | Description |
|-------------|-------------|
| `report.parameter.<Name>` | JasperReports parameter; type inferred (string, int, double, boolean) |

Example: `report.parameter.ReportTitle=Quarterly Summary`

### Timestamps in filenames

| Key | Default | Description |
|-----|---------|-------------|
| `report.timestamp.auto` | `true` | Append timestamp before extension when `true` |
| `report.timestamp.pattern` | `yyyyMMdd_HHmmss` | `DateTimeFormatter` pattern inside `{[...]}` placeholders |

When auto-timestamp is on, the pattern is applied to the resolved output path.

### Governor (per job)

Overrides application defaults for this run only.

| Key | Description |
|-----|-------------|
| `report.governor.maxPages` | JasperReports max pages (positive integer) |
| `report.governor.timeoutSeconds` | Fill/export timeout in seconds |

### Email (per job)

Requires `mail.smtp.enabled=true` in application properties.

| Key | Default | Description |
|-----|---------|-------------|
| `report.email.enabled` | `false` | Send report after successful write |
| `report.email.to` | — | Comma-separated TO addresses (required when enabled) |
| `report.email.cc` | — | CC addresses |
| `report.email.bcc` | — | BCC addresses |
| `report.email.subject` | `Report: <filename>` | Subject; supports `{[datePattern]}` placeholders |
| `report.email.body` | Plain default text | HTML body; supports placeholders |
| `report.email.body.plain` | *(derived from HTML)* | Plain-text alternative part |
| `report.email.body.escape` | `false` | When `true`, HTML-escape body before send |

### Logging

| Key | Default | Description |
|-----|---------|-------------|
| `verbose` | `false` | Fine-grained Java logging for this job (also `--verbose` on CLI) |

---

## Application properties

Path passed to `--properties`, or classpath default `application.properties`.

### Global report defaults

| Key | Default | Description |
|-----|---------|-------------|
| `report.default.format` | `PDF` | Used when job file omits `report.format` |
| `report.default.locale` | `en_US` | JasperReports locale |
| `report.default.timezone` | `UTC` | JasperReports time zone |
| `report.csv.delimiter` | `;` | Default CSV delimiter |
| `report.text.pageWidthChars` | `120` | Default TEXT width |
| `report.text.pageHeightChars` | `60` | Default TEXT height |
| `report.text.charWidth` | `6` | Default TEXT char width |
| `report.text.charHeight` | `12` | Default TEXT char height |
| `report.governor.maxPages` | `500` | Global max pages |
| `report.governor.timeoutSeconds` | `120` | Global timeout (seconds) |
| `report.db.loginTimeoutSeconds` | `10` | JDBC login timeout (restored after connect) |

### Virtualizer (large reports)

| Key | Default | Description |
|-----|---------|-------------|
| `report.virtualizer.enabled` | `false` | Enable `JRFileVirtualizer` during fill |
| `report.virtualizer.maxPages` | `100` | In-memory page threshold |
| `report.virtualizer.directory` | *(temp)* | Directory for swap files |

### Database blocks

Pattern: `db<N>.<property>` where `<N>` is 1, 2, 3, …

**Canonical guide:** [DATABASE.md](DATABASE.md) — Studio alignment, multi-DB, troubleshooting, Docker testing.

| Key | Description |
|-----|-------------|
| `db1.url` | JDBC URL |
| `db1.username` | Username (optional for some drivers) |
| `db1.password` | Password — prefer `REPORT_DB1_PASSWORD` |
| `db1.driver` | JDBC driver class (loaded before connect) |

Reference in job file: `database.id=db1`.

### SMTP

| Key | Default | Description |
|-----|---------|-------------|
| `mail.smtp.enabled` | `false` | Master switch for email features |
| `mail.smtp.host` | — | SMTP server (required when enabled) |
| `mail.smtp.port` | `587` | SMTP port |
| `mail.smtp.from` | — | From address (required when enabled) |
| `mail.smtp.username` | — | AUTH username |
| `mail.smtp.password` | — | AUTH password — prefer `REPORT_MAIL_PASSWORD` |
| `mail.smtp.auth` | `true` | Enable SMTP AUTH |
| `mail.smtp.starttls.enable` | `true` | STARTTLS (forced off when SSL enabled) |
| `mail.smtp.starttls.required` | `false` | Require STARTTLS |
| `mail.smtp.ssl.enable` | `false` | SMTPS / implicit SSL (port 465) |
| `mail.smtp.ssl.trust` | — | Trust host(s) for SSL/TLS (e.g. `smtp.internal` or `*` for lab use only) |
| `mail.smtp.ssl.checkserveridentity` | `true` | Verify SMTP server hostname against certificate |
| `mail.smtp.retry.count` | `2` | Retries after first failure |
| `mail.smtp.retry.delayMs` | `2000` | Initial retry delay (exponential backoff) |
| `mail.smtp.maxAttachmentBytes` | `26214400` (25 MiB) | Maximum attachment size |
| `mail.smtp.connectiontimeout` | `10000` | Connection timeout (ms) |
| `mail.smtp.timeout` | `10000` | Socket timeout (ms) |
| `mail.smtp.debug` | `false` | Jakarta Mail protocol debug |
| `mail.smtp.logo.resource` | — | Classpath PNG for inline HTML logo (`cid:logoGenExPlusReport`) |

### PDF signing

| Key | Default | Description |
|-----|---------|-------------|
| `signing.enabled` | `false` | Post-export CMS signature |
| `signing.keystore.path` | — | PKCS#12 or JKS keystore path |
| `signing.keystore.alias` | — | Private-key entry alias |
| `signing.keystore.type` | `PKCS12` | Keystore type |
| `signing.keystore.password` | — | Keystore password — prefer `REPORT_KEYSTORE_PASSWORD` |
| `signing.reason` | — | Signature reason (metadata) |
| `signing.location` | — | Signature location (metadata) |
| `signing.contact` | — | Contact info (metadata) |
| `signing.signerName` | — | Display name override (default: certificate CN) |
| `signing.certify` | `true` | DocMDP certification — locks document in compliant viewers |
| `signing.permissions` | `1` | DocMDP level: `1` = no changes, `2` = forms+signing, `3` = +annotations |
| `signing.visible` | `true` | Draw visible signature stamp on the PDF page |
| `signing.visible.page` | `-1` | Page for stamp (`0`-based); `-1` = last page |
| `signing.visible.print` | `false` | Include stamp in printed output (screen-only when false) |

Signing applies only when `report.format=PDF`. See **[PDF_SIGNING.md](PDF_SIGNING.md)** for keystore creation, trust, and troubleshooting.

---

## Environment variables

Convert any property key: `REPORT_` + uppercase with dots → underscores.

| Property | Variable |
|----------|----------|
| `db1.password` | `REPORT_DB1_PASSWORD` |
| `mail.smtp.password` | `REPORT_MAIL_PASSWORD` |
| `signing.keystore.password` | `REPORT_KEYSTORE_PASSWORD` |

### Process environment (non-REPORT)

| Variable | Effect |
|----------|--------|
| `GENEXPLUS_STRICT_EMAIL` | When `false` or `0`, email failure exits **0** if report was written. Default: strict (exit **5**) |

---

## Command-line interface

| Flag | Required | Description |
|------|----------|-------------|
| `--config <path>` | Yes | Report job file |
| `--properties <path>` | No | Application properties; classpath default if omitted |
| `--verbose` | No | Enable fine logging |
| `--version`, `-V` | — | Print version; exit 0 |
| `--help`, `-h` | — | Print usage; exit 0 |

---

## Exit codes

| Code | Constant | When |
|------|----------|------|
| 0 | `SUCCESS` | Report written; email succeeded or disabled |
| 1 | `VALIDATION_ERROR` | CLI, conf validation, bad paths, signing/format mismatch |
| 2 | `CONFIG_ERROR` | Missing/unreadable `application.properties` |
| 3 | `DATABASE_ERROR` | JDBC connect/query failure |
| 4 | `RENDER_ERROR` | Jasper compile/fill/export or signing failure |
| 5 | `EMAIL_ERROR` | Email enabled; send failed; **file on disk** |

Defined in `io.github.stefanbln.genexplus.report.ExitCodes`.

---

## JasperReports runtime defaults

GenExPlus sets these at startup if unset (see `JasperRuntimeSupport`):

| System property | Value |
|-----------------|-------|
| `java.awt.headless` | `true` |
| `net.sf.jasperreports.awt.ignore.missing.font` | `true` |
| `net.sf.jasperreports.default.font.name` | `SansSerif` |
| `net.sf.jasperreports.default.pdf.font.name` | `Helvetica` |

Classpath `jasperreports.properties` mirrors these for test and packaged layouts.

---

## Example files

| File | Purpose |
|------|---------|
| [report.conf.example](../report.conf.example) | Minimal working job (no DB) |
| `target/additional_resources/application.properties` | Defaults after `mvn package` |
| [HOWTO.md](HOWTO.md) | Narrative setup guides |
| [FAQ.md](FAQ.md) | Common questions |

---

*Last updated: July 2026*

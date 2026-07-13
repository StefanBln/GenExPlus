# How-To Guides

Practical walkthroughs for operating GenExPlus. Each section assumes you have [built the project](../README.md#build) unless noted otherwise.

---

## 1. Generate your first report

**Goal:** Produce a sample PDF with no database in under a minute.

**Fastest path — bundled example:**

```bash
mvn package
./start.sh report.conf.example
```

**Copy an E2E quickstart scenario:**

```bash
cp -r src/test/resources/e2e/scenarios/01-pdf-static ./my-first-report
# Edit my-first-report/report.conf (output dir, parameters), then:
mvn package test-compile
./scripts/e2e-run.sh 01-pdf-static
ls e2e-output/01-pdf-static/
```

**Steps (classic quickstart):**

```bash
git clone <repository-url> GenExPlus && cd GenExPlus
mvn package
./start.sh report.conf.example
ls -l genexplus-output/sample-report.pdf
```

**What happens:**

1. Maven builds the thin JAR, copies dependencies to `target/lib/`, and stages `additional_resources/` (properties + template).
2. `start.sh` runs `io.github.stefanbln.genexplus.report.Main` with the example conf file.
3. GenExPlus compiles `sample-report.jrxml`, fills with an empty data source, exports PDF, and writes `genexplus-output/sample-report.pdf`.

**Customize:** Copy `report.conf.example` to `my-report.conf`, change `report.output.filename`, `report.parameter.*`, or `report.format`.

**Equivalent manual command:**

```bash
java -cp "target/lib/*:target/genexplus-1.0.0-SNAPSHOT.jar:target/additional_resources" \
  io.github.stefanbln.genexplus.report.Main \
  --config report.conf.example \
  --properties target/additional_resources/application.properties
```

---

## 2. Connect a database

**Goal:** Fill a template from PostgreSQL (or another JDBC database).

> **Full guide:** [DATABASE.md](DATABASE.md) explains how JasperReports Studio `defaultdataadapter`, `database.id`, and `dbN.*` properties fit together — including multi-DB setups and Docker-based testing.

### 2.1 Start local test databases (optional)

```bash
./scripts/test-db-up.sh   # PostgreSQL on :5432, MySQL on :3306
```

### 2.2 Configure `application.properties`

```properties
db1.url=jdbc:postgresql://dbhost:5432/mydb
db1.username=report_user
db1.password=secret
db1.driver=org.postgresql.Driver
```

Use `REPORT_DB1_PASSWORD` instead of a literal password in production.

### 2.3 Point the job file at the database

```properties
database.id=db1
report.database.optional=false
report.template=/path/to/sales-summary.jrxml
report.output.dir=/var/reports/out
report.output.filename=sales-summary.pdf
report.format=PDF
```

Remove `report.database.optional=true` when the report requires JDBC.

### 2.4 Verify connectivity

Run the job with `verbose=true`. Look for log lines `Connecting to database: db1` and `Connected to database: db1`. Exit code **3** indicates JDBC failure.

**Optional second database:** Add `db2.url`, `db2.username`, … and set `database.id=db2` in the job that needs it.

---

## 3. Deliver reports by email

**Goal:** SMTP delivery with HTML body and PDF attachment after a successful render.

### 3.1 Enable SMTP globally

In `application.properties`:

```properties
mail.smtp.enabled=true
mail.smtp.host=smtp.example.com
mail.smtp.port=587
mail.smtp.from=reports@example.com
mail.smtp.auth=true
mail.smtp.username=reports@example.com
mail.smtp.starttls.enable=true
# password via REPORT_MAIL_PASSWORD
```

For SMTPS (port 465):

```properties
mail.smtp.port=465
mail.smtp.ssl.enable=true
```

### 3.2 Enable email on the job

```properties
report.email.enabled=true
report.email.to=finance@example.com
report.email.cc=audit@example.com
report.email.subject=Monthly report {[yyyy-MM-dd]}
report.email.body=<p>Please find the <b>monthly report</b> attached.</p>
report.email.body.plain=Please find the monthly report attached.
```

Placeholders `{[pattern]}` in subject/body resolve to the current date/time at send time.

### 3.3 Optional inline logo

1. Place `company-logo.png` on the classpath (e.g. in `additional_resources/`).
2. Set `mail.smtp.logo.resource=company-logo.png`.
3. In HTML body: `<img src="cid:logoGenExPlusReport" alt="Logo">`.

### 3.4 Hardening

| Setting | Purpose |
|---------|---------|
| `mail.smtp.retry.count` | Transient failure retries |
| `mail.smtp.maxAttachmentBytes` | Cap attachment size |
| `report.email.body.escape=true` | HTML-escape body (untrusted content) |
| `GENEXPLUS_STRICT_EMAIL=false` | Exit 0 when email fails but file exists |

Test rendering first with `report.email.enabled=false`, then enable delivery.

---

## 4. Sign PDF output

**Goal:** Cryptographically sign and certify PDF reports so recipients can verify integrity and viewers restrict edits.

See **[PDF_SIGNING.md](PDF_SIGNING.md)** for the full guide (DocMDP, trust, PDF Inspector, troubleshooting).

### 4.1 Create a keystore (once)

Prefer OpenSSL so certificate extensions are explicit:

```bash
openssl req -x509 -newkey rsa:2048 \
  -keyout signer-key.pem -out signer-cert.pem -days 3650 -nodes \
  -subj "/CN=Report Signer,O=Your Org,C=DE" \
  -addext "keyUsage=critical,digitalSignature,nonRepudiation" \
  -addext "extendedKeyUsage=emailProtection,codeSigning" -sha256

openssl pkcs12 -export -out /secure/report-signer.p12 \
  -inkey signer-key.pem -in signer-cert.pem \
  -name report-signer -passout pass:'change-me'
```

### 4.2 Configure signing

```properties
signing.enabled=true
signing.keystore.path=/secure/report-signer.p12
signing.keystore.alias=report-signer
signing.keystore.type=PKCS12
signing.reason=Approved for distribution
signing.location=Finance Department
signing.certify=true
signing.permissions=1
signing.visible=true
```

Export `REPORT_KEYSTORE_PASSWORD`. The job file **must** use `report.format=PDF`. By default a **visible signature stamp** appears on the bottom-right of the last page (screen only; set `signing.visible.print=true` to include it in printouts).

### 4.3 Run

GenExPlus renders to a temp file, signs to another temp file, then atomically moves the signed PDF to the final path. Failure exits **4** (signing/render bucket).

---

## 5. Tune CSV and TEXT export

**Goal:** Control delimiter and plain-text layout.

### CSV

```properties
report.format=CSV
report.csv.delimiter=|
```

Global default in `application.properties`:

```properties
report.csv.delimiter=,
```

### TEXT

```properties
report.format=TEXT
report.text.pageWidthChars=132
report.text.pageHeightChars=66
report.text.charWidth=6
report.text.charHeight=12
```

Per-job governor overrides (optional):

```properties
report.governor.maxPages=1000
report.governor.timeoutSeconds=300
```

---

## 6. Schedule with cron

**Goal:** Nightly PDF generation with distinct exit handling.

```cron
15 2 * * * cd /opt/genexplus && ./start.sh /etc/genexplus/nightly.conf /etc/genexplus/application.properties >> /var/log/genexplus/nightly.log 2>&1
echo $? > /var/run/genexplus/nightly.exit
```

**Recommended practices:**

- Use absolute paths for `--properties`, templates, and output directories.
- Enable `report.timestamp.auto=true` to avoid overwriting prior outputs.
- In monitoring, treat exit **5** as partial success (file exists, email failed).
- Set `REPORT_*` secrets in the cron environment or a wrapped shell script — not in world-readable files.

**systemd timer example (unit fragment):**

```ini
[Service]
Type=oneshot
WorkingDirectory=/opt/genexplus
Environment=REPORT_DB1_PASSWORD=...
ExecStart=/opt/genexplus/start.sh /etc/genexplus/nightly.conf /etc/genexplus/application.properties
```

---

## 7. Run in Docker

**Goal:** Container image with JRE 21 and packaged layout.

```bash
mvn -DskipTests package
docker build -t genexplus:local .
docker run --rm \
  -v /path/to/reports:/reports \
  -v /path/to/application.properties:/config/application.properties \
  genexplus:local \
  --config /reports/nightly.conf \
  --properties /config/application.properties
```

Mount templates, output directories, and keystores as volumes. The image contains JAR + `lib/` only; configuration stays outside the image.

---

## 8. Override secrets with environment variables

**Goal:** Keep passwords out of property files on disk.

```bash
export REPORT_DB1_PASSWORD='...'
export REPORT_MAIL_PASSWORD='...'
export REPORT_KEYSTORE_PASSWORD='...'
./start.sh production.conf /etc/genexplus/application.properties
```

Property keys map to env vars: dots → underscores, prefix `REPORT_`, uppercase. JVM system properties (`-Ddb1.password=...`) also override file values after load.

**Merge order (later wins):**

1. `application.properties` file
2. Matching system properties
3. `REPORT_*` environment variables

---

## 9. Debug a failing run

1. Re-run with `--verbose` or `verbose=true` in the conf file.
2. Note the [exit code](CONFIGURATION.md#exit-codes).
3. Match stderr to the [troubleshooting table](../README.md#troubleshooting).
4. For validation errors, fix the conf file before retrying render.
5. For exit **5**, confirm SMTP with email disabled first, then enable delivery.

---

*For property-level detail see [CONFIGURATION.md](CONFIGURATION.md). For quick answers see [FAQ.md](FAQ.md).*

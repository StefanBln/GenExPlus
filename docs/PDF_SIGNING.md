# PDF digital signing

GenExPlus signs PDF reports **after** JasperReports export. Signing is cryptographic: the output contains a detached PKCS#7 CMS blob over a defined byte range of the file. This is not a decorative signature stamp or an empty `/Sig` form field.

When certification is enabled (the default), the PDF is also **locked** with DocMDP so compliant viewers treat content edits as invalidating the signature.

---

## Table of contents

1. [What you get](#what-you-get)
2. [Certification vs approval](#certification-vs-approval)
3. [Configuration](#configuration)
4. [Creating a signing keystore](#creating-a-signing-keystore)
5. [Certificate requirements](#certificate-requirements)
6. [Trust and PDF viewers](#trust-and-pdf-viewers)
7. [What PDF Inspector should show](#what-pdf-inspector-should-show)
8. [Operational flow](#operational-flow)
9. [Troubleshooting](#troubleshooting)
10. [Testing and verification](#testing-and-verification)

---

## What you get

A signed GenExPlus PDF contains:

| PDF object | Purpose |
|------------|---------|
| `/Sig` dictionary | Signature field with filter `Adobe.PPKLite`, subfilter `adbe.pkcs7.detached` |
| `/Contents` | DER-encoded detached PKCS#7 CMS signature |
| `/ByteRange` | Exact byte offsets hashed and signed (everything except the placeholder signature bytes) |
| `/Name` | Signer display name (certificate CN unless overridden) |
| `/Reason`, `/Location`, `/ContactInfo` | Optional metadata shown in signature panels |
| `/Perms` → `/DocMDP` | Present when certification is enabled; tells viewers which edits are allowed |
| Visible widget + `/AP` | On-page signature stamp (default) with signer name, date, and reason |

GenExPlus uses **Apache PDFBox** for incremental save and external signing, and **Bouncy Castle** to build the CMS structure. The implementation follows the same detached-signing pattern as the PDFBox `CreateSignature` examples.

**Important:** Cryptographic signing and what you *see* in a viewer are related but distinct:

- **Invisible signatures** (PDFBox default without a visual template) are valid PKCS#7 but use a **zero-size widget** — many viewers show nothing on the page and PDF Inspector may report weak or no security UI.
- **Visible signatures** (GenExPlus default, `signing.visible=true`) draw a stamp on the last page (bottom-right) so Acrobat, Preview, and similar apps show an obvious signed appearance.

Without DocMDP certification, the document can still be edited in some viewers — edits break the signature. With certification (default), viewers that honor DocMDP restrict or block modifications.

---

## Certification vs approval

PDF distinguishes two kinds of digital signatures:

### Certification signature (default in GenExPlus)

- First signature on a document; establishes a **trusted baseline**.
- Sets `/Perms` / `/DocMDP` with a permission level (`1`–`3`).
- Viewers such as Adobe Acrobat show the document as **certified** and restrict editing according to the level.
- GenExPlus enables this when `signing.certify=true` (default).

### Approval signature

- Adds a signature without certifying the document.
- Cryptographic integrity still applies to the signed byte range.
- The PDF may remain fully editable; subsequent edits invalidate the signature but are not blocked upfront.
- Set `signing.certify=false` for approval-only signing.

For report distribution where recipients must not alter figures or text, keep **`signing.certify=true`** and **`signing.permissions=1`**.

### DocMDP permission levels

| Level | Property value | Meaning |
|-------|----------------|---------|
| **1** | `signing.permissions=1` | No changes allowed (strongest; recommended for finalized reports) |
| **2** | `signing.permissions=2` | Form filling and additional digital signatures only |
| **3** | `signing.permissions=3` | Form filling, signing, and annotation (comments) |

---

## Configuration

All signing keys live in `application.properties` (deployment-wide). Signing applies only when **`signing.enabled=true`** and the job uses **`report.format=PDF`**.

### Required properties

```properties
signing.enabled=true
signing.keystore.path=/secure/report-signer.p12
signing.keystore.alias=report-signer
signing.keystore.type=PKCS12
```

Password: set `signing.keystore.password` or export **`REPORT_KEYSTORE_PASSWORD`** (preferred for production).

### Optional metadata

```properties
signing.reason=Approved for distribution
signing.location=Finance Department
signing.contact=reports@example.com
signing.signerName=Corporate Report Signer
```

If `signing.signerName` is omitted, GenExPlus uses the **CN** from the signing certificate subject.

### Certification policy

```properties
# Default: certify and lock the document (DocMDP level 1 = no changes)
signing.certify=true
signing.permissions=1

# Visible stamp on the last page (bottom-right) — default on
signing.visible=true
signing.visible.page=-1
signing.visible.print=false

# Approval-only: cryptographic signature without document lock
# signing.certify=false

# Invisible signature (crypto only, no on-page stamp)
# signing.visible=false
```

### Example job file

```properties
report.template=/reports/monthly-summary.jrxml
report.format=PDF
report.output=/output/monthly-summary.pdf
```

Validation rejects `signing.enabled=true` when `report.format` is not `PDF` (exit code **1**).

See [CONFIGURATION.md](CONFIGURATION.md) for the full property table.

---

## Creating a signing keystore

Use a **PKCS#12** (`.p12` / `.pfx`) file containing an RSA private key and X.509 certificate. Prefer **OpenSSL** so you can set `keyUsage` and `extendedKeyUsage` explicitly.

### Recommended: OpenSSL

```bash
# 1. Generate self-signed cert (replace subject) or use your CA workflow
openssl req -x509 -newkey rsa:2048 \
  -keyout signer-key.pem \
  -out signer-cert.pem \
  -days 3650 -nodes \
  -subj "/CN=Report Signer/O=Your Organization/C=DE" \
  -addext "keyUsage=critical,digitalSignature,nonRepudiation" \
  -addext "extendedKeyUsage=emailProtection,codeSigning" \
  -sha256

# 2. Export PKCS#12
openssl pkcs12 -export \
  -out /secure/report-signer.p12 \
  -inkey signer-key.pem \
  -in signer-cert.pem \
  -name report-signer \
  -passout pass:'choose-a-strong-password'

# 3. Remove PEM files from disk when done
shred -u signer-key.pem signer-cert.pem   # or secure deletion equivalent
```

For production, obtain the certificate from your **internal CA** or a public CA and import the issued cert + private key into PKCS#12.

### Alternative: keytool

```bash
keytool -genkeypair -alias report-signer -keyalg RSA -keysize 2048 \
  -sigalg SHA256withRSA -validity 3650 \
  -keystore /secure/report-signer.p12 -storetype PKCS12 \
  -storepass 'change-me' \
  -dname "CN=Report Signer, O=Your Organization, C=DE"
```

`keytool`-generated certificates may lack extended key usage OIDs; GenExPlus validates certificates at startup when signing is enabled. If validation fails, regenerate with OpenSSL or import a CA-issued certificate.

### Test keystore (development only)

The repository includes `src/test/resources/test-keystore.p12` for unit and E2E tests. Regenerate with:

```bash
./scripts/generate-test-keystore.sh
```

Never use the test keystore in production.

---

## Certificate requirements

At startup (and in preflight validation), GenExPlus checks the signing certificate:

| Check | Requirement |
|-------|-------------|
| Validity | Certificate must be within `notBefore` / `notAfter` |
| Key usage | `digitalSignature` and/or `nonRepudiation` when `keyUsage` extension is present |
| Extended key usage | When present, must include at least one of: `emailProtection` (1.3.6.1.5.5.7.3.4), `codeSigning` (1.3.6.1.5.5.7.3.3), or Adobe document signing (1.2.840.113583.1.1.5) |

Private key algorithm: **RSA** with SHA-256 (or the certificate’s declared signature algorithm).

---

## Trust and PDF viewers

Cryptographic signing and **trust** are separate concerns:

1. **Integrity** — GenExPlus embeds a valid PKCS#7 signature over the PDF byte range. Anyone can verify the math with the embedded certificate.
2. **Trust** — Viewers show “trusted” or “valid” only if the signer certificate chains to a root in the viewer’s trust store.

### Self-signed certificates

- Signatures are **cryptographically valid** but viewers report **untrusted** or **unknown identity**.
- PDF Inspector may list the signature but not show a green “trusted” badge until you import the cert into a trust store.
- This is expected; use a CA-issued certificate for production recipients.

### Enterprise deployment

- Issue signing certificates from your internal PKI.
- Distribute the CA (or signer) certificate to workstations via group policy, MDM, or manual import into:
  - **Adobe Acrobat:** Edit → Preferences → Signatures → Identities & Trusted Certificates
  - **macOS Preview / system:** Keychain Access → import and mark as trusted for code/signing where appropriate

GenExPlus does **not** timestamp signatures (no TSA). Long-term validation depends on certificate expiry and viewer policies.

---

## What PDF Inspector should show

For a GenExPlus PDF with default settings (`signing.certify=true`, `signing.permissions=1`):

| Inspector area | Expected |
|----------------|----------|
| Signatures | One signature, subfilter `adbe.pkcs7.detached` |
| Signer | `/Name` populated (certificate CN or `signing.signerName`) |
| On-page stamp | Visible box bottom-right on last page (`signing.visible=true`) |
| `/Contents` | Non-empty binary PKCS#7 blob |
| `/ByteRange` | Four integers defining signed regions |
| Security / permissions | DocMDP present via `/Perms`; document certified — **not** “no security” |
| Editability | Compliant viewers block or warn on content changes (permission level 1) |

If you previously saw only an empty signature tag with no security:

- The document was likely **approval-signed** without DocMDP, or
- The viewer did not treat a self-signed cert as trusted (metadata still should show signer and PKCS#7), or
- The file was not produced by the current signing pipeline.

E2E scenario **`08-signed-pdf`** writes a reference artifact to `target/e2e-reports/08-signed-pdf/signed.pdf` after `mvn test`.

---

## Operational flow

```
JasperReports export (unsigned PDF)
        │
        ▼
PdfSigningService.sign()
        │
        ├─ Load PKCS#12 private key + certificate chain
        ├─ Set PDSignature metadata (name, reason, location, contact)
        ├─ If signing.certify=true → apply DocMDP (/Perms, /Reference)
        ├─ document.addSignature() + saveIncrementalForExternalSigning()
        ├─ Hash/sign byte range → CMS via Bouncy Castle
        └─ Write signed PDF (atomic replace of final output path)
```

Failures during signing return exit code **4** (render/signing bucket). The unsigned temp file is discarded.

---

## Troubleshooting

### “No security” or fully editable PDF

| Cause | Fix |
|-------|-----|
| `signing.certify=false` | Set `signing.certify=true` for DocMDP lock |
| `signing.visible=false` | Set `signing.visible=true` for on-page stamp |
| Signing disabled | Confirm `signing.enabled=true` and format is PDF |
| Old build without DocMDP/visible appearance | Upgrade and regenerate the PDF |
| Viewer ignores DocMDP | Test in Adobe Acrobat; some lightweight viewers only show validity |

### Signature shows as invalid after opening

| Cause | Fix |
|-------|-----|
| File was modified after signing | Regenerate; do not post-process the PDF |
| Partial download or corruption | Verify file size and re-transfer |
| Byte range altered | Ensure no middleware rewrites the PDF |

### “Untrusted” but signature details visible

| Cause | Fix |
|-------|-----|
| Self-signed certificate | Use CA-issued cert or import signer into trust store |
| Expired certificate | Renew certificate and re-issue keystore |

### Startup / validation errors

| Message | Fix |
|---------|-----|
| `signing.keystore.path is required` | Set path to readable PKCS#12 file |
| `Keystore alias not found` | Match `signing.keystore.alias` to `-name` in openssl pkcs12 |
| `keyUsage must include digitalSignature` | Regenerate cert with proper extensions (see above) |
| `Signing non-PDF` / format mismatch | Use `report.format=PDF` only |

### Password and secrets

- Prefer `REPORT_KEYSTORE_PASSWORD` over plaintext in `application.properties`.
- Restrict filesystem permissions on the keystore (`chmod 600`, dedicated service account).
- Never commit keystores or passwords to version control.

---

## Testing and verification

### Automated tests

Unit and E2E tests use `PdfSignatureVerifier`, which:

1. Confirms a `/Sig` dictionary with `adbe.pkcs7.detached`
2. Reconstructs the signed byte range and verifies CMS with Bouncy Castle
3. Asserts signer `/Name` is present
4. When certification is expected, asserts DocMDP permission level via `/Perms`

Run signing-related tests:

```bash
mvn test -Dtest='io.github.stefanbln.genexplus.report.signing.*,io.github.stefanbln.genexplus.report.e2e.E2eSignedPdfTest'
```

### Manual verification

1. Run E2E scenario 08 or a signed production job.
2. Open `signed.pdf` in Adobe Acrobat → Signatures panel.
3. Confirm **certified** document (padlock) when `signing.certify=true`.
4. Attempt to edit body text — viewer should block or invalidate signature.
5. Optional: inspect with [PDF Inspector](https://www.pdf-online.com/pdf-inspector/) or `pdfsig` (poppler) for PKCS#7 details.

### Regenerate test keystore

```bash
./scripts/generate-test-keystore.sh
```

---

## Related documentation

- [HOWTO.md § Sign PDF output](HOWTO.md#4-sign-pdf-output) — minimal setup steps
- [CONFIGURATION.md](CONFIGURATION.md) — property reference
- [FAQ.md § PDF signing](FAQ.md#pdf-signing) — quick answers
- [E2E scenario 08](../src/test/resources/e2e/README.md) — signed PDF example

---

*Last updated: July 2026*

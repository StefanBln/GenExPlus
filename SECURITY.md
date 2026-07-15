# Security Policy

## Supported versions

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

## Reporting a vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Email the maintainer via GitHub ([@StefanBln](https://github.com/StefanBln)) or contact **Stefan Schuetz - Locivera - Berlin** through a private channel you already use for Locivera business communication.

Include:

- Description of the issue and impact
- Steps to reproduce
- Affected version(s)
- Any suggested fix (optional)

You should receive an acknowledgment within a reasonable time. We will coordinate disclosure and a fix before public announcement when appropriate.

## Scope

In scope:

- GenExPlus CLI and bundled configuration handling
- PDF signing and keystore handling
- SMTP delivery and credential loading (`REPORT_*` env vars, properties files)
- JDBC connection configuration and SQL used by report templates (operator-supplied)

Out of scope:

- Vulnerabilities in JasperReports, PDFBox, or other third-party libraries (report upstream; we can bump dependencies)
- Misconfiguration by operators (weak passwords in plain-text files, unsigned PDFs when signing was not enabled)
- Report template content supplied by users (malicious JRXML in your own deployment)

## Safe defaults

GenExPlus ships with signing and SMTP **disabled** by default. Secrets should be injected via environment variables in production. See [CONFIGURATION.md](docs/CONFIGURATION.md) and [HOWTO.md](docs/HOWTO.md).

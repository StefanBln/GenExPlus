#!/bin/bash
# Regenerate the PKCS#12 test keystore used by PDF signing unit and E2E tests.
#
# Creates a self-signed RSA certificate with key usages suitable for PDF CMS signing.
# Output: src/test/resources/test-keystore.p12
#   alias:     testalias
#   password:  testpass
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/src/test/resources/test-keystore.p12"
ALIAS="testalias"
STOREPASS="testpass"
TMPDIR="$(mktemp -d)"
trap 'rm -rf "$TMPDIR"' EXIT

if command -v openssl >/dev/null 2>&1; then
  openssl req -x509 -newkey rsa:2048 \
    -keyout "$TMPDIR/key.pem" \
    -out "$TMPDIR/cert.pem" \
    -days 3650 -nodes \
    -subj "/CN=GenExPlus Test Signer/OU=Engineering/O=GenExPlus/L=Berlin/ST=Berlin/C=DE" \
    -addext "keyUsage=critical,digitalSignature,nonRepudiation" \
    -addext "extendedKeyUsage=emailProtection,codeSigning" \
    -sha256

  openssl pkcs12 -export \
    -out "$OUT" \
    -inkey "$TMPDIR/key.pem" \
    -in "$TMPDIR/cert.pem" \
    -name "$ALIAS" \
    -passout "pass:$STOREPASS"
elif command -v keytool >/dev/null 2>&1; then
  rm -f "$OUT"
  keytool -genkeypair \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -sigalg SHA256withRSA \
    -validity 3650 \
    -storetype PKCS12 \
    -keystore "$OUT" \
    -storepass "$STOREPASS" \
    -keypass "$STOREPASS" \
    -dname "CN=GenExPlus Test Signer, OU=Engineering, O=GenExPlus, L=Berlin, ST=Berlin, C=DE"
else
  echo "openssl or keytool required" >&2
  exit 1
fi

echo "Created $OUT (alias=$ALIAS, storepass=$STOREPASS)"

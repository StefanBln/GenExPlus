/*
 * Copyright 2026 Stefan Schuetz - Locivera - Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.stefanbln.genexplus.report.signing;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Generates self-signed X.509 certificates for signing validation tests.
 */
final class CertTestSupport {

    private CertTestSupport() {}

    static X509Certificate validSigningCertificate() throws Exception {
        return generate("CN=Valid Signer", Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
                KeyUsage.digitalSignature | KeyUsage.nonRepudiation,
                List.of(KeyPurposeId.id_kp_emailProtection));
    }

    static X509Certificate expiredCertificate() throws Exception {
        return generate("CN=Expired Signer",
                Date.from(Instant.now().minus(10, ChronoUnit.DAYS)),
                Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                KeyUsage.digitalSignature,
                List.of(KeyPurposeId.id_kp_emailProtection));
    }

    static X509Certificate certificateWithoutSigningKeyUsage() throws Exception {
        return generate("CN=Bad Key Usage",
                Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
                KeyUsage.keyEncipherment,
                List.of(KeyPurposeId.id_kp_emailProtection));
    }

    static X509Certificate certificateWithRejectedExtendedKeyUsage() throws Exception {
        return generate("CN=Bad EKU",
                Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
                KeyUsage.digitalSignature,
                List.of(KeyPurposeId.id_kp_serverAuth));
    }

    static X509Certificate certificateWithDocumentSigningEku() throws Exception {
        return generate("CN=Document Signer",
                Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
                KeyUsage.digitalSignature,
                List.of(KeyPurposeId.getInstance(new ASN1ObjectIdentifier("1.3.6.1.5.5.7.3.36"))));
    }

    private static X509Certificate generate(
            String subjectDn,
            Date notBefore,
            Date notAfter,
            int keyUsageBits,
            List<KeyPurposeId> extendedKeyUsage) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        var issuer = new X500Name(subjectDn);
        var builder = new JcaX509v3CertificateBuilder(
                issuer,
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                issuer,
                keyPair.getPublic());
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsageBits));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        if (!extendedKeyUsage.isEmpty()) {
            builder.addExtension(
                    Extension.extendedKeyUsage,
                    false,
                    new org.bouncycastle.asn1.x509.ExtendedKeyUsage(extendedKeyUsage.toArray(KeyPurposeId[]::new)));
        }

        X509CertificateHolder holder = builder.build(new JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.getPrivate()));
        return new JcaX509CertificateConverter().getCertificate(holder);
    }
}

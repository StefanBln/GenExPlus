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

import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static io.github.stefanbln.genexplus.report.TestResources.testKeystorePath;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningCertificateSupportTest {

    @Test
    void extractCommonNameReturnsCertificateCn() throws Exception {
        var certificate = loadTestCertificate();
        assertEquals("GenExPlus Test Signer", SigningCertificateSupport.extractCommonName(certificate));
    }

    @Test
    void extractCommonNameFallsBackToSubjectDnWhenCnMissing() throws Exception {
        var certificate = CertTestSupport.certificateWithDocumentSigningEku();
        String name = SigningCertificateSupport.extractCommonName(certificate);
        assertTrue(name.contains("Document Signer"));
    }

    @Test
    void acceptsValidTestKeystoreCertificate() throws Exception {
        assertDoesNotThrow(() -> SigningCertificateSupport.validateCertificateForPdfSigning(loadTestCertificate()));
    }

    @Test
    void acceptsDocumentSigningExtendedKeyUsage() throws Exception {
        var certificate = CertTestSupport.certificateWithDocumentSigningEku();
        assertDoesNotThrow(() -> SigningCertificateSupport.validateCertificateForPdfSigning(certificate));
    }

    @Test
    void rejectsExpiredCertificate() throws Exception {
        var certificate = CertTestSupport.expiredCertificate();
        var error = assertThrows(
                SigningException.class,
                () -> SigningCertificateSupport.validateCertificateForPdfSigning(certificate));
        assertTrue(error.getMessage().contains("not valid"));
    }

    @Test
    void rejectsCertificateWithoutSigningKeyUsage() throws Exception {
        var certificate = CertTestSupport.certificateWithoutSigningKeyUsage();
        var error = assertThrows(
                SigningException.class,
                () -> SigningCertificateSupport.validateCertificateForPdfSigning(certificate));
        assertTrue(error.getMessage().contains("keyUsage"));
    }

    @Test
    void rejectsCertificateWithUnsupportedExtendedKeyUsage() throws Exception {
        var certificate = CertTestSupport.certificateWithRejectedExtendedKeyUsage();
        var error = assertThrows(
                SigningException.class,
                () -> SigningCertificateSupport.validateCertificateForPdfSigning(certificate));
        assertTrue(error.getMessage().contains("extendedKeyUsage"));
    }

    private static X509Certificate loadTestCertificate() throws Exception {
        var keystore = KeyStore.getInstance("PKCS12");
        try (var input = testKeystorePath().toUri().toURL().openStream()) {
            keystore.load(input, "testpass".toCharArray());
        }
        return (X509Certificate) keystore.getCertificate("testalias");
    }
}

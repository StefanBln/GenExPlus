package io.github.stefanbln.genexplus.report.signing;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

/**
 * Certificate metadata and validation helpers for PDF CMS signing.
 */
final class SigningCertificateSupport {

    private SigningCertificateSupport() {}

    /**
     * Returns the certificate common name (CN) for display in PDF signature panels.
     */
    static String extractCommonName(X509Certificate certificate) {
        try {
            LdapName ldapName = new LdapName(certificate.getSubjectX500Principal().getName());
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
            return certificate.getSubjectX500Principal().getName();
        } catch (Exception e) {
            return certificate.getSubjectX500Principal().getName();
        }
    }

    /**
     * Validates that the certificate is suitable for PDF digital signatures.
     */
    static void validateCertificateForPdfSigning(X509Certificate certificate) throws SigningException {
        try {
            certificate.checkValidity();
        } catch (Exception e) {
            throw new SigningException("Signing certificate is not valid: " + e.getMessage(), e);
        }

        boolean[] keyUsage = certificate.getKeyUsage();
        if (keyUsage != null && keyUsage.length > 0) {
            boolean digitalSignature = keyUsage.length > 0 && keyUsage[0];
            boolean nonRepudiation = keyUsage.length > 1 && keyUsage[1];
            if (!digitalSignature && !nonRepudiation) {
                throw new SigningException(
                        "Signing certificate keyUsage must include digitalSignature or nonRepudiation");
            }
        }

        try {
            var extendedKeyUsage = certificate.getExtendedKeyUsage();
            if (extendedKeyUsage != null
                    && !extendedKeyUsage.contains("1.3.6.1.5.5.7.3.4") // emailProtection
                    && !extendedKeyUsage.contains("1.3.6.1.5.5.7.3.3") // codeSigning
                    && !extendedKeyUsage.contains("1.3.6.1.5.5.7.3.36") // documentSigning
                    && !extendedKeyUsage.contains("1.2.840.113583.1.1.5")) { // Adobe authentic documents
                throw new SigningException(
                        "Signing certificate extendedKeyUsage should include emailProtection, "
                                + "codeSigning, documentSigning, or Adobe document signing OID");
            }
        } catch (CertificateParsingException e) {
            throw new SigningException("Failed to read signing certificate extensions: " + e.getMessage(), e);
        }
    }
}

package io.github.stefanbln.genexplus.report.signing;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Creates detached CMS/PKCS#7 signatures for PDF signing.
 *
 * <p>Implementation aligned with the Apache PDFBox {@code CreateSignatureBase} example:
 * detached {@code CMSSignedData} over the PDF byte range, encoded as DER for the
 * {@code /Contents} field.
 */
final class CmsSignatureGenerator {

    private CmsSignatureGenerator() {}

    /**
     * Signs the PDF byte range and returns a detached PKCS#7 blob.
     *
     * @param content stream over the exact bytes covered by the PDF {@code /ByteRange}
     * @param privateKey signing private key from the keystore
     * @param certificateChain certificate chain for the signer (at least the signer cert)
     * @return DER-encoded detached CMS signature
     */
    static byte[] sign(InputStream content, PrivateKey privateKey, Certificate[] certificateChain)
            throws IOException {
        try {
            var certificate = (X509Certificate) certificateChain[0];
            var generator = new CMSSignedDataGenerator();
            String signatureAlgorithm = resolveSignatureAlgorithm(certificate);
            var signer = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
                            .build(signer, certificate));
            generator.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));

            CMSTypedData processable = new CmsProcessableInputStream(content);
            var signedData = generator.generate(processable, false);
            return signedData.getEncoded();
        } catch (GeneralSecurityException | CMSException | OperatorCreationException e) {
            throw new IOException("Failed to create CMS signature: " + e.getMessage(), e);
        }
    }

    private static String resolveSignatureAlgorithm(X509Certificate certificate) {
        String algorithm = certificate.getSigAlgName();
        if (algorithm != null && !algorithm.isBlank()) {
            return algorithm;
        }
        return "SHA256withRSA";
    }

  /**
   * Memory-efficient CMS processable over an input stream (PDFBox pattern).
   */
    private static final class CmsProcessableInputStream implements CMSTypedData {
        private final InputStream inputStream;
        private final ASN1ObjectIdentifier contentType;

        private CmsProcessableInputStream(InputStream inputStream) {
            this.contentType = new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId());
            this.inputStream = inputStream;
        }

        @Override
        public ASN1ObjectIdentifier getContentType() {
            return contentType;
        }

        @Override
        public Object getContent() {
            return inputStream;
        }

        @Override
        public void write(OutputStream out) throws IOException, CMSException {
            inputStream.transferTo(out);
            inputStream.close();
        }
    }
}

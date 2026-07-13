package io.github.stefanbln.genexplus.report.signing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.COSFilterInputStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Cryptographically verifies detached PKCS#7 signatures embedded in PDF files.
 */
public final class PdfSignatureVerifier {

    private PdfSignatureVerifier() {}

    /**
     * Verifies PKCS#7 detached signature integrity and expected certification metadata.
     */
    public static void assertCryptographicallySigned(Path pdf) throws IOException {
        assertCryptographicallySigned(pdf, true, 1, true);
    }

    /**
     * Verifies PKCS#7 detached signature integrity.
     *
     * @param pdf signed PDF
     * @param expectCertified when true, requires DocMDP {@code /Perms} certification
     * @param expectedDocMdpPermission expected DocMDP level ({@code 1}–{@code 3}) when certified
     */
    public static void assertCryptographicallySigned(
            Path pdf, boolean expectCertified, int expectedDocMdpPermission) throws IOException {
        assertCryptographicallySigned(pdf, expectCertified, expectedDocMdpPermission, false);
    }

    /**
     * Verifies PKCS#7 detached signature integrity and optional visible appearance.
     */
    public static void assertCryptographicallySigned(
            Path pdf, boolean expectCertified, int expectedDocMdpPermission, boolean expectVisible)
            throws IOException {
        if (!Files.isRegularFile(pdf)) {
            throw new IOException("PDF not found: " + pdf);
        }

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            var signatures = document.getSignatureDictionaries();
            if (signatures.isEmpty()) {
                throw new IOException("No signature dictionary in PDF: " + pdf);
            }

            boolean verified = false;
            for (PDSignature signature : signatures) {
                try (InputStream fileInput = Files.newInputStream(pdf)) {
                    if (verifyDetachedPkcs7Signature(fileInput, signature)) {
                        verified = true;
                        break;
                    }
                }
            }
            if (!verified) {
                throw new IOException("No cryptographically valid PKCS#7 detached signature in: " + pdf);
            }

            var signature = signatures.getFirst();
            if (signature.getName() == null || signature.getName().isBlank()) {
                throw new IOException("Signature /Name (signer identity) is missing");
            }

            int docMdp = PdfDocMdpSupport.getDocMdpPermission(document);
            if (expectCertified && docMdp == 0) {
                throw new IOException("PDF is not certified (missing /Perms /DocMDP); document remains fully editable");
            }
            if (expectCertified && docMdp != expectedDocMdpPermission) {
                throw new IOException("Expected DocMDP permission " + expectedDocMdpPermission
                        + " but found " + docMdp);
            }

            if (expectVisible && !hasVisibleSignatureWidget(document)) {
                throw new IOException("Signature widget has zero size; visible appearance is missing");
            }
        } catch (GeneralSecurityException | CMSException | OperatorCreationException e) {
            throw new IOException("PDF signature verification failed: " + e.getMessage(), e);
        }
    }

    private static boolean verifyDetachedPkcs7Signature(InputStream pdfStream, PDSignature signature)
            throws IOException, CMSException, GeneralSecurityException, OperatorCreationException {
        var subFilter = signature.getSubFilter();
        if (subFilter == null || !PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED.getName().equals(subFilter)) {
            return false;
        }

        byte[] contents = signature.getContents();
        if (contents == null || contents.length == 0) {
            return false;
        }

        int[] byteRange = signature.getByteRange();
        if (byteRange == null || byteRange.length != 4) {
            return false;
        }

        byte[] signedBytes;
        try (InputStream signedContent = new COSFilterInputStream(pdfStream, byteRange)) {
            signedBytes = signedContent.readAllBytes();
        }
        if (signedBytes.length == 0) {
            return false;
        }

        CMSSignedData signedData = new CMSSignedData(new CMSProcessableByteArray(signedBytes), contents);
        Store<?> certificates = signedData.getCertificates();
        Collection<SignerInformation> signers = signedData.getSignerInfos().getSigners();
        if (signers.isEmpty()) {
            return false;
        }

        SignerInformation signer = signers.iterator().next();
        @SuppressWarnings("unchecked")
        Collection<org.bouncycastle.cert.X509CertificateHolder> matches =
                certificates.getMatches(signer.getSID());
        if (matches.isEmpty()) {
            return false;
        }

        X509Certificate certificate = new JcaX509CertificateConverter()
                .getCertificate(matches.iterator().next());
        return signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificate));
    }

    private static boolean hasVisibleSignatureWidget(PDDocument document) throws IOException {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            return false;
        }
        for (PDField field : acroForm.getFields()) {
            if (!(field instanceof PDSignatureField signatureField)) {
                continue;
            }
            for (PDAnnotationWidget widget : signatureField.getWidgets()) {
                PDRectangle rect = widget.getRectangle();
                if (rect != null && rect.getWidth() > 0 && rect.getHeight() > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}

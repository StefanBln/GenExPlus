package io.github.stefanbln.genexplus.report.signing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Post-signs exported PDF files using detached PKCS#7 CMS signatures.
 *
 * <p>GenExPlus renders the PDF first, then this service:
 * <ol>
 *   <li>Creates a {@code /Sig} field with signer metadata from the X.509 certificate</li>
 *   <li>Optionally certifies the document with DocMDP (locks content against modification)</li>
 *   <li>Embeds a detached PKCS#7 CMS blob over the PDF byte range</li>
 * </ol>
 *
 * <p>This is cryptographic signing — not a decorative signature stamp. Viewers such as Adobe Acrobat
 * and PDF Inspector should report a valid PKCS#7 detached signature when the certificate is trusted.
 */
public final class PdfSigningService {

    private static final Logger LOGGER = Logger.getLogger(PdfSigningService.class.getName());

    /**
     * Signs an unsigned PDF and writes the certified/signed result to a new file.
     */
    public void sign(Path unsignedPdf, Path signedPdf, SigningConfig config) throws SigningException {
        try {
            if (!Files.exists(unsignedPdf)) {
                throw new SigningException("Unsigned PDF not found: " + unsignedPdf);
            }

            var keyMaterial = config.loadPrivateKeyAndChain();
            signInternal(unsignedPdf, signedPdf, config, keyMaterial);
            LOGGER.log(Level.INFO, "PDF signed successfully: {0}", signedPdf);
        } catch (SigningException e) {
            throw e;
        } catch (IOException e) {
            throw new SigningException("Failed to sign PDF: " + e.getMessage(), e);
        } finally {
            config.clearPassword();
        }
    }

    private static void signInternal(Path unsignedPdf, Path signedPdf, SigningConfig config,
            SigningConfig.KeyMaterial keyMaterial) throws IOException, SigningException {
        var parent = signedPdf.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        var signerCertificate = (X509Certificate) keyMaterial.certificateChain()[0];

        try (PDDocument document = Loader.loadPDF(unsignedPdf.toFile());
             OutputStream output = Files.newOutputStream(signedPdf)) {
            PdfVisibleSignatureSupport.prepareAcroFormForSigning(document);

            var signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setSignDate(Calendar.getInstance());

            String displayName = config.signerName()
                    .orElseGet(() -> SigningCertificateSupport.extractCommonName(signerCertificate));
            signature.setName(displayName);
            config.reason().ifPresent(signature::setReason);
            config.location().ifPresent(signature::setLocation);
            config.contactInfo().ifPresent(signature::setContactInfo);

            if (config.certify()) {
                PdfDocMdpSupport.setDocMdpPermission(document, signature, config.docMdpPermission());
                LOGGER.log(Level.FINE, "Applying DocMDP certification with permission level {0}",
                        config.docMdpPermission());
            }

            var signatureOptions = new SignatureOptions();
            signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE);

            if (config.visible()) {
                int pageIndex = PdfVisibleSignatureSupport.resolvePageIndex(document, config.visiblePage());
                var rect = PdfVisibleSignatureSupport.createBottomRightRectangle(document, pageIndex);
                signatureOptions.setVisualSignature(
                        PdfVisibleSignatureSupport.createVisualSignatureTemplate(
                                document, pageIndex, rect, signature));
                signatureOptions.setPage(pageIndex);
                LOGGER.log(Level.FINE, "Applying visible signature on page {0} (print={1})",
                        new Object[] {pageIndex + 1, config.visiblePrint()});
            }

            document.addSignature(signature, signatureOptions);

            if (config.visible()) {
                PdfVisibleSignatureSupport.applyStampPrintPolicy(document, config.visiblePrint());
            }

            ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(output);
            try (InputStream content = externalSigning.getContent()) {
                byte[] cmsSignature = CmsSignatureGenerator.sign(
                        content,
                        keyMaterial.privateKey(),
                        keyMaterial.certificateChain());
                externalSigning.setSignature(cmsSignature);
            } finally {
                signatureOptions.close();
            }
        }
    }
}

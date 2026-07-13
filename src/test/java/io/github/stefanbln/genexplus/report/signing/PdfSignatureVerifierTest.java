package io.github.stefanbln.genexplus.report.signing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfSignatureVerifierTest {

    @TempDir
    Path tempDir;

    @Test
    void verifiesCryptographicSignatureFromSigningService() throws Exception {
        var unsignedPdf = tempDir.resolve("unsigned.pdf");
        var signedPdf = tempDir.resolve("signed.pdf");
        renderAndSign(unsignedPdf, signedPdf);

        assertDoesNotThrow(() -> PdfSignatureVerifier.assertCryptographicallySigned(signedPdf));
    }

    @Test
    void approvalOnlySignatureSkipsDocMdpRequirement() throws Exception {
        var unsignedPdf = tempDir.resolve("unsigned.pdf");
        var signedPdf = tempDir.resolve("signed.pdf");
        PdfSigningServiceTestHelper.renderSamplePdf(unsignedPdf);

        var configuration = PdfSigningServiceTestHelper.enabledSigningConfiguration();
        configuration.setProperty("signing.certify", "false");
        new PdfSigningService().sign(unsignedPdf, signedPdf, SigningConfig.from(configuration));

        assertDoesNotThrow(() -> PdfSignatureVerifier.assertCryptographicallySigned(signedPdf, false, 0, false));
    }

    @Test
    void invisibleSignatureSkipsVisibleWidgetRequirement() throws Exception {
        var unsignedPdf = tempDir.resolve("unsigned.pdf");
        var signedPdf = tempDir.resolve("signed.pdf");
        PdfSigningServiceTestHelper.renderSamplePdf(unsignedPdf);

        var configuration = PdfSigningServiceTestHelper.enabledSigningConfiguration();
        configuration.setProperty("signing.visible", "false");
        new PdfSigningService().sign(unsignedPdf, signedPdf, SigningConfig.from(configuration));

        assertDoesNotThrow(() -> PdfSignatureVerifier.assertCryptographicallySigned(signedPdf, true, 1, false));
    }

    @Test
    void rejectsUnsignedPdf() throws Exception {
        var unsignedPdf = tempDir.resolve("unsigned.pdf");
        PdfSigningServiceTestHelper.renderSamplePdf(unsignedPdf);

        assertThrows(IOException.class,
                () -> PdfSignatureVerifier.assertCryptographicallySigned(unsignedPdf));
    }

    private static void renderAndSign(Path unsignedPdf, Path signedPdf) throws Exception {
        PdfSigningServiceTestHelper.renderSamplePdf(unsignedPdf);
        new PdfSigningService().sign(unsignedPdf, signedPdf, PdfSigningServiceTestHelper.testSigningConfig());
    }
}

package io.github.stefanbln.genexplus.report.signing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfSigningServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsWrongKeystorePassword() throws Exception {
        var configuration = PdfSigningServiceTestHelper.enabledSigningConfiguration();
        configuration.setProperty("signing.keystore.password", "wrong-password");

        var error = SigningConfig.validateForReporting(configuration);
        assertNotNull(error);
        assertTrue(error.toLowerCase().contains("password") || error.toLowerCase().contains("keystore"));
    }

    @Test
    void rejectsMissingAlias() throws Exception {
        var configuration = PdfSigningServiceTestHelper.enabledSigningConfiguration();
        configuration.setProperty("signing.keystore.alias", "missing-alias");

        var error = SigningConfig.validateForReporting(configuration);
        assertNotNull(error);
        assertTrue(error.contains("alias"));
    }
}

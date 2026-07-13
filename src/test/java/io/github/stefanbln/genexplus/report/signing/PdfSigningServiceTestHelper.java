package io.github.stefanbln.genexplus.report.signing;

import io.github.stefanbln.genexplus.report.JasperRuntimeSupport;
import io.github.stefanbln.genexplus.report.Renderer;
import io.github.stefanbln.genexplus.report.config.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static io.github.stefanbln.genexplus.report.TestResources.testKeystorePath;

/**
 * Shared helpers for PDF signing tests.
 */
final class PdfSigningServiceTestHelper {

    private PdfSigningServiceTestHelper() {}

    static void renderSamplePdf(Path outputFile) throws Exception {
        JasperRuntimeSupport.configureHeadlessDefaults();
        var renderer = new Renderer(new Configuration(""));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Signing Test");
        parameters.put("Author", "GenExPlus");

        try (var out = Files.newOutputStream(outputFile)) {
            renderer.renderToStream("test-template.jrxml", "PDF", parameters, null, out);
        }
    }

    static SigningConfig testSigningConfig() throws Exception {
        return SigningConfig.from(enabledSigningConfiguration());
    }

    static Configuration enabledSigningConfiguration() throws Exception {
        var configuration = new Configuration("");
        configuration.setProperty("signing.enabled", "true");
        configuration.setProperty("signing.keystore.path", testKeystorePath().toString());
        configuration.setProperty("signing.keystore.type", "PKCS12");
        configuration.setProperty("signing.keystore.alias", "testalias");
        configuration.setProperty("signing.keystore.password", "testpass");
        configuration.setProperty("signing.reason", "GenExPlus test signature");
        configuration.setProperty("signing.location", "GenExPlus E2E");
        return configuration;
    }
}

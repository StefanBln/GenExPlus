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

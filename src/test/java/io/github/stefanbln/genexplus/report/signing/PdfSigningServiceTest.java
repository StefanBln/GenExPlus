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

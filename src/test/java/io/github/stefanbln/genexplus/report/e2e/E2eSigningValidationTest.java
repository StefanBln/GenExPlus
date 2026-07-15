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

package io.github.stefanbln.genexplus.report.e2e;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.assertScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.copyKeystoreToTemp;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.defaultE2eProperties;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.outputDirFor;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.run;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.signingNonPdfScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.writePropertiesWithOverrides;

@Tag("e2e")
class E2eSigningValidationTest {

    @Test
    void rejectsSigningWhenFormatIsNotPdf() throws Exception {
        var keystore = copyKeystoreToTemp();
        var properties = writePropertiesWithOverrides(defaultE2eProperties(), Map.of(
                "signing.enabled", "true",
                "signing.keystore.path", keystore.toString(),
                "signing.keystore.alias", "testalias",
                "signing.keystore.type", "PKCS12",
                "signing.keystore.password", "testpass"
        ));

        var scenario = signingNonPdfScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties);
        assertScenario(scenario, result);
    }
}

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

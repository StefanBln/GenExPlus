package io.github.stefanbln.genexplus.report.e2e;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Map;

import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.assertScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.defaultE2eProperties;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.emailFailureScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.outputDirFor;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.run;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.writePropertiesWithOverrides;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("e2e")
@Tag("e2e-email")
class E2eEmailFailureTest {

    @Test
    void exitsFiveWhenSmtpUnreachableButFileWritten() throws Exception {
        var properties = writePropertiesWithOverrides(defaultE2eProperties(), Map.of(
                "mail.smtp.enabled", "true",
                "mail.smtp.host", "127.0.0.1",
                "mail.smtp.port", "1",
                "mail.smtp.from", "sender@example.com",
                "mail.smtp.auth", "false",
                "mail.smtp.starttls.enable", "false"
        ));

        var scenario = emailFailureScenario();
        var scenarioDir = outputDirFor(scenario.id());
        var result = run(scenario, scenarioDir, properties);
        assertScenario(scenario, result);
        assertTrue(Files.exists(scenarioDir.resolve("email-fail.pdf")));
    }
}

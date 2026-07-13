package io.github.stefanbln.genexplus.report.e2e;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;
import java.util.Map;

import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.assertScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.defaultE2eProperties;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.emailDeliveryScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.outputDirFor;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.run;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.writePropertiesWithOverrides;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("e2e")
@Tag("e2e-email")
class E2eEmailCliTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    private Path properties;

    @BeforeEach
    void setUp() throws Exception {
        GREEN_MAIL.reset();
        properties = writePropertiesWithOverrides(defaultE2eProperties(), Map.of(
                "mail.smtp.enabled", "true",
                "mail.smtp.host", "127.0.0.1",
                "mail.smtp.port", String.valueOf(GREEN_MAIL.getSmtp().getPort()),
                "mail.smtp.from", "sender@example.com",
                "mail.smtp.auth", "false",
                "mail.smtp.starttls.enable", "false"
        ));
    }

    @Test
    void deliversReportViaFullCliPipeline() throws Exception {
        var scenario = emailDeliveryScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties);
        assertScenario(scenario, result);
        assertEquals(1, GREEN_MAIL.getReceivedMessages().length);
    }
}

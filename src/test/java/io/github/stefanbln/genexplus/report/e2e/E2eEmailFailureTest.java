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
                "mail.smtp.starttls.enable", "false",
                "mail.smtp.retry.count", "0",
                "mail.smtp.connectiontimeout", "2000",
                "mail.smtp.timeout", "2000"
        ));

        var scenario = emailFailureScenario();
        var scenarioDir = outputDirFor(scenario.id());
        var result = run(scenario, scenarioDir, properties);
        assertScenario(scenario, result);
        assertTrue(Files.exists(scenarioDir.resolve("email-fail.pdf")));
    }
}

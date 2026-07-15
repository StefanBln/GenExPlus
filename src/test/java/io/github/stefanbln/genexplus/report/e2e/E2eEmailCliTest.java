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

import com.icegreen.greenmail.junit5.GreenMailExtension;
import io.github.stefanbln.genexplus.report.GreenMailTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;

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
    static final GreenMailExtension GREEN_MAIL = GreenMailTestSupport.extension();

    private Path properties;

    @BeforeEach
    void setUp() throws Exception {
        GREEN_MAIL.reset();
        properties = writePropertiesWithOverrides(
                defaultE2eProperties(), GreenMailTestSupport.smtpProperties(GREEN_MAIL));
    }

    @Test
    void deliversReportViaFullCliPipeline() throws Exception {
        var scenario = emailDeliveryScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties);
        assertScenario(scenario, result);
        assertEquals(1, GREEN_MAIL.getReceivedMessages().length);
    }
}

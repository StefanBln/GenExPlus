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

package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationSettingsValidatorTest {

    @Test
    void validDefaultsPass() throws IOException {
        var appConfig = new Configuration("");
        assertNull(ApplicationSettingsValidator.validateLocaleAndTimezone(appConfig));
    }

    @Test
    void invalidLocaleFails() throws IOException {
        var appConfig = new Configuration("");
        appConfig.setProperty("report.default.locale", "!!!");

        var error = ApplicationSettingsValidator.validateLocaleAndTimezone(appConfig);
        assertNotNull(error);
        assertTrue(error.contains("report.default.locale"));
    }

    @Test
    void invalidTimezoneFails() throws IOException {
        var appConfig = new Configuration("");
        appConfig.setProperty("report.default.timezone", "Not/A/Timezone");

        var error = ApplicationSettingsValidator.validateLocaleAndTimezone(appConfig);
        assertNotNull(error);
        assertTrue(error.contains("report.default.timezone"));
    }
}

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

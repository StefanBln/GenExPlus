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

package io.github.stefanbln.genexplus.report.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ExportSettingsTest {

    @Test
    void loadsPerReportOverrides() throws Exception {
        var appConfig = new Configuration("");
        appConfig.setProperty("report.csv.delimiter", ",");
        appConfig.setProperty("report.text.pageWidthChars", "100");

        var reportProps = new Properties();
        reportProps.setProperty("report.csv.delimiter", "|");
        reportProps.setProperty("report.governor.maxPages", "25");
        reportProps.setProperty("report.governor.timeoutSeconds", "120");

        var settings = ExportSettings.from(reportProps, appConfig);

        assertEquals("|", settings.csvDelimiter());
        assertEquals(100, settings.textPageWidthChars());
        assertEquals(25, settings.governorMaxPages());
        assertEquals(120, settings.governorTimeoutSeconds());
    }

    @Test
    void fallsBackToApplicationDefaults() throws Exception {
        var appConfig = new Configuration("");
        appConfig.setProperty("report.csv.delimiter", "\t");
        appConfig.setProperty("report.text.pageHeightChars", "80");
        appConfig.setProperty("report.governor.maxPages", "50");

        var settings = ExportSettings.from(new Properties(), appConfig);

        assertEquals("\t", settings.csvDelimiter());
        assertEquals(80, settings.textPageHeightChars());
        assertEquals(50, settings.governorMaxPages());
        assertNull(settings.governorTimeoutSeconds());
    }

    @Test
    void blankCsvDelimiterFallsBackToDefault() throws Exception {
        var appConfig = new Configuration("");
        var reportProps = new Properties();
        reportProps.setProperty("report.csv.delimiter", "   ");

        var settings = ExportSettings.from(reportProps, appConfig);

        assertEquals(";", settings.csvDelimiter());
    }
}

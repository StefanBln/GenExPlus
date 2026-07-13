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

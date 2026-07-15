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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test-Klasse für ReportConfig.
 *
 */
class ReportConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void testLoadValidConfig() throws IOException {
        Path configFile = tempDir.resolve("test.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/absolute/path/test.jrxml
                report.output.dir=/absolute/path/output
                report.output.filename=report.pdf
                report.format=PDF
                report.parameter.Title=Test
                report.parameter.Year=2025
                report.parameter.Enabled=true
                verbose=true
                """);

        var config = ReportConfig.load(configFile.toString());

        assertEquals("db1", config.getDatabaseId());
        assertEquals("/absolute/path/test.jrxml", config.getTemplate());
        assertEquals("/absolute/path/output", config.getOutputDir());
        assertEquals("report.pdf", config.getOutputFilename());
        assertEquals("PDF", config.getFormat());
        assertTrue(config.isVerbose());
        assertEquals(3, config.getParameters().size());
        assertEquals("Test", config.getParameters().get("Title"));
        assertEquals(2025, config.getParameters().get("Year"));
        assertEquals(true, config.getParameters().get("Enabled"));
    }

    @Test
    void testLoadConfigWithDefaults() throws IOException {
        Path configFile = tempDir.resolve("minimal.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.pdf
                """);

        var config = ReportConfig.load(configFile.toString());

        assertEquals("PDF", config.getFormat()); // Default
        assertFalse(config.isVerbose()); // Default
        assertTrue(config.getParameters().isEmpty());
    }

    @Test
    void loadsConfigWithoutDatabaseIdForDeferredValidation() throws IOException {
        Path configFile = tempDir.resolve("no-db-id.conf");
        Files.writeString(configFile, """
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.pdf
                """);

        var config = ReportConfig.load(configFile.toString());

        assertTrue(config.getDatabaseId().isBlank());
        assertFalse(config.isDatabaseOptional());
    }

    @Test
    void testLoadConfigWithoutDatabaseWhenOptional() throws IOException {
        Path configFile = tempDir.resolve("optional-db.conf");
        Files.writeString(configFile, """
                report.database.optional=true
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.pdf
                """);

        var config = ReportConfig.load(configFile.toString());

        assertEquals("", config.getDatabaseId());
        assertTrue(config.isDatabaseOptional());
    }

    @Test
    void testLoadConfigMissingTemplate() throws IOException {
        Path configFile = tempDir.resolve("invalid.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.output.dir=/path/output
                report.output.filename=report.pdf
                """);

        assertThrows(IOException.class, () -> ReportConfig.load(configFile.toString()));
    }

    @Test
    void testLoadConfigMissingOutputDir() throws IOException {
        Path configFile = tempDir.resolve("invalid.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.filename=report.pdf
                """);

        assertThrows(IOException.class, () -> ReportConfig.load(configFile.toString()));
    }

    @Test
    void testLoadConfigMissingOutputFilename() throws IOException {
        Path configFile = tempDir.resolve("invalid.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                """);

        assertThrows(IOException.class, () -> ReportConfig.load(configFile.toString()));
    }

    @Test
    void testLoadNonExistentFile() {
        assertThrows(IOException.class, () -> ReportConfig.load("nonexistent.conf"));
    }

    @Test
    void testParameterTypeParsing() throws IOException {
        Path configFile = tempDir.resolve("types.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.pdf
                report.parameter.StringValue=Hello
                report.parameter.IntValue=42
                report.parameter.DoubleValue=3.14
                report.parameter.BooleanTrue=true
                report.parameter.BooleanFalse=false
                """);

        var config = ReportConfig.load(configFile.toString());

        assertEquals("Hello", config.getParameters().get("StringValue"));
        assertEquals(42, config.getParameters().get("IntValue"));
        assertEquals(3.14, config.getParameters().get("DoubleValue"));
        assertEquals(true, config.getParameters().get("BooleanTrue"));
        assertEquals(false, config.getParameters().get("BooleanFalse"));
    }

    @Test
    void testFormatNormalization() throws IOException {
        Path configFile = tempDir.resolve("format.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.xlsx
                report.format=xlsx
                """);

        var config = ReportConfig.load(configFile.toString());

        assertEquals("XLSX", config.getFormat()); // Should be uppercased
    }

    @Test
    void testAutoTimestampDefaults() throws IOException {
        Path configFile = tempDir.resolve("timestamp.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.pdf
                """);

        var config = ReportConfig.load(configFile.toString());

        // Default: auto-timestamp enabled
        assertTrue(config.isAutoTimestamp());
        assertEquals("yyyyMMdd_HHmmss", config.getTimestampPattern());
    }

    @Test
    void testAutoTimestampDisabled() throws IOException {
        Path configFile = tempDir.resolve("no-timestamp.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.pdf
                report.timestamp.auto=false
                """);

        var config = ReportConfig.load(configFile.toString());

        assertFalse(config.isAutoTimestamp());
    }

    @Test
    void testDefaultFormatFromApplicationProperties() throws Exception {
        Path configFile = tempDir.resolve("default-format.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.csv
                """);

        var appConfig = new Configuration("");
        appConfig.setProperty("report.default.format", "csv");

        var config = ReportConfig.load(configFile.toString(), appConfig);

        assertEquals("CSV", config.getFormat());
        assertEquals(";", config.getExportSettings().csvDelimiter());
    }

    @Test
    void testExportSettingsFromReportConfig() throws IOException {
        Path configFile = tempDir.resolve("export.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.csv
                report.format=CSV
                report.csv.delimiter=|
                report.governor.maxPages=10
                report.email.body.escape=true
                """);

        var config = ReportConfig.load(configFile.toString());

        assertEquals("|", config.getExportSettings().csvDelimiter());
        assertEquals(10, config.getExportSettings().governorMaxPages());
        assertTrue(config.isEmailBodyEscape());
    }

    @Test
    void testCustomTimestampPattern() throws IOException {
        Path configFile = tempDir.resolve("custom-pattern.conf");
        Files.writeString(configFile, """
                database.id=db1
                report.template=/path/test.jrxml
                report.output.dir=/path/output
                report.output.filename=report.pdf
                report.timestamp.auto=true
                report.timestamp.pattern=yyyy-MM-dd
                """);

        var config = ReportConfig.load(configFile.toString());

        assertTrue(config.isAutoTimestamp());
        assertEquals("yyyy-MM-dd", config.getTimestampPattern());
    }
}

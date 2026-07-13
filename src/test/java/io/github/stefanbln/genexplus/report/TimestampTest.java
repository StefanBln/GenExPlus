package io.github.stefanbln.genexplus.report;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for automatic timestamps in output filenames.
 *
 * <p>Each test runs the full CLI with XLSX rendering and is tagged
 * {@code @Tag("integration")}.
 */
class TimestampTest {

    private final Main main = new Main();

    @TempDir
    Path tempDir;

    @Test
    @Tag("integration")
    void testAutoTimestampEnabled() throws Exception {
        Path configFile = createConfigFile("timestamp-enabled.conf", """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=report.xlsx
                report.format=XLSX
                report.timestamp.auto=true
                """.formatted(tempDir.toString()));

        int exitCode = main.run(new String[]{"--config", configFile.toString()});
        assertEquals(0, exitCode);

        // Verify that a timestamped file exists
        Pattern pattern = Pattern.compile("report_\\d{8}_\\d{6}\\.xlsx");
        var files = Files.list(tempDir)
                .filter(f -> pattern.matcher(f.getFileName().toString()).matches())
                .toList();

        assertEquals(1, files.size(), "Expected exactly one timestamped file");
        assertTrue(Files.exists(files.get(0)));
        assertTrue(Files.size(files.get(0)) > 0);
    }

    @Test
    @Tag("integration")
    void testAutoTimestampDisabled() throws Exception {
        Path configFile = createConfigFile("timestamp-disabled.conf", """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=report-no-timestamp.xlsx
                report.format=XLSX
                report.timestamp.auto=false
                """.formatted(tempDir.toString()));

        int exitCode = main.run(new String[]{"--config", configFile.toString()});
        assertEquals(0, exitCode);

        // Verify that the file exists without a timestamp suffix
        Path expectedFile = tempDir.resolve("report-no-timestamp.xlsx");
        assertTrue(Files.exists(expectedFile));
        assertTrue(Files.size(expectedFile) > 0);
    }

    @Test
    @Tag("integration")
    void testCustomTimestampPattern() throws Exception {
        Path configFile = createConfigFile("custom-pattern.conf", """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=monthly.xlsx
                report.format=XLSX
                report.timestamp.auto=true
                report.timestamp.pattern=yyyy-MM
                """.formatted(tempDir.toString()));

        int exitCode = main.run(new String[]{"--config", configFile.toString()});
        assertEquals(0, exitCode);

        // Verify that a file with the custom pattern exists
        Pattern pattern = Pattern.compile("monthly_\\d{4}-\\d{2}\\.xlsx");
        var files = Files.list(tempDir)
                .filter(f -> pattern.matcher(f.getFileName().toString()).matches())
                .toList();

        assertEquals(1, files.size(), "Expected exactly one file with custom timestamp");
        assertTrue(Files.exists(files.get(0)));
    }

    @Test
    @Tag("integration")
    void testTimestampWithManualPlaceholder() throws Exception {
        Path configFile = createConfigFile("both.conf", """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=report_{[yyyyMMdd]}.xlsx
                report.format=XLSX
                report.timestamp.auto=true
                report.timestamp.pattern=HHmmss
                """.formatted(tempDir.toString()));

        int exitCode = main.run(new String[]{"--config", configFile.toString()});
        assertEquals(0, exitCode);

        // Expected format: report_20251008_143052.xlsx
        Pattern pattern = Pattern.compile("report_\\d{8}_\\d{6}\\.xlsx");
        var files = Files.list(tempDir)
                .filter(f -> pattern.matcher(f.getFileName().toString()).matches())
                .toList();

        assertEquals(1, files.size());
        assertTrue(Files.exists(files.get(0)));
    }

    private Path createConfigFile(String filename, String content) throws Exception {
        Path configFile = tempDir.resolve(filename);
        Files.writeString(configFile, content);
        return configFile;
    }
}

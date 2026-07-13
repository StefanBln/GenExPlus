package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.ReportConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OutputPathResolverTest {

    private final OutputPathResolver resolver = new OutputPathResolver();

    @TempDir
    Path tempDir;

    @Test
    void isValidTimestampPatternAcceptsKnownPattern() {
        assertTrue(OutputPathResolver.isValidTimestampPattern("yyyyMMdd_HHmmss"));
    }

    @Test
    void isValidTimestampPatternRejectsInvalidPattern() {
        assertFalse(OutputPathResolver.isValidTimestampPattern("not-valid-[["));
    }

    @Test
    void resolvesFilenamePlaceholder() throws IOException {
        var config = loadConfig("""
                report.database.optional=true
                report.template=sample-report.jrxml
                report.output.dir=%s
                report.output.filename=report_{[yyyyMMdd]}.pdf
                report.format=PDF
                report.timestamp.auto=false
                """.formatted(tempDir));

        var outputPath = resolver.resolve(config);
        assertTrue(outputPath.getFileName().toString().matches("report_\\d{8}\\.pdf"));
        assertEquals(tempDir.toAbsolutePath().normalize(), outputPath.getParent().toAbsolutePath().normalize());
    }

    @Test
    void appendsTimestampWhenAutoEnabled() throws IOException {
        var config = loadConfig("""
                report.database.optional=true
                report.template=sample-report.jrxml
                report.output.dir=%s
                report.output.filename=monthly.pdf
                report.format=PDF
                report.timestamp.auto=true
                report.timestamp.pattern=yyyy-MM
                """.formatted(tempDir));

        var outputPath = resolver.resolve(config);
        assertTrue(outputPath.getFileName().toString().matches("monthly_\\d{4}-\\d{2}\\.pdf"));
    }

    private ReportConfig loadConfig(String content) throws IOException {
        var configFile = tempDir.resolve("report.conf");
        Files.writeString(configFile, content);
        return ReportConfig.load(configFile.toString());
    }
}

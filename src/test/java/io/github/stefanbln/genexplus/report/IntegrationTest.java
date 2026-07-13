package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for GenExPlus.
 *
 * <p>Heavy tests that render reports or run the full CLI are tagged
 * {@code @Tag("integration")} and excluded from the default {@code mvn test} run.
 */
class IntegrationTest {

    @TempDir
    Path tempDir;

    private Configuration configuration;
    private Renderer renderer;

    @BeforeEach
    void setUp() {
        configuration = new Configuration();
        renderer = new Renderer(configuration);
    }

    @Test
    @Tag("integration")
    void testCompleteReportGenerationWorkflow() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Integration Test Report");
        parameters.put("Author", "GenEx Integration Test");

        byte[] pdfData = renderer.render(
            "test-template.jrxml",
            "PDF",
            parameters,
            null
        );

        assertNotNull(pdfData, "PDF should be generated");
        assertTrue(pdfData.length > 0, "PDF should contain data");

        String header = new String(pdfData, 0, Math.min(5, pdfData.length));
        assertTrue(header.startsWith("%PDF"), "PDF should have a valid header");

        Path outputFile = tempDir.resolve("integration-test.pdf");
        Files.write(outputFile, pdfData);

        assertTrue(Files.exists(outputFile), "PDF should be written to the temp directory");
        assertEquals(pdfData.length, Files.size(outputFile), "Written file size should match rendered bytes");
    }

    @Test
    @Tag("integration")
    void testMultiFormatExport() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Multi-Format Test");
        parameters.put("Author", "GenEx");

        String[] formats = {"PDF", "XLSX", "XLS", "CSV", "TEXT"};

        for (String format : formats) {
            byte[] data = renderer.render(
                "test-template.jrxml",
                format,
                parameters,
                null
            );

            assertNotNull(data, "Format " + format + " sollte Daten erzeugen");
            assertTrue(data.length > 0, "Format " + format + " sollte nicht leer sein");

            Path outputFile = tempDir.resolve("test." + format.toLowerCase());
            Files.write(outputFile, data);
            assertTrue(Files.exists(outputFile));
        }
    }

    @Test
    @Tag("integration")
    void testCommandLineIntegration() throws Exception {
        Path outputFile = tempDir.resolve("cli-test.xlsx");
        Path configFile = tempDir.resolve("cli-test.conf");

        Files.writeString(configFile, """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=cli-test.xlsx
                report.format=XLSX
                report.parameter.ReportTitle=CLI Integration Test
                report.parameter.Author=Command Line
                report.timestamp.auto=false
                """.formatted(tempDir.toString()));

        String[] args = {"--config", configFile.toString()};

        Main main = new Main();
        int exitCode = main.run(args);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(outputFile));
    }

    @Test
    void testConfigurationIntegration() {
        Configuration config = new Configuration();

        assertNull(config.getString("db1.url"));
        assertNull(config.getDatabaseConfig("db1"));
    }

    @Test
    @Tag("integration")
    void testParameterTypeConversion() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Type Test");
        parameters.put("Count", 42);
        parameters.put("Price", 19.99);
        parameters.put("Active", true);

        byte[] pdfData = renderer.render(
            "test-template.jrxml",
            "PDF",
            parameters,
            null
        );

        assertNotNull(pdfData);
        assertTrue(pdfData.length > 0);
    }

    @Test
    @Tag("integration")
    void testMultipleRenderingWithoutCache() throws Exception {
        Map<String, Object> parameters = new HashMap<>();

        long startTime = System.currentTimeMillis();
        byte[] result1 = renderer.render("test-template.jrxml", "PDF", parameters, null);
        long firstDuration = System.currentTimeMillis() - startTime;

        assertNotNull(result1);
        assertTrue(result1.length > 0);

        startTime = System.currentTimeMillis();
        byte[] result2 = renderer.render("test-template.jrxml", "PDF", parameters, null);
        long secondDuration = System.currentTimeMillis() - startTime;

        assertNotNull(result2);
        assertTrue(result2.length > 0);
    }

    @Test
    void testErrorHandling() {
        assertThrows(Exception.class, () ->
            renderer.render("non-existent.jrxml", "PDF", null, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
            renderer.render("test-template.jrxml", "INVALID", null, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
            renderer.render(null, "PDF", null, null)
        );
    }
}
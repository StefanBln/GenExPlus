package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;
import io.github.stefanbln.genexplus.report.rendering.datasources.DataSourceProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for {@link Renderer}.
 */
class RendererTest {

    @TempDir
    Path testOutputDir;

    private Configuration configuration;
    private Renderer renderer;
    private DataSourceProvider provider;
    private Connection connection;

    @BeforeEach
    void setUp() {
        JasperRuntimeSupport.configureHeadlessDefaults();
        configuration = new Configuration();
        renderer = new Renderer(configuration);
        provider = new DataSourceProvider(configuration);
        connection = null;
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (provider != null) {
            provider.close();
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private Connection requireDatabaseConnection() throws SQLException {
        Assumptions.assumeTrue(DatabaseTestSupport.isPostgresAvailable(), "PostgreSQL not available");
        if (connection == null || connection.isClosed()) {
            connection = provider.getConnection("db1");
            assertNotNull(connection);
        }
        return connection;
    }

    private Path writeTestOutput(String filename, byte[] data) throws IOException {
        Path outputFile = testOutputDir.resolve(filename);
        Files.write(outputFile, data);
        return outputFile;
    }

    @Test
    @Tag("integration")
    void testRenderToStreamWritesValidPdf() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Stream Test");
        parameters.put("Author", "GenExPlus");

        Path outputFile = testOutputDir.resolve("stream-render.pdf");
        try (var out = Files.newOutputStream(outputFile)) {
            renderer.renderToStream("test-template.jrxml", "PDF", parameters, null, out);
        }

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
        byte[] header = Files.readAllBytes(outputFile);
        assertTrue(new String(header, 0, Math.min(4, header.length)).startsWith("%PDF"));
    }

    @Test
    @Tag("integration")
    void testRenderToStreamMatchesByteRender() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Parity Test");

        byte[] inMemory = renderer.render("test-template.jrxml", "PDF", parameters, null);

        Path outputFile = testOutputDir.resolve("parity-render.pdf");
        try (var out = Files.newOutputStream(outputFile)) {
            renderer.renderToStream("test-template.jrxml", "PDF", parameters, null, out);
        }

        assertArrayEquals(inMemory, Files.readAllBytes(outputFile));
    }

    @Test
    void testConstructorWithNullConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new Renderer(null));
    }

    @Test
    @Tag("integration")
    void testRenderWithTestTemplate() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Unit Test Report");
        parameters.put("Author", "Test Suite");

        byte[] result = renderer.render(
                "test-template.jrxml",
                "PDF",
                parameters,
                requireDatabaseConnection());

        assertNotNull(result);
        assertTrue(result.length > 0);

        String header = new String(result, 0, Math.min(5, result.length));
        assertTrue(header.startsWith("%PDF"));

        Path outputFile = writeTestOutput("test-render-pdf.pdf", result);
        assertTrue(Files.exists(outputFile));
        assertEquals(result.length, Files.size(outputFile));
    }

    @Test
    @Tag("integration")
    void testRenderXlsx() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Excel Test");

        byte[] result = renderer.render(
                "test-template.jrxml",
                "XLSX",
                parameters,
                requireDatabaseConnection());

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertTrue(result[0] == 0x50 && result[1] == 0x4B);

        Path outputFile = writeTestOutput("test-render.xlsx", result);
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
    }

    @Test
    @Tag("integration")
    void testRenderCsv() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "CSV Test");

        byte[] result = renderer.render(
                "test-template.jrxml",
                "CSV",
                parameters,
                requireDatabaseConnection());

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertFalse(new String(result).isBlank());

        Path outputFile = writeTestOutput("test-render.csv", result);
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
    }

    @Test
    void testRenderWithInvalidTemplate() {
        Map<String, Object> parameters = new HashMap<>();

        assertThrows(Exception.class, () -> renderer.render(
                "non-existent-template.jrxml",
                "PDF",
                parameters,
                null));
    }

    @Test
    void testRenderWithInvalidFormat() {
        Map<String, Object> parameters = new HashMap<>();

        var exception = assertThrows(IllegalArgumentException.class, () -> renderer.render(
                "test-template.jrxml",
                "INVALID",
                parameters,
                null));
        assertTrue(exception.getMessage().contains("INVALID"));
    }

    @Test
    void supportedFormatsIncludeCsvAndText() {
        assertTrue(Renderer.isSupportedFormat("CSV"));
        assertTrue(Renderer.isSupportedFormat("TEXT"));
        assertTrue(Renderer.isSupportedFormat("pdf"));
        assertFalse(Renderer.isSupportedFormat("INVALID"));
    }

    @Test
    void testRenderWithNullTemplate() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render(
                null,
                "PDF",
                new HashMap<>(),
                null));

        assertThrows(IllegalArgumentException.class, () -> renderer.render(
                "",
                "PDF",
                new HashMap<>(),
                null));
    }

    @Test
    @Tag("integration")
    void testRenderWithNullParameters() throws Exception {
        byte[] result = renderer.render(
                "test-template.jrxml",
                "PDF",
                null,
                null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @Tag("integration")
    void testTemplateRecompilationOnEveryRender() throws Exception {
        Map<String, Object> params = new HashMap<>();

        byte[] result1 = renderer.render("test-template.jrxml", "PDF", params, null);
        byte[] result2 = renderer.render("test-template.jrxml", "PDF", params, null);

        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.length > 0);
        assertTrue(result2.length > 0);
        assertEquals(result1.length, result2.length);
    }

    @Test
    void testRenderWithUnsupportedTemplateFormat() {
        Map<String, Object> parameters = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> renderer.render(
                "template.doc",
                "PDF",
                parameters,
                null));
    }

    @Test
    @Tag("integration")
    void testFormatCaseInsensitive() throws Exception {
        Map<String, Object> parameters = new HashMap<>();

        byte[] result1 = renderer.render(
                "test-template.jrxml",
                "pdf",
                parameters,
                requireDatabaseConnection());

        byte[] result2 = renderer.render(
                "test-template.jrxml",
                "Pdf",
                parameters,
                requireDatabaseConnection());

        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    @Tag("integration")
    void testXlsExport() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "XLS Test");

        byte[] result = renderer.render(
                "test-template.jrxml",
                "XLS",
                parameters,
                requireDatabaseConnection());

        assertNotNull(result);
        assertTrue(result.length > 0);

        Path outputFile = writeTestOutput("test-render.xls", result);
        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
    }
}

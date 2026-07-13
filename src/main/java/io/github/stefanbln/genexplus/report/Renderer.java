package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;
import io.github.stefanbln.genexplus.report.config.ExportSettings;
import io.github.stefanbln.genexplus.report.rendering.exporters.CsvExporter;
import io.github.stefanbln.genexplus.report.rendering.exporters.Exporter;
import io.github.stefanbln.genexplus.report.rendering.exporters.ExporterFactory;
import io.github.stefanbln.genexplus.report.rendering.exporters.TextExporter;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fill.JRFileVirtualizer;
import net.sf.jasperreports.engine.util.JRLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renders JasperReports templates into binary export formats.
 *
 * <p>The renderer compiles or loads a template, fills it with data from a JDBC connection or an
 * empty data source, and exports the result through a format-specific
 * {@link io.github.stefanbln.genexplus.report.rendering.exporters.Exporter}.
 *
 * <h2>Supported template types</h2>
 * <ul>
 *   <li>{@code .jrxml} — compiled on every run (no caching)</li>
 *   <li>{@code .jasper} — precompiled binary template</li>
 * </ul>
 *
 * <p>Templates may be loaded from the classpath or the filesystem. Locale and timezone defaults
 * come from {@code report.default.locale} and {@code report.default.timezone} in
 * {@link io.github.stefanbln.genexplus.report.config.Configuration}.
 *
 * <p>Use {@link #renderToStream} for file-target export; {@link #render} remains for callers that
 * need an in-memory {@code byte[]}.
 */
public final class Renderer {

    private static final Logger LOGGER = Logger.getLogger(Renderer.class.getName());
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_LOCALE = "en_US";
    private static final int DEFAULT_MAX_PAGES = 500;
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;
    private static final int DEFAULT_VIRTUALIZER_MAX_PAGES = 100;
    private static final int MILLISECONDS_PER_SECOND = 1_000;

    private final Configuration configuration;

    /**
     * Creates a renderer bound to the given application configuration.
     *
     * @param configuration shared application settings
     * @throws IllegalArgumentException if {@code configuration} is {@code null}
     */
    public Renderer(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
        this.configuration = configuration;
    }

    /**
     * Renders a report template into the requested export format.
     *
     * @param templatePath path or classpath resource to a {@code .jrxml} or {@code .jasper} file
     * @param format export format name (case-insensitive); see {@link ExporterFactory}
     * @param parameters JasperReports parameters merged with locale and timezone defaults
     * @param connection optional JDBC connection; when {@code null}, an empty data source is used
     * @return rendered report bytes
     * @throws JRException when JasperReports compilation, fill, or export fails
     * @throws Exception when the template cannot be loaded from disk or classpath
     */
    public byte[] render(String templatePath, String format, Map<String, Object> parameters,
            Connection connection) throws Exception {

        var buffer = new ByteArrayOutputStream();
        renderToStream(templatePath, format, parameters, connection, buffer,
                ExportSettings.from(new java.util.Properties(), configuration));
        return buffer.toByteArray();
    }

    /**
     * Renders a report template directly to an output stream without an intermediate {@code byte[]}.
     *
     * <p>Uses {@link ExportSettings} derived from application defaults. For per-job overrides,
     * call {@link #renderToStream(String, String, Map, Connection, OutputStream, ExportSettings)}.
     *
     * @param templatePath path or classpath resource to a {@code .jrxml} or {@code .jasper} file
     * @param format export format name (case-insensitive); see {@link ExporterFactory}
     * @param parameters JasperReports parameters merged with locale and timezone defaults
     * @param connection optional JDBC connection; when {@code null}, an empty data source is used
     * @param outputStream destination for exported bytes; not closed by this method
     * @throws JRException when JasperReports compilation, fill, or export fails
     * @throws Exception when the template cannot be loaded from disk or classpath
     */
    public void renderToStream(String templatePath, String format, Map<String, Object> parameters,
            Connection connection, OutputStream outputStream) throws Exception {
        renderToStream(templatePath, format, parameters, connection, outputStream,
                ExportSettings.from(new java.util.Properties(), configuration));
    }

    /**
     * Renders a report template directly to an output stream without an intermediate {@code byte[]}.
     *
     * @param templatePath path or classpath resource to a {@code .jrxml} or {@code .jasper} file
     * @param format export format name (case-insensitive); see {@link ExporterFactory}
     * @param parameters JasperReports parameters merged with locale and timezone defaults
     * @param connection optional JDBC connection; when {@code null}, an empty data source is used
     * @param outputStream destination for exported bytes; not closed by this method
     * @param exportSettings per-report export and governor overrides
     * @throws JRException when JasperReports compilation, fill, or export fails
     * @throws Exception when the template cannot be loaded from disk or classpath
     */
    public void renderToStream(String templatePath, String format, Map<String, Object> parameters,
            Connection connection, OutputStream outputStream, ExportSettings exportSettings) throws Exception {

        LOGGER.log(Level.FINE, "Template: {0}", templatePath);
        LOGGER.log(Level.FINE, "Format: {0}", format);

        validateFormat(format);

        var previousGovernorSettings = captureGovernorSettings();
        JRFileVirtualizer virtualizer = null;
        try {
            applyGovernorSettings(exportSettings);

            JasperReport report = loadTemplate(templatePath);
            Map<String, Object> finalParameters = prepareParameters(parameters);
            virtualizer = createVirtualizerIfEnabled();
            if (virtualizer != null) {
                finalParameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
                LOGGER.fine("Using JRFileVirtualizer for report fill phase");
            }

            JasperPrint jasperPrint = fillReport(report, finalParameters, connection);
            exportReport(jasperPrint, format, outputStream, exportSettings);
        } finally {
            if (virtualizer != null) {
                virtualizer.cleanup();
            }
            restoreGovernorSettings(previousGovernorSettings);
        }
    }

    private JRFileVirtualizer createVirtualizerIfEnabled() throws IOException {
        if (!configuration.getBoolean("report.virtualizer.enabled", false)) {
            return null;
        }

        int maxPages = configuration.getInt("report.virtualizer.maxPages", DEFAULT_VIRTUALIZER_MAX_PAGES);
        if (maxPages <= 0) {
            maxPages = DEFAULT_VIRTUALIZER_MAX_PAGES;
        }

        var directory = configuration.getString("report.virtualizer.directory");
        if (directory == null || directory.isBlank()) {
            return new JRFileVirtualizer(maxPages);
        }

        Path virtualizerDir = PathUtils.normalizeSafe(directory).toAbsolutePath().normalize();
        Files.createDirectories(virtualizerDir);
        return new JRFileVirtualizer(maxPages, virtualizerDir.toString());
    }

    private record GovernorSettings(String maxPages, String timeout) {}

    private GovernorSettings captureGovernorSettings() {
        return new GovernorSettings(
                System.getProperty("net.sf.jasperreports.governor.max.pages"),
                System.getProperty("net.sf.jasperreports.governor.timeout"));
    }

    private void restoreGovernorSettings(GovernorSettings previous) {
        restoreProperty("net.sf.jasperreports.governor.max.pages", previous.maxPages());
        restoreProperty("net.sf.jasperreports.governor.timeout", previous.timeout());
    }

    private static void restoreProperty(String key, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }

    /**
     * Applies JasperReports governor limits from configuration to protect against runaway reports.
     */
    private void applyGovernorSettings(ExportSettings exportSettings) {
        int maxPages = exportSettings.governorMaxPages() != null
                ? exportSettings.governorMaxPages()
                : configuration.getInt("report.governor.maxPages", DEFAULT_MAX_PAGES);
        if (maxPages > 0) {
            System.setProperty("net.sf.jasperreports.governor.max.pages", String.valueOf(maxPages));
        }

        int timeoutSeconds = exportSettings.governorTimeoutSeconds() != null
                ? exportSettings.governorTimeoutSeconds()
                : configuration.getInt("report.governor.timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        if (timeoutSeconds > 0) {
            System.setProperty("net.sf.jasperreports.governor.timeout",
                    String.valueOf(timeoutSeconds * MILLISECONDS_PER_SECOND));
            LOGGER.log(Level.FINE, "Governor timeout set to {0} seconds", timeoutSeconds);
        }
    }

    private JasperReport loadTemplate(String templatePath) throws Exception {
        if (templatePath == null || templatePath.isEmpty()) {
            throw new IllegalArgumentException("Template path must not be empty");
        }

        if (templatePath.endsWith(".jasper")) {
            return loadCompiledTemplate(templatePath);
        }
        if (templatePath.endsWith(".jrxml")) {
            return compileTemplate(templatePath);
        }
        throw new IllegalArgumentException("Unsupported template format: " + templatePath);
    }

    /**
     * Loads a precompiled {@code .jasper} template from the classpath or filesystem.
     */
    private JasperReport loadCompiledTemplate(String templatePath) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);
        if (is != null) {
            try {
                return (JasperReport) JRLoader.loadObject(is);
            } finally {
                is.close();
            }
        }

        try {
            var real = PathUtils.resolveExistingSecureFile(templatePath);
            return (JasperReport) JRLoader.loadObject(real.toFile());
        } catch (IOException | JRException e) {
            throw new JRException("Template not found: " + templatePath, e);
        }
    }

    /**
     * Compiles a {@code .jrxml} template from the classpath or filesystem.
     */
    private JasperReport compileTemplate(String templatePath) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);
        if (is != null) {
            try {
                return JasperCompileManager.compileReport(is);
            } catch (JRException e) {
                LOGGER.log(Level.SEVERE, "Failed to compile classpath template {0}: {1}",
                        new Object[]{templatePath, e.getMessage()});
                throw new JRException("Compilation failed: " + summarizeJasperMessage(e), e);
            } finally {
                is.close();
            }
        }

        try {
            var real = PathUtils.resolveExistingSecureFile(templatePath);
            return JasperCompileManager.compileReport(real.toString());
        } catch (JRException e) {
            LOGGER.log(Level.SEVERE, "Failed to compile filesystem template {0}: {1}",
                    new Object[]{templatePath, e.getMessage()});
            throw new JRException("Compilation failed: " + summarizeJasperMessage(e), e);
        } catch (IOException e) {
            throw new JRException("Template not found: " + templatePath, e);
        }
    }

    private static String summarizeJasperMessage(JRException exception) {
        var message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "see logs for details";
        }
        var firstLine = message.lines().findFirst().orElse(message);
        return firstLine.length() > 200 ? firstLine.substring(0, 200) + "..." : firstLine;
    }

    /**
     * Merges user parameters with locale and timezone defaults from configuration.
     */
    private Map<String, Object> prepareParameters(Map<String, Object> userParameters) {
        Map<String, Object> parameters = new HashMap<>();

        var timezoneId = configuration.getString("report.default.timezone", DEFAULT_TIMEZONE);
        var localeTag = configuration.getString("report.default.locale", DEFAULT_LOCALE);

        parameters.put(JRParameter.REPORT_TIME_ZONE, TimeZone.getTimeZone(timezoneId));
        parameters.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag(localeTag.replace('_', '-')));
        if (userParameters != null) {
            parameters.putAll(userParameters);
        }

        LOGGER.log(Level.FINE, "Prepared {0} report parameters", parameters.size());
        return parameters;
    }

    /**
     * Fills the report using either the JDBC connection or an empty data source.
     */
    private JasperPrint fillReport(JasperReport report, Map<String, Object> parameters,
            Connection connection) throws JRException {
        if (connection != null) {
            return JasperFillManager.fillReport(report, parameters, connection);
        }
        return JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
    }

    /**
     * Exports a filled report through the appropriate {@link Exporter} implementation.
     */
    private void exportReport(JasperPrint jasperPrint, String format, OutputStream outputStream,
            ExportSettings exportSettings) throws JRException {
        LOGGER.log(Level.INFO, "Exporting report as: {0}", format);

        Exporter exporter = resolveExporter(format, exportSettings);
        exporter.export(jasperPrint, outputStream);
        LOGGER.log(Level.INFO, "Report export completed for format: {0}", format);
    }

    private static Exporter resolveExporter(String format, ExportSettings exportSettings) {
        if ("CSV".equalsIgnoreCase(format)) {
            return new CsvExporter(exportSettings.csvDelimiter());
        }
        if ("TEXT".equalsIgnoreCase(format)) {
            return new TextExporter(exportSettings);
        }
        return ExporterFactory.getExporter(format);
    }

    private static void validateFormat(String format) {
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("Export format must not be empty");
        }
        if (!isSupportedFormat(format)) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    /**
     * Returns whether the given export format name is supported.
     *
     * @param format format name (case-insensitive)
     * @return {@code true} for PDF, Excel, CSV, TEXT, and aliases registered in
     *         {@link io.github.stefanbln.genexplus.report.rendering.exporters.ExporterFactory}
     */
    static boolean isSupportedFormat(String format) {
        if ("CSV".equalsIgnoreCase(format) || "TEXT".equalsIgnoreCase(format)) {
            return true;
        }
        return ExporterFactory.isSupported(format);
    }
}

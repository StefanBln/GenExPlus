package io.github.stefanbln.genexplus.report.config;

import io.github.stefanbln.genexplus.report.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

/**
 * Immutable per-report job configuration loaded from a {@code .conf} properties file.
 *
 * <p>Each report job has its own {@code .conf} file describing which template to render,
 * where to write the output, which export format to use, and optional email delivery settings.
 * Shared resources such as database connections are referenced by ID and resolved through
 * {@link Configuration}.
 *
 * <h2>Required keys</h2>
 * <ul>
 *   <li>{@code database.id} — database ID from {@code application.properties} (omit when
 *       {@code report.database.optional=true} and no JDBC connection is needed)</li>
 *   <li>{@code report.template} — filesystem path or classpath resource</li>
 *   <li>{@code report.output.dir} — output directory</li>
 *   <li>{@code report.output.filename} — output filename without directory</li>
 * </ul>
 *
 * <h2>Optional keys</h2>
 * <ul>
 *   <li>{@code report.format} — export format (default: {@code PDF}, or {@code report.default.format}
 *       from {@code application.properties})</li>
 *   <li>{@code report.csv.delimiter} — CSV column delimiter (default: {@code ;})</li>
 *   <li>{@code report.governor.maxPages}, {@code report.governor.timeoutSeconds} — per-job overrides</li>
 *   <li>{@code report.text.pageWidthChars}, {@code report.text.pageHeightChars}, … — TEXT export layout</li>
 *   <li>{@code report.parameter.<name>} — JasperReports parameters</li>
 *   <li>{@code report.timestamp.auto} — append timestamp suffix (default: {@code true})</li>
 *   <li>{@code report.timestamp.pattern} — timestamp format (default: {@code yyyyMMdd_HHmmss})</li>
 *   <li>{@code report.email.enabled}, {@code report.email.to}, {@code report.email.subject}, …</li>
 *   <li>{@code report.email.body.plain} — optional plain-text body (derived from HTML when omitted)</li>
 *   <li>{@code report.email.body.escape} — escape HTML entities in the body (default: {@code false})</li>
 *   <li>{@code report.database.optional} — allow reports without {@code database.id} (default:
 *       {@code false}); when an ID is set, its JDBC settings must still be configured</li>
 *   <li>{@code verbose} — enable fine logging for this job</li>
 * </ul>
 */
public final class ReportConfig {
    private static final Logger LOGGER = Logger.getLogger(ReportConfig.class.getName());

    private final String databaseId;
    private final String template;
    private final String outputDir;
    private final String outputFilename;
    private final String format;
    private final Map<String, Object> parameters;
    private final boolean verbose;
    private final boolean autoTimestamp;
    private final String timestampPattern;
    private final boolean emailEnabled;
    private final List<String> emailTo;
    private final List<String> emailCc;
    private final List<String> emailBcc;
    private final String emailSubject;
    private final String emailBody;
    private final String emailBodyPlain;
    private final boolean emailBodyEscape;
    private final boolean databaseOptional;
    private final ExportSettings exportSettings;

    private ReportConfig(String databaseId, String template, String outputDir, String outputFilename,
                        String format, Map<String, Object> parameters, boolean verbose,
                        boolean autoTimestamp, String timestampPattern,
                        boolean emailEnabled, List<String> emailTo, List<String> emailCc,
                        List<String> emailBcc, String emailSubject, String emailBody,
                        String emailBodyPlain, boolean emailBodyEscape, boolean databaseOptional,
                        ExportSettings exportSettings) {
        this.databaseId = databaseId;
        this.template = template;
        this.outputDir = outputDir;
        this.outputFilename = outputFilename;
        this.format = format;
        this.parameters = Map.copyOf(parameters);
        this.verbose = verbose;
        this.autoTimestamp = autoTimestamp;
        this.timestampPattern = timestampPattern;
        this.emailEnabled = emailEnabled;
        this.emailTo = List.copyOf(emailTo);
        this.emailCc = List.copyOf(emailCc);
        this.emailBcc = List.copyOf(emailBcc);
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;
        this.emailBodyPlain = emailBodyPlain;
        this.emailBodyEscape = emailBodyEscape;
        this.databaseOptional = databaseOptional;
        this.exportSettings = exportSettings;
    }

    /**
     * Loads a report configuration from a properties file.
     *
     * @param configPath path to the {@code .conf} file
     * @return parsed immutable configuration
     * @throws IOException if the file is missing or required keys are absent
     */
    public static ReportConfig load(String configPath) throws IOException {
        return load(configPath, null);
    }

    /**
     * Loads a report configuration using shared application defaults when provided.
     *
     * @param configPath path to the {@code .conf} file
     * @param appConfig shared application settings for format and export defaults
     * @return parsed immutable configuration
     * @throws IOException if the file is missing or required keys are absent
     */
    public static ReportConfig load(String configPath, Configuration appConfig) throws IOException {
        var path = PathUtils.resolveExistingSecureFile(configPath);

        var props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        }

        var databaseOptional = Boolean.parseBoolean(props.getProperty("report.database.optional", "false"));
        var databaseIdRaw = props.getProperty("database.id", "");
        final var databaseId = databaseIdRaw != null ? databaseIdRaw.trim() : "";
        // database.id may be omitted when report.database.optional=true or when inferred from the template adapter name

        var template = getRequired(props, "report.template");
        var outputDir = getRequired(props, "report.output.dir");
        var outputFilename = getRequired(props, "report.output.filename");
        var formatProperty = props.getProperty("report.format");
        var format = formatProperty != null && !formatProperty.isBlank()
                ? formatProperty.toUpperCase()
                : resolveDefaultFormat(appConfig);

        var exportSettings = ExportSettings.from(props, appConfig != null ? appConfig : emptyDefaults());

        var parameters = new HashMap<String, Object>();
        props.stringPropertyNames().stream()
                .filter(key -> key.startsWith("report.parameter."))
                .forEach(key -> {
                    var paramName = key.substring("report.parameter.".length());
                    var value = props.getProperty(key);
                    parameters.put(paramName, parseValue(value));
                });

        var verbose = Boolean.parseBoolean(props.getProperty("verbose", "false"));
        var autoTimestamp = Boolean.parseBoolean(props.getProperty("report.timestamp.auto", "true"));
        var timestampPattern = props.getProperty("report.timestamp.pattern", "yyyyMMdd_HHmmss");

        var emailEnabled = Boolean.parseBoolean(props.getProperty("report.email.enabled", "false"));
        var emailTo = parseEmailList(props.getProperty("report.email.to", ""));
        var emailCc = parseEmailList(props.getProperty("report.email.cc", ""));
        var emailBcc = parseEmailList(props.getProperty("report.email.bcc", ""));
        var emailSubject = props.getProperty("report.email.subject", "Report: " + outputFilename);
        var emailBody = props.getProperty("report.email.body", "Please find the report attached.");
        var emailBodyPlain = props.getProperty("report.email.body.plain", "");
        var emailBodyEscape = Boolean.parseBoolean(props.getProperty("report.email.body.escape", "false"));

        LOGGER.fine(() -> String.format(
                "Loaded report config: DB=%s, Template=%s, OutputDir=%s, Filename=%s, Format=%s, Params=%d, AutoTimestamp=%s, EmailEnabled=%s, DatabaseOptional=%s",
                databaseId.isBlank() ? "(none)" : databaseId, template, outputDir, outputFilename, format,
                parameters.size(), autoTimestamp, emailEnabled, databaseOptional));

        return new ReportConfig(databaseId, template, outputDir, outputFilename, format, parameters, verbose,
                autoTimestamp, timestampPattern, emailEnabled, emailTo, emailCc, emailBcc, emailSubject, emailBody,
                emailBodyPlain, emailBodyEscape, databaseOptional, exportSettings);
    }

    private static String resolveDefaultFormat(Configuration appConfig) {
        if (appConfig == null) {
            return "PDF";
        }
        var configured = appConfig.getString("report.default.format", "PDF");
        return configured != null && !configured.isBlank() ? configured.toUpperCase() : "PDF";
    }

    private static Configuration emptyDefaults() {
        try {
            return new Configuration("");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create empty configuration defaults", e);
        }
    }

    /**
     * Parses a comma-separated list of email addresses.
     */
    private static List<String> parseEmailList(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String getRequired(Properties props, String key) throws IOException {
        var value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IOException("Required configuration missing: " + key);
        }
        return value.trim();
    }

    /**
     * Coerces a property value to {@link Boolean}, {@link Integer}, {@link Double}, or
     * {@link String} when possible.
     */
    private static Object parseValue(String value) {
        if (value == null) {
            return "";
        }

        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.valueOf(value);
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            // not an integer
        }

        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            // not a double
        }

        return value;
    }

    /** Database ID referenced in {@code application.properties}. */
    public String getDatabaseId() {
        return databaseId;
    }

    /** Template path or classpath resource ({@code .jrxml} or {@code .jasper}). */
    public String getTemplate() {
        return template;
    }

    /** Absolute or relative output directory. */
    public String getOutputDir() {
        return outputDir;
    }

    /** Output filename without directory component. */
    public String getOutputFilename() {
        return outputFilename;
    }

    /** Export format in upper case (for example {@code PDF}, {@code XLSX}). */
    public String getFormat() {
        return format;
    }

    /** JasperReports parameters passed to the template. */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /** Whether fine-grained logging is enabled for this job. */
    public boolean isVerbose() {
        return verbose;
    }

    /** Whether a timestamp suffix is appended to the output filename. */
    public boolean isAutoTimestamp() {
        return autoTimestamp;
    }

    /** {@link java.time.format.DateTimeFormatter} pattern for automatic timestamps. */
    public String getTimestampPattern() {
        return timestampPattern;
    }

    /** Whether email delivery is requested for this job. */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    /** Primary email recipients. */
    public List<String> getEmailTo() {
        return emailTo;
    }

    /** CC recipients. */
    public List<String> getEmailCc() {
        return emailCc;
    }

    /** BCC recipients. */
    public List<String> getEmailBcc() {
        return emailBcc;
    }

    /** Email subject line. */
    public String getEmailSubject() {
        return emailSubject;
    }

    /** Email HTML body. */
    public String getEmailBody() {
        return emailBody;
    }

    /** Optional plain-text email body; empty when derived from {@link #getEmailBody()}. */
    public String getEmailBodyPlain() {
        return emailBodyPlain;
    }

    /** When {@code true}, HTML special characters in the email body are escaped before sending. */
    public boolean isEmailBodyEscape() {
        return emailBodyEscape;
    }

    /**
     * When {@code true}, a missing or unconfigured {@code database.id} uses an empty data source
     * instead of failing validation.
     */
    public boolean isDatabaseOptional() {
        return databaseOptional;
    }

    /** Per-report export and governor overrides. */
    public ExportSettings getExportSettings() {
        return exportSettings;
    }

    @Override
    public String toString() {
        return "ReportConfig{" +
                "databaseId='" + databaseId + '\'' +
                ", template='" + template + '\'' +
                ", outputDir='" + outputDir + '\'' +
                ", outputFilename='" + outputFilename + '\'' +
                ", format='" + format + '\'' +
                ", parameters=" + parameters.size() +
                ", verbose=" + verbose +
                ", autoTimestamp=" + autoTimestamp +
                ", timestampPattern='" + timestampPattern + '\'' +
                ", emailEnabled=" + emailEnabled +
                ", emailTo=" + emailTo.size() +
                '}';
    }
}

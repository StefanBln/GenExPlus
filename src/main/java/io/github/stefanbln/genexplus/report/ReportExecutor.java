package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;
import io.github.stefanbln.genexplus.report.config.ReportConfig;
import io.github.stefanbln.genexplus.report.delivery.EmailConfig;
import io.github.stefanbln.genexplus.report.delivery.EmailDeliveryService;
import io.github.stefanbln.genexplus.report.delivery.EmailMessage;
import io.github.stefanbln.genexplus.report.rendering.datasources.DatabaseProfileResolver;
import io.github.stefanbln.genexplus.report.rendering.datasources.DataSourceProvider;
import io.github.stefanbln.genexplus.report.signing.PdfSigningService;
import io.github.stefanbln.genexplus.report.signing.SigningConfig;
import io.github.stefanbln.genexplus.report.signing.SigningException;

import net.sf.jasperreports.engine.JRException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.mail.MessagingException;

/**
 * Orchestrates a single report generation run.
 *
 * <p>{@link #execute(io.github.stefanbln.genexplus.report.ArgumentParser.Arguments)} is the main pipeline:
 * load configuration, validate, render to a temp file, optionally sign PDF output, atomically
 * move to the final path, and optionally send email.
 *
 * <h2>Pipeline stages</h2>
 * <ol>
 *   <li>Load {@link io.github.stefanbln.genexplus.report.config.Configuration}</li>
 *   <li>Load {@link io.github.stefanbln.genexplus.report.config.ReportConfig} with application format defaults</li>
 *   <li>Validate via {@link io.github.stefanbln.genexplus.report.ReportConfigValidator}</li>
 *   <li>Render through {@link io.github.stefanbln.genexplus.report.Renderer} using
 *       {@link io.github.stefanbln.genexplus.report.config.ExportSettings} from the job file</li>
 *   <li>Sign when {@code signing.enabled} and format is PDF</li>
 *   <li>Email when {@code report.email.enabled}; honour {@code GENEXPLUS_STRICT_EMAIL}</li>
 * </ol>
 *
 * <p>Temp files are always cleaned up in {@code finally} blocks; failed renders do not leave
 * partial output at the destination path.
 *
 * @see io.github.stefanbln.genexplus.report.Main
 * @see io.github.stefanbln.genexplus.report.ExitCodes
 */
public final class ReportExecutor {

    private static final Logger LOGGER = Logger.getLogger(ReportExecutor.class.getName());

    private final ReportConfigValidator validator;
    private final OutputPathResolver outputPathResolver;
    private final PdfSigningService pdfSigningService;

    public ReportExecutor() {
        this(new ReportConfigValidator(ReportExecutor.class.getClassLoader()),
                new OutputPathResolver(),
                new PdfSigningService());
    }

    ReportExecutor(ReportConfigValidator validator, OutputPathResolver outputPathResolver,
            PdfSigningService pdfSigningService) {
        this.validator = validator;
        this.outputPathResolver = outputPathResolver;
        this.pdfSigningService = pdfSigningService;
    }

    /**
     * Executes report generation for the given command-line arguments.
     *
     * @return a process exit code; see {@link ExitCodes}
     */
    public int execute(ArgumentParser.Arguments args) {
        final Configuration configuration;
        try {
            configuration = new Configuration(args.propertiesFile());
        } catch (IOException e) {
            System.err.println("Configuration error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Configuration error", e);
            return ExitCodes.CONFIG_ERROR;
        }

        final ReportConfig reportConfig;
        try {
            reportConfig = loadReportConfig(args.configFile(), configuration);
        } catch (IOException e) {
            System.err.println("Validation error: " + e.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        }

        try {
            LoggingConfigurer.configure(args.verbose() || reportConfig.isVerbose());

            if (!validator.validate(reportConfig, configuration)) {
                return ExitCodes.VALIDATION_ERROR;
            }

            var outputPath = outputPathResolver.resolve(reportConfig);
            generateAndWriteReport(reportConfig, configuration, outputPath);

            if (!deliverEmail(reportConfig, configuration, outputPath)) {
                return ExitCodes.EMAIL_ERROR;
            }

            return ExitCodes.SUCCESS;

        } catch (ValidationException e) {
            System.err.println("Validation error: " + e.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (InvalidPathException e) {
            System.err.println("Validation error: " + e.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (IOException e) {
            System.err.println("Unexpected error during report generation: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Unexpected I/O error during report generation", e);
            return ExitCodes.RENDER_ERROR;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Database error", e);
            return ExitCodes.DATABASE_ERROR;
        } catch (JRException e) {
            System.err.println("Render error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Render error", e);
            return ExitCodes.RENDER_ERROR;
        } catch (SigningException e) {
            System.err.println("Signing error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Signing error", e);
            return ExitCodes.RENDER_ERROR;
        } catch (Exception e) {
            System.err.println("Unexpected error during report generation: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Unexpected error during report generation", e);
            return ExitCodes.RENDER_ERROR;
        }
    }

    private ReportConfig loadReportConfig(String configFile, Configuration configuration) throws IOException {
        var reportConfig = ReportConfig.load(configFile, configuration);
        LOGGER.log(Level.INFO, "Loaded report configuration: {0}", configFile);
        LOGGER.fine(reportConfig.toString());
        return reportConfig;
    }

    private void generateAndWriteReport(ReportConfig reportConfig, Configuration configuration,
            Path outputPath) throws Exception {
        LOGGER.info("Generating report...");
        LOGGER.log(Level.FINE, "Template: {0}", reportConfig.getTemplate());
        LOGGER.log(Level.FINE, "Format: {0}", reportConfig.getFormat());
        LOGGER.log(Level.FINE, "Database profile: {0}", reportConfig.getDatabaseId());

        if (Files.exists(outputPath)) {
            LOGGER.log(Level.WARNING, "Overwriting existing output file: {0}", outputPath);
        }

        var parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        var writeDirectory = parent != null ? parent : Path.of("").toAbsolutePath();
        var tempFile = Files.createTempFile(writeDirectory, ".genexplus-", tempSuffixForFormat(reportConfig.getFormat()));

        try (var dsProvider = new DataSourceProvider(configuration)) {
            var renderer = new Renderer(configuration);
            var connection = getConnection(configuration, dsProvider, reportConfig);

            try (OutputStream out = new BufferedOutputStream(
                    Files.newOutputStream(tempFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
                renderer.renderToStream(
                        reportConfig.getTemplate(),
                        reportConfig.getFormat(),
                        reportConfig.getParameters(),
                        connection,
                        out,
                        reportConfig.getExportSettings());
            }

            if (configuration.isSigningEnabled() && "PDF".equalsIgnoreCase(reportConfig.getFormat())) {
                var signedTemp = Files.createTempFile(writeDirectory, ".genexplus-", ".signed.pdf");
                try {
                    var signingConfig = SigningConfig.from(configuration);
                    pdfSigningService.sign(tempFile, signedTemp, signingConfig);
                    moveTempFileToOutput(signedTemp, outputPath);
                } finally {
                    Files.deleteIfExists(signedTemp);
                }
            } else {
                moveTempFileToOutput(tempFile, outputPath);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }

        LOGGER.log(Level.INFO, "Report generated successfully: {0}", outputPath);
    }

    private static void moveTempFileToOutput(Path tempFile, Path outputPath) throws IOException {
        try {
            Files.move(tempFile, outputPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tempFile, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String tempSuffixForFormat(String format) {
        return switch (format.toUpperCase()) {
            case "PDF" -> ".pdf";
            case "XLSX" -> ".xlsx";
            case "XLS" -> ".xls";
            case "CSV" -> ".csv";
            case "TEXT" -> ".txt";
            default -> ".tmp";
        };
    }

    private Connection getConnection(Configuration configuration, DataSourceProvider dsProvider,
            ReportConfig reportConfig) throws SQLException {
        var resolver = new DatabaseProfileResolver(ReportExecutor.class.getClassLoader(), configuration);
        var profile = resolver.resolve(reportConfig, configuration);
        if (!profile.requiresConnection()) {
            return null;
        }

        if (!profile.hasResolvableJdbcUrl()) {
            throw new SQLException("Database '" + profile.profileId() + "' is referenced but not configured");
        }

        return dsProvider.getConnection(profile.jdbc(), profile.profileId());
    }

    /**
     * Sends the report by email when enabled.
     *
     * @return {@code true} when email is disabled or delivery succeeds
     */
    private boolean deliverEmail(ReportConfig reportConfig, Configuration configuration, Path outputPath) {
        if (!reportConfig.isEmailEnabled()) {
            return true;
        }

        try {
            sendReportByEmail(reportConfig, configuration, outputPath);
            return true;
        } catch (IllegalArgumentException | IllegalStateException | MessagingException | IOException e) {
            System.err.println("Email delivery failed: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Email delivery failed", e);
            System.err.println("Report was written to disk, but email delivery did not succeed.");
            if (!isStrictEmailFailure(System.getenv("GENEXPLUS_STRICT_EMAIL"))) {
                LOGGER.warning("GENEXPLUS_STRICT_EMAIL=false — treating email failure as success for exit code");
                return true;
            }
            return false;
        }
    }

    static boolean isStrictEmailFailure(String envValue) {
        if (envValue == null || envValue.isBlank()) {
            return true;
        }
        var normalized = envValue.trim();
        return !"false".equalsIgnoreCase(normalized) && !"0".equals(normalized);
    }

    private void sendReportByEmail(ReportConfig reportConfig, Configuration configuration, Path reportFile)
            throws MessagingException, IOException {
        var emailConfig = EmailConfig.from(configuration);
        var emailService = new EmailDeliveryService(emailConfig);

        var bodyBuilder = EmailMessage.builder()
                .to(reportConfig.getEmailTo())
                .cc(reportConfig.getEmailCc())
                .bcc(reportConfig.getEmailBcc())
                .subject(reportConfig.getEmailSubject());
        if (reportConfig.isEmailBodyEscape()) {
            bodyBuilder.bodyEscaped(reportConfig.getEmailBody());
        } else {
            bodyBuilder.body(reportConfig.getEmailBody());
        }
        var message = bodyBuilder
                .bodyPlain(reportConfig.getEmailBodyPlain())
                .attachment(reportFile)
                .build();

        emailService.send(message);
        LOGGER.info("Report email sent successfully");
    }
}

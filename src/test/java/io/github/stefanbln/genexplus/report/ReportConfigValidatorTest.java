package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;
import io.github.stefanbln.genexplus.report.config.ReportConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.stefanbln.genexplus.report.TestResources.testKeystorePath;
import static org.junit.jupiter.api.Assertions.*;

class ReportConfigValidatorTest {

    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    private ReportConfigValidator validator;
    private Configuration appConfig;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        System.setErr(new PrintStream(errContent));
        validator = new ReportConfigValidator(getClass().getClassLoader());
        appConfig = new Configuration("");
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
    }

    @Test
    void validConfigPasses() throws IOException {
        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=valid-report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertTrue(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().isEmpty());
    }

    @Test
    void bundledSampleTemplatePasses() throws IOException {
        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=sample-report.jrxml
                report.output.dir=%s
                report.output.filename=sample-report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertTrue(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().isEmpty());
    }

    @Test
    void unsupportedFormatFailsWithMessage() throws IOException {
        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.xyz
                report.format=UNKNOWN
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        var errors = errContent.toString();
        assertTrue(errors.contains("Unsupported format: UNKNOWN"));
        assertTrue(errors.contains("Supported formats:"));
    }

    @Test
    void missingTemplateFileFails() throws IOException {
        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=nonexistent-template.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Template file not found: nonexistent-template.jrxml"));
    }

    @Test
    void emailEnabledButSmtpDisabledFails() throws IOException {
        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                report.email.enabled=true
                report.email.to=recipient@example.com
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("mail.smtp.enabled=true required"));
    }

    @Test
    void emailEnabledButEmptyRecipientsFails() throws IOException {
        configureSmtp(appConfig);

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                report.email.enabled=true
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Email delivery is enabled but report.email.to is empty"));
    }

    @Test
    void emailEnabledWithInvalidRecipientFails() throws IOException {
        configureSmtp(appConfig);

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                report.email.enabled=true
                report.email.to=not-an-email
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Invalid email address in report.email.to"));
    }

    @Test
    void emailEnabledWithoutSmtpHostFails() throws IOException {
        appConfig.setProperty("mail.smtp.enabled", "true");
        appConfig.setProperty("mail.smtp.from", "sender@example.com");

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                report.email.enabled=true
                report.email.to=recipient@example.com
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("mail.smtp.host"));
    }

    @Test
    void emailEnabledWithoutAuthCredentialsFails() throws IOException {
        configureSmtp(appConfig);
        appConfig.setProperty("mail.smtp.auth", "true");

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                report.email.enabled=true
                report.email.to=recipient@example.com
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        var errors = errContent.toString();
        assertTrue(errors.contains("mail.smtp.auth=true"));
    }

    @Test
    void pdfWithValidSigningConfigurationPasses() throws Exception {
        appConfig.setProperty("signing.enabled", "true");
        appConfig.setProperty("signing.keystore.path", testKeystorePath().toString());
        appConfig.setProperty("signing.keystore.type", "PKCS12");
        appConfig.setProperty("signing.keystore.alias", "testalias");
        appConfig.setProperty("signing.keystore.password", "testpass");

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertTrue(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().isEmpty());
    }

    @Test
    void signingEnabledForNonPdfFormatFails() throws Exception {
        appConfig.setProperty("signing.enabled", "true");
        appConfig.setProperty("signing.keystore.path", testKeystorePath().toString());
        appConfig.setProperty("signing.keystore.alias", "testalias");
        appConfig.setProperty("signing.keystore.password", "testpass");

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.xlsx
                report.format=XLSX
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("only PDF is supported"));
    }

    @Test
    void signingEnabledWithoutKeystorePathFails() throws Exception {
        appConfig.setProperty("signing.enabled", "true");

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("signing.keystore.path"));
    }
    @Test
    void databaseReferencedButNotConfiguredFails() throws IOException {
        var reportConfig = loadConfig("""
                database.id=db99
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        var errors = errContent.toString();
        assertTrue(errors.contains("Database 'db99' is referenced but not configured"));
        assertTrue(errors.contains("db99.username"));
    }

    @Test
    void databaseOptionalAllowsMissingDatabaseIdForDbLessTemplate() throws IOException {
        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertTrue(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().isEmpty());
    }

    @Test
    void databaseOptionalRejectsSqlTemplateWithoutDatabaseId() throws IOException {
        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=test-template-sql-no-adapter.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Template contains SQL but no database profile"));
    }

    @Test
    void infersDatabaseProfileFromTemplateWhenIdOmitted() throws IOException {
        var propsFile = tempDir.resolve("app.properties");
        Files.writeString(propsFile, """
                db1.url=jdbc:postgresql://localhost:5432/postgres
                db1.username=postgres
                """);
        var configuredApp = new Configuration(propsFile.toString());

        var reportConfig = loadConfig("""
                report.database.optional=false
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertTrue(validator.validate(reportConfig, configuredApp));
    }

    @Test
    void warnsWhenStudioAdapterDiffersFromDatabaseId() throws IOException {
        var propsFile = tempDir.resolve("app.properties");
        Files.writeString(propsFile, """
                db1.url=jdbc:postgresql://localhost:5432/postgres
                db1.username=postgres
                """);
        var configuredApp = new Configuration(propsFile.toString());

        var reportConfig = loadConfig("""
                database.id=db1
                report.template=test-template-studio-db2.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertTrue(validator.validate(reportConfig, configuredApp));
        assertTrue(errContent.toString().contains("defaultdataadapter='db2'"));
    }

    @Test
    void databaseOptionalWithConfiguredIdButMissingDbConfigFails() throws IOException {
        var reportConfig = loadConfig("""
                database.id=db99
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Database 'db99' is referenced but not configured"));
    }

    @Test
    void compiledJasperRequiresExplicitDatabaseId() throws IOException {
        var jasper = tempDir.resolve("compiled.jasper");
        Files.writeString(jasper, "not-a-real-jasper");

        var reportConfig = loadConfig("""
                report.database.optional=false
                report.template=%s
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(jasper, tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Compiled .jasper templates require explicit database.id"));
    }

    @Test
    void invalidLocaleFails() throws IOException {
        appConfig.setProperty("report.default.locale", "!!!");

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Invalid report.default.locale"));
    }

    @Test
    void invalidTimezoneFails() throws IOException {
        appConfig.setProperty("report.default.timezone", "Not/A/Timezone");

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Invalid report.default.timezone"));
    }

    @Test
    void invalidOutputDirectoryFails() throws IOException {
        Files.writeString(tempDir.resolve("existing-file.txt"), "not a directory");

        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir.resolve("existing-file.txt")));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Invalid output directory"));
    }

    private static void configureSmtp(Configuration appConfig) {
        appConfig.setProperty("mail.smtp.enabled", "true");
        appConfig.setProperty("mail.smtp.host", "smtp.example.com");
        appConfig.setProperty("mail.smtp.from", "sender@example.com");
        appConfig.setProperty("mail.smtp.auth", "false");
    }

    @Test
    void invalidTimestampPatternFails() throws IOException {
        var reportConfig = loadConfig("""
                report.database.optional=true
                report.template=simple.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                report.timestamp.auto=true
                report.timestamp.pattern=not-a-valid-pattern-[[[
                """.formatted(tempDir));

        assertFalse(validator.validate(reportConfig, appConfig));
        assertTrue(errContent.toString().contains("Invalid report.timestamp.pattern"));
    }

    private ReportConfig loadConfig(String content) throws IOException {
        var configFile = tempDir.resolve("report.conf");
        Files.writeString(configFile, content);
        return ReportConfig.load(configFile.toString());
    }
}

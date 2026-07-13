package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ReportExecutor} orchestration and exit-code semantics.
 */
class ReportExecutorTest {

    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
    }

    @Test
    @Tag("integration")
    void emailDeliveryFailureReturnsExitCodeOne() throws IOException {
        var propertiesFile = tempDir.resolve("application.properties");
        Files.writeString(propertiesFile, """
                mail.smtp.enabled=true
                mail.smtp.host=127.0.0.1
                mail.smtp.port=1
                mail.smtp.from=sender@example.com
                mail.smtp.auth=false
                mail.smtp.starttls.enable=false
                """);

        var configFile = tempDir.resolve("email-fail.conf");
        Files.writeString(configFile, """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=email-fail.pdf
                report.format=PDF
                report.parameter.ReportTitle=Email Failure Test
                report.parameter.Author=GenExPlus
                report.timestamp.auto=false
                report.email.enabled=true
                report.email.to=recipient@example.com
                report.email.subject=Test
                report.email.body=Test body
                """.formatted(tempDir));

        var args = new ArgumentParser.Arguments(configFile.toString(), propertiesFile.toString(), false);
        int exitCode = new ReportExecutor().execute(args);

        assertEquals(ExitCodes.EMAIL_ERROR, exitCode);
        assertTrue(Files.exists(tempDir.resolve("email-fail.pdf")));
        assertTrue(errContent.toString().contains("Email delivery failed"));
    }

    @Test
    void strictEmailFailurePolicy() {
        assertTrue(ReportExecutor.isStrictEmailFailure(null));
        assertTrue(ReportExecutor.isStrictEmailFailure(""));
        assertTrue(ReportExecutor.isStrictEmailFailure("true"));
        assertFalse(ReportExecutor.isStrictEmailFailure("false"));
        assertFalse(ReportExecutor.isStrictEmailFailure("0"));
    }

    @Test
    @Tag("integration")
    void successfulRunWithoutEmailReturnsExitCodeZero() throws IOException {
        var configFile = tempDir.resolve("success.conf");
        Files.writeString(configFile, """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=success.pdf
                report.format=PDF
                report.parameter.ReportTitle=Success Test
                report.parameter.Author=GenExPlus
                report.timestamp.auto=false
                """.formatted(tempDir));

        var args = new ArgumentParser.Arguments(configFile.toString(), null, false);
        int exitCode = new ReportExecutor().execute(args);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(tempDir.resolve("success.pdf")));
    }

    @Test
    @Tag("integration")
    void overwritesExistingOutputFile() throws IOException {
        var configFile = tempDir.resolve("overwrite.conf");
        Files.writeString(configFile, """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=overwrite.pdf
                report.format=PDF
                report.parameter.ReportTitle=Overwrite Test
                report.parameter.Author=GenExPlus
                report.timestamp.auto=false
                """.formatted(tempDir));

        var args = new ArgumentParser.Arguments(configFile.toString(), null, false);
        var executor = new ReportExecutor();

        assertEquals(ExitCodes.SUCCESS, executor.execute(args));
        assertEquals(ExitCodes.SUCCESS, executor.execute(args));
        assertTrue(Files.exists(tempDir.resolve("overwrite.pdf")));
        assertTrue(Files.size(tempDir.resolve("overwrite.pdf")) > 0);
    }

    @Test
    void validationFailureReturnsExitCodeOne() throws IOException {
        var configFile = tempDir.resolve("invalid.conf");
        Files.writeString(configFile, """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=UNKNOWN
                """.formatted(tempDir));

        var args = new ArgumentParser.Arguments(configFile.toString(), null, false);
        assertEquals(ExitCodes.VALIDATION_ERROR, new ReportExecutor().execute(args));
    }

    @Test
    void missingPropertiesFileReturnsConfigError() throws IOException {
        var configFile = tempDir.resolve("valid.conf");
        Files.writeString(configFile, """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=report.pdf
                report.format=PDF
                """.formatted(tempDir));

        var missingProperties = tempDir.resolve("missing-application.properties");
        var args = new ArgumentParser.Arguments(configFile.toString(), missingProperties.toString(), false);

        assertEquals(ExitCodes.CONFIG_ERROR, new ReportExecutor().execute(args));
        assertTrue(errContent.toString().contains("Configuration error"));
    }

    @Test
    @Tag("integration")
    void compilationFailureReturnsRenderError() throws IOException {
        var brokenTemplate = tempDir.resolve("broken.jrxml");
        Files.writeString(brokenTemplate, "<jasperReport><broken></jasperReport>");

        var configFile = tempDir.resolve("broken.conf");
        Files.writeString(configFile, """
                report.database.optional=true
                report.template=%s
                report.output.dir=%s
                report.output.filename=broken.pdf
                report.format=PDF
                report.timestamp.auto=false
                """.formatted(brokenTemplate, tempDir));

        var args = new ArgumentParser.Arguments(configFile.toString(), null, false);
        assertEquals(ExitCodes.RENDER_ERROR, new ReportExecutor().execute(args));
        var err = errContent.toString();
        assertTrue(err.contains("Render error") || err.contains("Unexpected error during report generation"),
                () -> "stderr should describe render failure but was: " + err);
    }
}

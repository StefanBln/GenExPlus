package io.github.stefanbln.genexplus.report.delivery;

import io.github.stefanbln.genexplus.report.config.Configuration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link EmailDeliveryService} using an in-process SMTP server.
 */
@Tag("integration")
class EmailDeliveryIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    private Configuration configuration;

    @BeforeEach
    void setUp() throws IOException {
        GREEN_MAIL.reset();
        configuration = new Configuration("");
        configuration.setProperty("mail.smtp.enabled", "true");
        configuration.setProperty("mail.smtp.host", "127.0.0.1");
        configuration.setProperty("mail.smtp.port", String.valueOf(GREEN_MAIL.getSmtp().getPort()));
        configuration.setProperty("mail.smtp.from", "sender@example.com");
        configuration.setProperty("mail.smtp.auth", "false");
        configuration.setProperty("mail.smtp.starttls.enable", "false");
    }

    @Test
    void sendsEmailWithAttachment() throws Exception {
        var emailService = new EmailDeliveryService(EmailConfig.from(configuration));
        var attachment = Files.createTempFile("report", ".pdf");
        Files.write(attachment, new byte[]{0x25, 0x50, 0x44, 0x46});

        try {
            var message = EmailMessage.builder()
                    .to("recipient@example.com")
                    .subject("Monthly Report")
                    .body("Please find the report attached.")
                    .attachment(attachment)
                    .build();

            assertDoesNotThrow(() -> emailService.send(message));
            assertEquals(1, GREEN_MAIL.getReceivedMessages().length);

            var received = GREEN_MAIL.getReceivedMessages()[0];
            assertEquals("Monthly Report", received.getSubject());
            assertTrue(GreenMailUtil.getBody(received).contains("Please find the report attached."));
        } finally {
            Files.deleteIfExists(attachment);
        }
    }

    @Test
    void sendsSeparatePlainAndHtmlBodies() throws Exception {
        var emailService = new EmailDeliveryService(EmailConfig.from(configuration));

        var message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("HTML Report")
                .body("<p>Hello <b>GenExPlus</b></p>")
                .bodyPlain("Hello GenExPlus")
                .build();

        emailService.send(message);

        var received = GREEN_MAIL.getReceivedMessages()[0];
        var content = received.getContent();
        assertInstanceOf(MimeMultipart.class, content);

        var mixed = (MimeMultipart) content;
        var alternative = (MimeMultipart) mixed.getBodyPart(0).getContent();
        var plainPart = alternative.getBodyPart(0).getContent().toString();
        var htmlWrapper = alternative.getBodyPart(1).getContent();
        var htmlPart = ((MimeMultipart) htmlWrapper).getBodyPart(0).getContent().toString();

        assertEquals("Hello GenExPlus", plainPart.trim());
        assertTrue(htmlPart.contains("<b>GenExPlus</b>"));
    }

    @Test
    void derivesPlainBodyFromHtmlWhenNotProvided() {
        var message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Derived plain text")
                .body("<p>Hello <b>World</b></p>")
                .build();

        assertEquals("Hello World", message.getBodyPlain());
    }

    @Test
    void rejectsMissingAttachmentBeforeSend() throws Exception {
        var emailService = new EmailDeliveryService(EmailConfig.from(configuration));

        var message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Broken attachment")
                .body("Body")
                .attachment(Path.of("/does/not/exist/report.pdf"))
                .build();

        assertThrows(IOException.class, () -> emailService.send(message));
        assertEquals(0, GREEN_MAIL.getReceivedMessages().length);
    }

    @Test
    void rejectsOversizedAttachmentBeforeSend() throws Exception {
        configuration.setProperty("mail.smtp.maxAttachmentBytes", "16");
        var emailService = new EmailDeliveryService(EmailConfig.from(configuration));

        var attachment = Files.createTempFile("large", ".pdf");
        Files.write(attachment, new byte[32]);

        try {
            var message = EmailMessage.builder()
                    .to("recipient@example.com")
                    .subject("Too large")
                    .body("Body")
                    .attachment(attachment)
                    .build();

            assertThrows(IllegalArgumentException.class, () -> emailService.send(message));
            assertEquals(0, GREEN_MAIL.getReceivedMessages().length);
        } finally {
            Files.deleteIfExists(attachment);
        }
    }

    @Test
    void sendsViaSmtpsWhenSslEnabled() throws Exception {
        var smtps = new GreenMailExtension(ServerSetupTest.SMTPS);
        smtps.start();

        try {
            configuration.setProperty("mail.smtp.port", String.valueOf(smtps.getSmtps().getPort()));
            configuration.setProperty("mail.smtp.ssl.enable", "true");
            configuration.setProperty("mail.smtp.starttls.enable", "false");

            var emailService = new EmailDeliveryService(EmailConfig.from(configuration));
            var message = EmailMessage.builder()
                    .to("recipient@example.com")
                    .subject("SMTPS Report")
                    .body("Sent over implicit SSL")
                    .build();

            emailService.send(message);
            assertEquals(1, smtps.getReceivedMessages().length);
            assertEquals("SMTPS Report", smtps.getReceivedMessages()[0].getSubject());
        } finally {
            smtps.stop();
        }
    }
}

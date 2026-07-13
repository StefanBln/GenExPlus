package io.github.stefanbln.genexplus.report.delivery;

import io.github.stefanbln.genexplus.report.config.Configuration;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EmailDeliveryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsOversizedAttachment() throws IOException {
        var config = emailConfig(builder -> builder.setProperty("mail.smtp.maxAttachmentBytes", "10"));
        var service = new EmailDeliveryService(EmailConfig.from(config));

        var attachment = tempDir.resolve("large.pdf");
        Files.write(attachment, new byte[20]);

        var message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Too large")
                .body("Body")
                .attachment(attachment)
                .build();

        var error = assertThrows(IllegalArgumentException.class, () -> service.send(message));
        assertTrue(error.getMessage().contains("exceeds maximum size"));
    }

    @Test
    void retriesTransientFailuresWithExponentialBackoff() throws Exception {
        var config = emailConfig(builder -> {
            builder.setProperty("mail.smtp.retry.count", "2");
            builder.setProperty("mail.smtp.retry.delayMs", "100");
        });
        var emailConfig = EmailConfig.from(config);
        var attempts = new AtomicInteger();
        var sleepCalls = new AtomicInteger();

        var service = new EmailDeliveryService(
                emailConfig,
                message -> {
                    if (attempts.incrementAndGet() < 3) {
                        throw new MessagingException("transient SMTP failure");
                    }
                },
                delayMs -> {
                    sleepCalls.incrementAndGet();
                    assertTrue(delayMs == 100 || delayMs == 200);
                }
        );

        var message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Retry test")
                .body("Body")
                .build();

        assertDoesNotThrow(() -> service.send(message));
        assertEquals(3, attempts.get());
        assertEquals(2, sleepCalls.get());
    }

    @Test
    void failsAfterExhaustingRetries() throws IOException {
        var config = emailConfig(builder -> builder.setProperty("mail.smtp.retry.count", "1"));
        var emailConfig = EmailConfig.from(config);
        var attempts = new AtomicInteger();

        var service = new EmailDeliveryService(
                emailConfig,
                message -> {
                    attempts.incrementAndGet();
                    throw new MessagingException("persistent failure");
                },
                delayMs -> { }
        );

        var message = EmailMessage.builder()
                .to("recipient@example.com")
                .subject("Fail test")
                .body("Body")
                .build();

        assertThrows(MessagingException.class, () -> service.send(message));
        assertEquals(2, attempts.get());
    }

    private static Configuration emailConfig(java.util.function.Consumer<Configuration> customizer) throws IOException {
        var configuration = new Configuration("");
        configuration.setProperty("mail.smtp.host", "smtp.example.com");
        configuration.setProperty("mail.smtp.from", "sender@example.com");
        configuration.setProperty("mail.smtp.auth", "false");
        customizer.accept(configuration);
        return configuration;
    }
}

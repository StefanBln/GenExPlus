package io.github.stefanbln.genexplus.report.delivery;

import io.github.stefanbln.genexplus.report.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für EmailConfig.
 *
 */
class EmailConfigTest {

    @AfterEach
    void cleanup() {
        // Entferne alle mail.* System-Properties
        System.getProperties().stringPropertyNames().stream()
            .filter(key -> key.startsWith("mail."))
            .forEach(System::clearProperty);
    }

    @Test
    void testFromConfigurationWithMinimalSettings() {
        var config = createTestConfiguration(props -> {
            props.setProperty("mail.smtp.host", "smtp.example.com");
            props.setProperty("mail.smtp.from", "noreply@example.com");
        });

        var emailConfig = EmailConfig.from(config);

        assertEquals("smtp.example.com", emailConfig.getSmtpHost());
        assertEquals(587, emailConfig.getSmtpPort()); // Default
        assertEquals("noreply@example.com", emailConfig.getFromAddress());  
    }

    @Test
    void testFromConfigurationWithFullSettings() {
        var config = createTestConfiguration(props -> {
            props.setProperty("mail.smtp.host", "smtp.example.com");
            props.setProperty("mail.smtp.port", "587");
            props.setProperty("mail.smtp.from", "noreply@example.com");
            props.setProperty("mail.smtp.username", "user@example.com");
            props.setProperty("mail.smtp.password", "secret");
            props.setProperty("mail.smtp.auth", "true");
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.starttls.required", "true");
            props.setProperty("mail.smtp.connectiontimeout", "5000");
            props.setProperty("mail.smtp.timeout", "5000");
            props.setProperty("mail.smtp.debug", "true");
        });

        var emailConfig = EmailConfig.from(config);

        assertEquals("smtp.example.com", emailConfig.getSmtpHost());
        assertEquals(587, emailConfig.getSmtpPort());
        assertEquals("noreply@example.com", emailConfig.getFromAddress());
        assertEquals("user@example.com", emailConfig.getUsername().orElse(null));
        assertEquals("secret", emailConfig.getPassword().orElse(null));
        assertTrue(emailConfig.isAuthEnabled());
        assertTrue(emailConfig.isStartTlsEnabled());
        assertTrue(emailConfig.isDebugEnabled());
    }

    @Test
    void testFromConfigurationMissingHost() {
        var config = createTestConfiguration(props -> {
            props.setProperty("mail.smtp.from", "noreply@example.com");
            // mail.smtp.host fehlt
        });

        assertThrows(IllegalStateException.class, () -> EmailConfig.from(config));
    }

    @Test
    void testFromConfigurationMissingFromAddress() {
        var config = createTestConfiguration(props -> {
            props.setProperty("mail.smtp.host", "smtp.example.com");
            // mail.smtp.from fehlt
        });

        assertThrows(IllegalStateException.class, () -> EmailConfig.from(config));
    }

    @Test
    void testIsEnabled() {
        var enabledConfig = createTestConfiguration(props ->
                props.setProperty("mail.smtp.enabled", "true")
        );

        var disabledConfig = createTestConfiguration(props ->
                props.setProperty("mail.smtp.enabled", "false")
        );

        var defaultConfig = createTestConfiguration(props -> {
            // Keine mail.smtp.enabled Einstellung
        });

        assertTrue(EmailConfig.isEnabled(enabledConfig));
        assertFalse(EmailConfig.isEnabled(disabledConfig));
        assertFalse(EmailConfig.isEnabled(defaultConfig)); // Default: false
    }

    @Test
    void testToPropertiesIncludesSslSettings() {
        var config = createTestConfiguration(props -> {
            props.setProperty("mail.smtp.host", "smtp.example.com");
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.from", "noreply@example.com");
            props.setProperty("mail.smtp.ssl.enable", "true");
        });

        var emailConfig = EmailConfig.from(config);
        var mailProps = emailConfig.toProperties();

        assertEquals("true", mailProps.getProperty("mail.smtp.ssl.enable"));
        assertEquals("false", mailProps.getProperty("mail.smtp.starttls.enable"));
        assertEquals("465", mailProps.getProperty("mail.smtp.socketFactory.port"));
        assertEquals("javax.net.ssl.SSLSocketFactory", mailProps.getProperty("mail.smtp.socketFactory.class"));
        assertTrue(emailConfig.isSslEnabled());
    }

    @Test
    void testRetryAndAttachmentDefaults() {
        var config = createTestConfiguration(props -> {
            props.setProperty("mail.smtp.host", "smtp.example.com");
            props.setProperty("mail.smtp.from", "noreply@example.com");
        });

        var emailConfig = EmailConfig.from(config);

        assertEquals(EmailConfig.DEFAULT_RETRY_COUNT, emailConfig.getRetryCount());
        assertEquals(EmailConfig.DEFAULT_RETRY_DELAY_MS, emailConfig.getRetryDelayMs());
        assertEquals(EmailConfig.DEFAULT_MAX_ATTACHMENT_BYTES, emailConfig.getMaxAttachmentBytes());
    }

    @Test
    void testToProperties() {
        var config = createTestConfiguration(props -> {
            props.setProperty("mail.smtp.host", "smtp.example.com");
            props.setProperty("mail.smtp.port", "587");
            props.setProperty("mail.smtp.from", "noreply@example.com");
            props.setProperty("mail.smtp.auth", "true");
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.debug", "true");
        });

        var emailConfig = EmailConfig.from(config);
        var mailProps = emailConfig.toProperties();

        assertEquals("smtp.example.com", mailProps.getProperty("mail.smtp.host"));
        assertEquals("587", mailProps.getProperty("mail.smtp.port"));
        assertEquals("true", mailProps.getProperty("mail.smtp.auth"));
        assertEquals("true", mailProps.getProperty("mail.smtp.starttls.enable"));
        assertEquals("true", mailProps.getProperty("mail.debug"));
    }


    @Test
    void testToStringDoesNotContainPassword() {
        var config = createTestConfiguration(props -> {
            props.setProperty("mail.smtp.host", "smtp.example.com");
            props.setProperty("mail.smtp.from", "noreply@example.com");
            props.setProperty("mail.smtp.username", "user@example.com");
            props.setProperty("mail.smtp.password", "secret");
        });

        var emailConfig = EmailConfig.from(config);
        var str = emailConfig.toString();

        assertTrue(str.contains("smtp.example.com"));
        assertFalse(str.contains("secret"));
    }

    /**
     * Hilfsmethode zum Erstellen einer Test-Konfiguration.
     */
    private Configuration createTestConfiguration(java.util.function.Consumer<Properties> propsConsumer) {
        // Erstelle temporäre Properties-Datei im Memory
        var props = new Properties();
        propsConsumer.accept(props);

        // Setze Properties als System-Properties (höchste Priorität)
        props.forEach((key, value) -> System.setProperty(key.toString(), value.toString()));

        // Skip file loading; rely on system properties set above.
        try {
            return new Configuration("");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

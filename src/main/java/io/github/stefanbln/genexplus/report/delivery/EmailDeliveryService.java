/*
 * Copyright 2026 Stefan Schuetz - Locivera - Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.stefanbln.genexplus.report.delivery;

import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends report emails over SMTP using Jakarta Mail.
 *
 * <p>Messages are sent as multipart/alternative content with plain text and HTML bodies. When
 * {@code mail.smtp.logo.resource} is set in application properties, that classpath resource is
 * embedded inline in the HTML part.
 *
 * <p>Transient {@link MessagingException}s are retried with exponential backoff according to
 * {@link EmailConfig#getRetryCount()} and {@link EmailConfig#getRetryDelayMs()}.
 */
public final class EmailDeliveryService {

    private static final Logger LOGGER = Logger.getLogger(EmailDeliveryService.class.getName());

    /**
     * Sends a prepared MIME message. Used for testing retry behaviour.
     */
    @FunctionalInterface
    interface MailSender {
        void send(MimeMessage message) throws MessagingException;
    }

    private final EmailConfig config;
    private final MailSender mailSender;
    private final Sleeper sleeper;

    /**
     * Creates a delivery service for the given SMTP configuration.
     *
     * @param config validated SMTP settings
     */
    public EmailDeliveryService(EmailConfig config) {
        this(config, Transport::send, Thread::sleep);
    }

    EmailDeliveryService(EmailConfig config, MailSender mailSender, Sleeper sleeper) {
        this.config = config;
        this.mailSender = mailSender;
        this.sleeper = sleeper;
    }

    /**
     * Sends the given message through the configured SMTP server.
     *
     * @param message email content and optional attachment
     * @throws MessagingException when Jakarta Mail cannot send the message after all retries
     * @throws IOException when an attachment file cannot be read
     * @throws IllegalArgumentException when the attachment exceeds the configured size limit
     */
    public void send(EmailMessage message) throws MessagingException, IOException {
        LOGGER.log(Level.INFO, "Sending email to: {0}", message.getTo());
        LOGGER.log(Level.FINE, "Email details: {0}", message);

        validateAttachmentSize(message);

        var session = createSession();
        var mimeMessage = buildMimeMessage(session, message);
        sendWithRetry(mimeMessage);

        LOGGER.log(Level.INFO, "Email sent successfully to: {0}", message.getTo());
    }

    /**
     * Verifies that the configured SMTP server is reachable and accepts authentication.
     *
     * @throws MessagingException when the connection or authentication fails
     */
    public void testConnection() throws MessagingException {
        LOGGER.log(Level.INFO, "Testing SMTP connection to: {0}:{1}",
                new Object[]{config.getSmtpHost(), config.getSmtpPort()});

        var session = createSession();
        var protocol = config.isSslEnabled() ? "smtps" : "smtp";
        try (var transport = session.getTransport(protocol)) {
            if (config.isAuthEnabled() && config.getUsername().isPresent() && config.getPassword().isPresent()) {
                transport.connect(
                        config.getSmtpHost(),
                        config.getSmtpPort(),
                        config.getUsername().get(),
                        config.getPassword().get()
                );
            } else {
                transport.connect(config.getSmtpHost(), config.getSmtpPort(), null, null);
            }

            LOGGER.info("SMTP connection successful");
        }
    }

    private void validateAttachmentSize(EmailMessage message) throws IOException {
        if (!message.hasAttachment()) {
            return;
        }

        var attachment = message.getAttachment();
        if (!Files.exists(attachment)) {
            throw new IOException("Attachment not found: " + attachment);
        }

        var size = Files.size(attachment);
        var maxBytes = config.getMaxAttachmentBytes();
        if (size > maxBytes) {
            throw new IllegalArgumentException(
                    "Attachment '" + message.getAttachmentName() + "' exceeds maximum size of "
                            + maxBytes + " bytes (actual: " + size + " bytes)");
        }
    }

    private void sendWithRetry(MimeMessage mimeMessage) throws MessagingException {
        var maxRetries = config.getRetryCount();
        var baseDelayMs = config.getRetryDelayMs();
        MessagingException lastFailure = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                mailSender.send(mimeMessage);
                if (attempt > 0) {
                    LOGGER.log(Level.INFO, "Email sent successfully after {0} retries", attempt);
                }
                return;
            } catch (MessagingException e) {
                lastFailure = e;
                if (attempt >= maxRetries) {
                    break;
                }

                var delayMs = baseDelayMs > 0 ? baseDelayMs * (1L << attempt) : 0L;
                LOGGER.log(Level.WARNING,
                        "Email send failed (attempt {0}/{1}), retrying in {2} ms: {3}",
                        new Object[]{attempt + 1, maxRetries + 1, delayMs, e.getMessage()});
                sleep(delayMs);
            }
        }

        throw lastFailure;
    }

    private void sleep(long delayMs) throws MessagingException {
        if (delayMs <= 0) {
            return;
        }
        try {
            sleeper.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("Email retry interrupted", e);
        }
    }

    private Session createSession() {
        var props = config.toProperties();

        Authenticator authenticator = null;
        if (config.isAuthEnabled() && config.getUsername().isPresent() && config.getPassword().isPresent()) {
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            config.getUsername().get(),
                            config.getPassword().get()
                    );
                }
            };
        }

        return Session.getInstance(props, authenticator);
    }

    private MimeMessage buildMimeMessage(Session session, EmailMessage message)
            throws MessagingException, IOException {
        var mimeMessage = new MimeMessage(session);

        mimeMessage.setFrom(new InternetAddress(config.getFromAddress()));

        for (var to : message.getTo()) {
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        }

        for (var cc : message.getCc()) {
            mimeMessage.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
        }

        for (var bcc : message.getBcc()) {
            mimeMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
        }

        mimeMessage.setSubject(message.getSubject(), "UTF-8");
        mimeMessage.setContent(buildContent(message));

        return mimeMessage;
    }

    private Multipart buildContent(EmailMessage message) throws MessagingException, IOException {
        var mixed = new MimeMultipart("mixed");

        var alternative = new MimeMultipart("alternative");

        var textPart = new MimeBodyPart();
        textPart.setText(message.getBodyPlain(), "UTF-8");
        alternative.addBodyPart(textPart);

        var htmlWrapper = new MimeBodyPart();
        htmlWrapper.setContent(buildHtmlContent(message));
        alternative.addBodyPart(htmlWrapper);

        var alternativeWrapper = new MimeBodyPart();
        alternativeWrapper.setContent(alternative);
        mixed.addBodyPart(alternativeWrapper);

        if (message.hasAttachment()) {
            var attachmentPart = new MimeBodyPart();
            var attachment = message.getAttachment();

            attachmentPart.attachFile(attachment.toFile());
            attachmentPart.setFileName(message.getAttachmentName());
            mixed.addBodyPart(attachmentPart);

            LOGGER.log(Level.FINE, "Email attachment: {0} ({1} bytes)",
                    new Object[]{message.getAttachmentName(), Files.size(attachment)});
        }

        return mixed;
    }

    /**
     * Builds the HTML body and optionally embeds a logo from the classpath.
     */
    private Multipart buildHtmlContent(EmailMessage message) throws MessagingException, IOException {
        var htmlMultipart = new MimeMultipart("related");

        var htmlPart = new MimeBodyPart();
        htmlPart.setContent(message.getBody(), "text/html; charset=UTF-8");
        htmlMultipart.addBodyPart(htmlPart);

        var logoResource = config.getLogoResource();
        if (logoResource.isPresent()) {
            String resourceName = logoResource.get();
            try (InputStream logoStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                if (logoStream != null) {
                    var logoBytes = logoStream.readAllBytes();
                    var logoPart = new MimeBodyPart();
                    logoPart.setDataHandler(new DataHandler(new ByteArrayDataSource(logoBytes, "image/png")));
                    logoPart.setHeader("Content-Type", "image/png");
                    logoPart.setContentID("<logoGenExPlusReport>");
                    logoPart.setDisposition(MimeBodyPart.INLINE);
                    htmlMultipart.addBodyPart(logoPart);
                } else {
                    LOGGER.log(Level.WARNING, "Configured logo resource not found on classpath: {0}", resourceName);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load logo resource: {0}", resourceName);
            }
        }

        return htmlMultipart;
    }

    /**
     * Abstraction for retry backoff delays (test seam).
     */
    @FunctionalInterface
    interface Sleeper {
        void sleep(long delayMs) throws InterruptedException;
    }

    @Override
    public String toString() {
        return "EmailDeliveryService{config=" + config + '}';
    }
}

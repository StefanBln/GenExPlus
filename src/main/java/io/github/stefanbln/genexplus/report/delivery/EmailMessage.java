package io.github.stefanbln.genexplus.report.delivery;

import io.github.stefanbln.genexplus.report.PlaceholderResolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable email message with optional report attachment.
 *
 * <p>Instances are created through {@link Builder}. Subject and body strings may contain
 * timestamp placeholders in the form {@code {[pattern]}}, which are resolved at build time
 * using {@link #resolvePlaceholders(String)}.
 */
public final class EmailMessage {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private final List<String> to;
    private final List<String> cc;
    private final List<String> bcc;
    private final String subject;
    private final String body;
    private final String bodyPlain;
    private final Path attachment;
    private final String attachmentName;

    private EmailMessage(List<String> to, List<String> cc, List<String> bcc,
                        String subject, String body, String bodyPlain,
                        Path attachment, String attachmentName) {
        if (to == null || to.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient required");
        }
        to.forEach(EmailMessage::validateEmail);
        cc.forEach(EmailMessage::validateEmail);
        bcc.forEach(EmailMessage::validateEmail);

        this.to = Collections.unmodifiableList(new ArrayList<>(to));
        this.cc = Collections.unmodifiableList(new ArrayList<>(cc));
        this.bcc = Collections.unmodifiableList(new ArrayList<>(bcc));
        this.subject = Objects.requireNonNull(subject, "Subject cannot be null");
        this.body = Objects.requireNonNull(body, "Body cannot be null");
        this.bodyPlain = Objects.requireNonNull(bodyPlain, "Plain body cannot be null");
        this.attachment = attachment;
        this.attachmentName = attachmentName;
    }

    /**
     * Creates a new message builder.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Replaces {@code {[datePattern]}} placeholders in the given text with the current time.
     *
     * <p>Invalid patterns are left unchanged.
     *
     * @param text input text, may be {@code null}
     * @return text with placeholders resolved, or {@code null} when input is {@code null}
     */
    public static String resolvePlaceholders(String text) {
        return PlaceholderResolver.resolveNow(text);
    }

    /**
     * Escapes HTML special characters for safe inclusion in email bodies.
     *
     * @param text raw text, may be {@code null}
     * @return escaped text, or {@code null} when input is {@code null}
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        var escaped = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    /**
     * Returns whether the given string is a valid email address for report delivery.
     *
     * @param email address to check
     * @return {@code true} when the address matches the supported format
     */
    public static boolean isValidAddress(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private static void validateEmail(String email) {
        if (!isValidAddress(email)) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
    }

    /** Primary recipients. */
    public List<String> getTo() {
        return to;
    }

    /** CC recipients. */
    public List<String> getCc() {
        return cc;
    }

    /** BCC recipients. */
    public List<String> getBcc() {
        return bcc;
    }

    /** Message subject after placeholder resolution. */
    public String getSubject() {
        return subject;
    }

    /** HTML message body after placeholder resolution. */
    public String getBody() {
        return body;
    }

    /** Plain-text message body after placeholder resolution. */
    public String getBodyPlain() {
        return bodyPlain;
    }

    /** Attached file path, or {@code null} when no attachment is present. */
    public Path getAttachment() {
        return attachment;
    }

    /** Filename used for the attachment in the email. */
    public String getAttachmentName() {
        return attachmentName;
    }

    /** Returns whether this message includes a file attachment. */
    public boolean hasAttachment() {
        return attachment != null;
    }

    @Override
    public String toString() {
        return "EmailMessage{" +
                "to=" + to +
                ", cc=" + cc +
                ", bcc=" + bcc +
                ", subject='" + subject + '\'' +
                ", hasAttachment=" + hasAttachment() +
                '}';
    }

    private static String derivePlainText(String htmlOrText) {
        if (htmlOrText == null || htmlOrText.isBlank()) {
            return "";
        }
        if (!htmlOrText.contains("<")) {
            return htmlOrText;
        }
        return htmlOrText.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Fluent builder for {@link EmailMessage}.
     */
    public static final class Builder {
        private final List<String> to = new ArrayList<>();
        private final List<String> cc = new ArrayList<>();
        private final List<String> bcc = new ArrayList<>();
        private String subject;
        private String body;
        private String bodyPlain;
        private Path attachment;
        private String attachmentName;

        /** Adds TO recipients. */
        public Builder to(String... addresses) {
            Collections.addAll(to, addresses);
            return this;
        }

        /** Adds TO recipients from a list. */
        public Builder to(List<String> addresses) {
            to.addAll(addresses);
            return this;
        }

        /** Adds CC recipients. */
        public Builder cc(String... addresses) {
            Collections.addAll(cc, addresses);
            return this;
        }

        /** Adds CC recipients from a list. */
        public Builder cc(List<String> addresses) {
            cc.addAll(addresses);
            return this;
        }

        /** Adds BCC recipients. */
        public Builder bcc(String... addresses) {
            Collections.addAll(bcc, addresses);
            return this;
        }

        /** Adds BCC recipients from a list. */
        public Builder bcc(List<String> addresses) {
            bcc.addAll(addresses);
            return this;
        }

        /**
         * Sets the subject line. Placeholders are resolved at build time.
         */
        public Builder subject(String subject) {
            this.subject = resolvePlaceholders(subject);
            return this;
        }

        /**
         * Sets the HTML message body. Placeholders are resolved at build time.
         */
        public Builder body(String body) {
            this.body = resolvePlaceholders(body);
            return this;
        }

        /**
         * Sets the message body after escaping HTML special characters.
         */
        public Builder bodyEscaped(String body) {
            this.body = resolvePlaceholders(escapeHtml(body));
            return this;
        }

        /**
         * Sets the plain-text message body. When omitted, plain text is derived from the HTML body.
         */
        public Builder bodyPlain(String bodyPlain) {
            this.bodyPlain = resolvePlaceholders(bodyPlain);
            return this;
        }

        /**
         * Attaches a file using its filename as the attachment name.
         */
        public Builder attachment(Path attachment) {
            this.attachment = attachment;
            if (attachment != null) {
                this.attachmentName = attachment.getFileName().toString();
            }
            return this;
        }

        /**
         * Attaches a file with a custom attachment filename.
         */
        public Builder attachment(Path attachment, String customName) {
            this.attachment = attachment;
            this.attachmentName = customName;
            return this;
        }

        /** Builds the immutable message. */
        public EmailMessage build() {
            var resolvedBody = body != null ? body : "";
            var resolvedPlain = bodyPlain != null && !bodyPlain.isBlank()
                    ? bodyPlain
                    : derivePlainText(resolvedBody);
            return new EmailMessage(to, cc, bcc, subject, resolvedBody, resolvedPlain,
                    attachment, attachmentName);
        }
    }
}

package io.github.stefanbln.genexplus.report.delivery;

import io.github.stefanbln.genexplus.report.config.Configuration;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Immutable SMTP settings for report email delivery.
 *
 * <p>Values are read from {@code mail.smtp.*} properties in {@link Configuration}. Passwords
 * can be supplied through the {@code REPORT_MAIL_PASSWORD} environment variable or the
 * {@code mail.smtp.password} property.
 *
 * <h2>Required properties</h2>
 * <ul>
 *   <li>{@code mail.smtp.host}</li>
 *   <li>{@code mail.smtp.from}</li>
 * </ul>
 *
 * <h2>Common optional properties</h2>
 * <ul>
 *   <li>{@code mail.smtp.port} (default: {@value #DEFAULT_SMTP_PORT})</li>
 *   <li>{@code mail.smtp.auth} (default: {@code true})</li>
 *   <li>{@code mail.smtp.starttls.enable} (default: {@code true})</li>
 *   <li>{@code mail.smtp.ssl.enable} — SMTPS / port 465 (default: {@code false})</li>
 *   <li>{@code mail.smtp.retry.count} — retries after first failure (default: {@value #DEFAULT_RETRY_COUNT})</li>
 *   <li>{@code mail.smtp.retry.delayMs} — base backoff delay (default: {@value #DEFAULT_RETRY_DELAY_MS} ms)</li>
 *   <li>{@code mail.smtp.maxAttachmentBytes} — attachment limit (default: 25 MiB)</li>
 *   <li>{@code mail.smtp.connectiontimeout}, {@code mail.smtp.timeout}
 *       (default: {@value #DEFAULT_SMTP_TIMEOUT_MS} ms)</li>
 *   <li>{@code mail.smtp.logo.resource} — optional classpath image for HTML emails</li>
 *   <li>{@code mail.smtp.ssl.trust} — trust host(s) for implicit SSL / STARTTLS (e.g. internal CA)</li>
 *   <li>{@code mail.smtp.ssl.checkserveridentity} — hostname verification (default: {@code true})</li>
 * </ul>
 */
public final class EmailConfig {

    /** Default SMTP submission port. */
    public static final int DEFAULT_SMTP_PORT = 587;

    /** Default socket timeout in milliseconds. */
    public static final int DEFAULT_SMTP_TIMEOUT_MS = 10_000;

    /** Default maximum attachment size: 25 MiB. */
    public static final long DEFAULT_MAX_ATTACHMENT_BYTES = 26_214_400L;

    /** Default number of retries after the first failed send attempt. */
    public static final int DEFAULT_RETRY_COUNT = 2;

    /** Default delay before the first retry. */
    public static final int DEFAULT_RETRY_DELAY_MS = 2_000;

    private static final String ENV_MAIL_PASSWORD = "REPORT_MAIL_PASSWORD";

    private final String smtpHost;
    private final int smtpPort;
    private final String fromAddress;
    private final String username;
    private final String password;
    private final boolean authEnabled;
    private final boolean startTlsEnabled;
    private final boolean startTlsRequired;
    private final boolean sslEnabled;
    private final int retryCount;
    private final int retryDelayMs;
    private final long maxAttachmentBytes;
    private final int connectionTimeout;
    private final int timeout;
    private final boolean debugEnabled;
    private final String logoResource;
    private final String sslTrust;
    private final boolean sslCheckServerIdentity;

    private EmailConfig(String smtpHost, int smtpPort, String fromAddress,
                       String username, String password, boolean authEnabled,
                       boolean startTlsEnabled, boolean startTlsRequired, boolean sslEnabled,
                       int retryCount, int retryDelayMs, long maxAttachmentBytes,
                       int connectionTimeout, int timeout, boolean debugEnabled,
                       String logoResource, String sslTrust, boolean sslCheckServerIdentity) {
        this.smtpHost = Objects.requireNonNull(smtpHost, "SMTP host cannot be null");
        this.smtpPort = smtpPort;
        this.fromAddress = Objects.requireNonNull(fromAddress, "From address cannot be null");
        this.username = username;
        this.password = password;
        this.authEnabled = authEnabled;
        this.startTlsEnabled = startTlsEnabled;
        this.startTlsRequired = startTlsRequired;
        this.sslEnabled = sslEnabled;
        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
        this.maxAttachmentBytes = maxAttachmentBytes;
        this.connectionTimeout = connectionTimeout;
        this.timeout = timeout;
        this.debugEnabled = debugEnabled;
        this.logoResource = logoResource;
        this.sslTrust = sslTrust;
        this.sslCheckServerIdentity = sslCheckServerIdentity;
    }

    /**
     * Builds an {@link EmailConfig} from application configuration.
     *
     * @param configuration shared application settings
     * @return immutable SMTP configuration
     * @throws IllegalStateException if required SMTP properties are missing
     */
    public static EmailConfig from(Configuration configuration) {
        var host = configuration.getString("mail.smtp.host");
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("SMTP host not configured: mail.smtp.host");
        }

        var fromAddress = configuration.getString("mail.smtp.from");
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("SMTP from address not configured: mail.smtp.from");
        }

        var port = configuration.getInt("mail.smtp.port", DEFAULT_SMTP_PORT);
        var username = configuration.getString("mail.smtp.username");
        var password = getPasswordFromEnvOrConfig(configuration);
        var authEnabled = configuration.getBoolean("mail.smtp.auth", true);
        var sslEnabled = configuration.getBoolean("mail.smtp.ssl.enable", false);
        var startTlsEnabled = configuration.getBoolean("mail.smtp.starttls.enable", true);
        if (sslEnabled) {
            startTlsEnabled = false;
        }
        var startTlsRequired = configuration.getBoolean("mail.smtp.starttls.required", false);
        var retryCount = Math.max(0, configuration.getInt("mail.smtp.retry.count", DEFAULT_RETRY_COUNT));
        var retryDelayMs = Math.max(0, configuration.getInt("mail.smtp.retry.delayMs", DEFAULT_RETRY_DELAY_MS));
        var maxAttachmentBytes = configuration.getLong("mail.smtp.maxAttachmentBytes", DEFAULT_MAX_ATTACHMENT_BYTES);
        if (maxAttachmentBytes <= 0) {
            maxAttachmentBytes = Long.MAX_VALUE;
        }
        var connectionTimeout = configuration.getInt("mail.smtp.connectiontimeout", DEFAULT_SMTP_TIMEOUT_MS);
        var timeout = configuration.getInt("mail.smtp.timeout", DEFAULT_SMTP_TIMEOUT_MS);
        var debugEnabled = configuration.getBoolean("mail.smtp.debug", false);
        var logoResource = configuration.getString("mail.smtp.logo.resource");
        var sslTrust = configuration.getString("mail.smtp.ssl.trust");
        var sslCheckServerIdentity = configuration.getBoolean("mail.smtp.ssl.checkserveridentity", true);

        return new EmailConfig(host, port, fromAddress, username, password,
                authEnabled, startTlsEnabled, startTlsRequired, sslEnabled,
                retryCount, retryDelayMs, maxAttachmentBytes,
                connectionTimeout, timeout, debugEnabled, logoResource, sslTrust, sslCheckServerIdentity);
    }

    /**
     * Returns whether SMTP delivery is enabled globally.
     *
     * @param configuration shared application settings
     * @return {@code true} when {@code mail.smtp.enabled=true}
     */
    public static boolean isEnabled(Configuration configuration) {
        return configuration.getBoolean("mail.smtp.enabled", false);
    }

    /**
     * Validates SMTP settings required when report email delivery is enabled.
     *
     * @param configuration shared application settings
     * @return error message when validation fails, otherwise {@code null}
     */
    public static String validateForReporting(Configuration configuration) {
        if (!isEnabled(configuration)) {
            return "Email delivery is enabled but SMTP is not configured (mail.smtp.enabled=true required)";
        }

        try {
            var emailConfig = from(configuration);
            if (!EmailMessage.isValidAddress(emailConfig.getFromAddress())) {
                return "Invalid SMTP from address: " + emailConfig.getFromAddress();
            }
            if (emailConfig.isAuthEnabled()) {
                if (emailConfig.getUsername().isEmpty()) {
                    return "mail.smtp.auth=true but mail.smtp.username is not configured";
                }
                if (emailConfig.getPassword().isEmpty()) {
                    return "mail.smtp.auth=true but no password configured "
                            + "(set mail.smtp.password or REPORT_MAIL_PASSWORD)";
                }
            }
            return null;
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    /**
     * Converts these settings to Jakarta Mail {@link Properties}.
     *
     * @return mail session properties ready for {@link jakarta.mail.Session#getInstance}
     */
    public Properties toProperties() {
        var props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", String.valueOf(authEnabled));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));
        props.put("mail.smtp.starttls.required", String.valueOf(startTlsRequired));
        props.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(timeout));

        if (sslEnabled) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            if (sslTrust != null && !sslTrust.isBlank()) {
                props.put("mail.smtp.ssl.trust", sslTrust);
            }
            props.put("mail.smtp.ssl.checkserveridentity", String.valueOf(sslCheckServerIdentity));
        }

        if (debugEnabled) {
            props.put("mail.debug", "true");
        }

        return props;
    }

    /** SMTP server hostname. */
    public String getSmtpHost() {
        return smtpHost;
    }

    /** SMTP server port. */
    public int getSmtpPort() {
        return smtpPort;
    }

    /** Sender address used in the {@code From} header. */
    public String getFromAddress() {
        return fromAddress;
    }

    /** SMTP username when authentication is enabled. */
    public Optional<String> getUsername() {
        return Optional.ofNullable(username).filter(s -> !s.isBlank());
    }

    /** SMTP password when authentication is enabled. */
    public Optional<String> getPassword() {
        return Optional.ofNullable(password).filter(s -> !s.isBlank());
    }

    /** Whether SMTP AUTH is requested. */
    public boolean isAuthEnabled() {
        return authEnabled;
    }

    /** Whether STARTTLS is enabled. */
    public boolean isStartTlsEnabled() {
        return startTlsEnabled;
    }

    /** Whether implicit SSL (SMTPS) is enabled. */
    public boolean isSslEnabled() {
        return sslEnabled;
    }

    /** Number of retries after the first failed send attempt. */
    public int getRetryCount() {
        return retryCount;
    }

    /** Base delay in milliseconds before the first retry (doubled for each subsequent retry). */
    public int getRetryDelayMs() {
        return retryDelayMs;
    }

    /** Maximum attachment size in bytes. */
    public long getMaxAttachmentBytes() {
        return maxAttachmentBytes;
    }

    /** Whether Jakarta Mail protocol debugging is enabled. */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /** Optional classpath resource for an inline HTML email logo. */
    public Optional<String> getLogoResource() {
        return Optional.ofNullable(logoResource).filter(s -> !s.isBlank());
    }

    private static String getPasswordFromEnvOrConfig(Configuration configuration) {
        return Optional.ofNullable(System.getenv(ENV_MAIL_PASSWORD))
                .filter(s -> !s.isEmpty())
                .orElseGet(() -> configuration.getString("mail.smtp.password"));
    }

    /**
     * Returns a debug representation without credentials.
     */
    @Override
    public String toString() {
        return "EmailConfig{" +
                "smtpHost='" + smtpHost + '\'' +
                ", smtpPort=" + smtpPort +
                ", fromAddress='" + fromAddress + '\'' +
                ", authEnabled=" + authEnabled +
                ", startTlsEnabled=" + startTlsEnabled +
                ", sslEnabled=" + sslEnabled +
                ", retryCount=" + retryCount +
                ", maxAttachmentBytes=" + maxAttachmentBytes +
                ", debugEnabled=" + debugEnabled +
                '}';
    }
}

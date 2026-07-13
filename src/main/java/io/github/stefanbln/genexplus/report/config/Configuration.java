package io.github.stefanbln.genexplus.report.config;

import io.github.stefanbln.genexplus.report.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads shared application configuration for GenExPlus.
 *
 * <p>Configuration values are merged from multiple sources. Later sources override earlier ones:
 * <ol>
 *   <li>Properties file ({@code application.properties} on the classpath or filesystem)</li>
 *   <li>Matching JVM system properties ({@code report.*}, {@code db*}, {@code mail.*},
 *       {@code signing.*})</li>
 *   <li>Environment variables prefixed with {@code REPORT_} (for example
 *       {@code REPORT_DB1_PASSWORD} maps to {@code db1.password})</li>
 * </ol>
 *
 * <h2>Common property keys</h2>
 * <ul>
 *   <li>{@code db1.url}, {@code db1.username}, {@code db1.password}, {@code db1.driver}</li>
 *   <li>{@code mail.smtp.host}, {@code mail.smtp.port}, {@code mail.smtp.from}, …</li>
 *   <li>{@code signing.enabled}, {@code signing.keystore.path}, {@code signing.keystore.alias}</li>
 *   <li>{@code report.governor.maxPages}, {@code report.governor.timeoutSeconds}</li>
 *   <li>{@code report.default.locale}, {@code report.default.timezone}</li>
 * </ul>
 *
 * <p>Sensitive values are redacted in {@link #toString()}.
 */
public final class Configuration {

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    private static final String DEFAULT_CONFIG_FILE = "application.properties";
    private static final String ENV_PREFIX = "REPORT_";
    private static final String KEY_SIGNING_KEYSTORE_PATH = "signing.keystore.path";
    private static final String KEY_SIGNING_KEYSTORE_ALIAS = "signing.keystore.alias";
    private static final String KEY_SIGNING_KEYSTORE_TYPE = "signing.keystore.type";
    private static final String KEY_SIGNING_KEYSTORE_PASSWORD = "signing.keystore.password";
    private static final String ENV_KEYSTORE_PASSWORD = "REPORT_KEYSTORE_PASSWORD";

    private final Properties properties;
    private final Map<String, DatabaseConfig> databaseConfigs;

    /**
     * Creates a configuration that loads {@code application.properties} from the classpath
     * when available.
     */
    public Configuration() {
        this.properties = new Properties();
        this.databaseConfigs = new HashMap<>();
        try {
            loadConfiguration(null);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load default configuration", e);
        }
        parseDatabaseConfigs();
    }

    /**
     * Creates a configuration backed by the given properties file.
     *
     * @param configFile path to a properties file, {@code null} for the default classpath resource,
     *                   or {@code ""} to skip file loading (used in tests)
     * @throws IOException when an explicit {@code configFile} path cannot be read
     */
    public Configuration(String configFile) throws IOException {
        this.properties = new Properties();
        this.databaseConfigs = new HashMap<>();
        loadConfiguration(configFile);
        parseDatabaseConfigs();
    }

    private void loadConfiguration(String configFile) throws IOException {
        if (configFile == null) {
            loadFromFile(DEFAULT_CONFIG_FILE, false);
        } else if (!configFile.isEmpty()) {
            loadFromFile(configFile, true);
        }

        applyPropertyOverrides();
    }

    private void applyPropertyOverrides() {
        System.getProperties().forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.startsWith("report.") || keyStr.startsWith("mail.")
                    || keyStr.startsWith("signing.")
                    || keyStr.matches("db\\d+\\..*")) {
                properties.setProperty(keyStr, value.toString());
            }
        });

        System.getenv().forEach((key, value) -> {
            if (key.startsWith(ENV_PREFIX)) {
                String propKey = key.substring(ENV_PREFIX.length())
                        .toLowerCase()
                        .replace('_', '.');
                properties.setProperty(propKey, value);
            }
        });
    }

    private void loadFromFile(String path, boolean required) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                properties.load(is);
                LOGGER.log(Level.INFO, "Loaded configuration from classpath: {0}", path);
                return;
            }
        } catch (IOException e) {
            if (required) {
                throw new IOException("Failed to load configuration from classpath: " + path, e);
            }
            LOGGER.log(Level.FINE, "Classpath configuration not found: {0}", path);
        }

        try {
            var real = PathUtils.resolveExistingSecureFile(path);
            try (InputStream is = java.nio.file.Files.newInputStream(real)) {
                properties.load(is);
                LOGGER.log(Level.INFO, "Loaded configuration from filesystem: {0}", real);
            }
        } catch (IOException | java.nio.file.InvalidPathException e) {
            if (required) {
                throw new IOException("Configuration file not found or not readable: " + path, e);
            }
            LOGGER.log(Level.FINE, "No configuration file found: {0}", path);
        }
    }

    /**
     * Groups {@code dbN.*} properties into {@link DatabaseConfig} records.
     */
    private void parseDatabaseConfigs() {
        Map<String, DatabaseConfig> configs = new HashMap<>();

        properties.forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.matches("^db\\d+\\..*")) {
                String[] parts = keyStr.split("\\.", 2);
                String dbId = parts[0];
                String property = parts[1];

                var currentConfig = configs.getOrDefault(dbId, new DatabaseConfig(dbId));

                var updatedConfig = switch (property) {
                    case "url" -> new DatabaseConfig(dbId, value.toString(),
                            currentConfig.username(), currentConfig.password(), currentConfig.driver());
                    case "username", "user" -> {
                        if ("user".equals(property)) {
                            LOGGER.log(Level.FINE,
                                    "Property {0} is accepted as an alias for {1}; prefer {1} in application.properties",
                                    new Object[]{dbId + ".user", dbId + ".username"});
                        }
                        yield new DatabaseConfig(dbId, currentConfig.url(),
                                value.toString(), currentConfig.password(), currentConfig.driver());
                    }
                    case "password" -> new DatabaseConfig(dbId, currentConfig.url(),
                            currentConfig.username(), value.toString(), currentConfig.driver());
                    case "driver" -> new DatabaseConfig(dbId, currentConfig.url(),
                            currentConfig.username(), currentConfig.password(), value.toString());
                    default -> currentConfig;
                };
                configs.put(dbId, updatedConfig);
            }
        });

        this.databaseConfigs.putAll(configs);
        LOGGER.log(Level.FINE, "Loaded database configurations: {0}", configs.keySet());
    }

    /**
     * Returns the raw string value for a property key.
     *
     * @param key property name
     * @return value, or {@code null} if not set
     */
    public String getString(String key) {
        return properties.getProperty(key);
    }

    /**
     * Returns the string value for a property key, or a default when unset.
     *
     * @param key property name
     * @param defaultValue fallback value
     * @return configured value or {@code defaultValue}
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Returns a boolean property. Recognizes {@code true}, {@code yes}, and {@code 1} as true.
     *
     * @param key property name
     * @return {@code false} when the property is unset
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Returns a boolean property with a default for unset values.
     *
     * @param key property name
     * @param defaultValue fallback value
     * @return parsed boolean value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * Returns an integer property with a default for unset or invalid values.
     *
     * @param key property name
     * @param defaultValue fallback value
     * @return parsed integer value
     */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid integer value for {0}: {1}", new Object[]{key, value});
            return defaultValue;
        }
    }

    /**
     * Returns a long property with a default for unset or invalid values.
     *
     * @param key property name
     * @param defaultValue fallback value
     * @return parsed long value
     */
    public long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid long value for {0}: {1}", new Object[]{key, value});
            return defaultValue;
        }
    }

    /**
     * Returns the JDBC configuration for a database ID such as {@code db1}.
     *
     * @param dbId database identifier from the report configuration
     * @return database configuration, or {@code null} if no matching properties exist
     */
    public DatabaseConfig getDatabaseConfig(String dbId) {
        return databaseConfigs.get(dbId);
    }

    /**
     * Returns database profile IDs that have a non-blank JDBC URL configured.
     */
    public java.util.Set<String> getConfiguredDatabaseIds() {
        return databaseConfigs.entrySet().stream()
                .filter(entry -> entry.getValue().url() != null && !entry.getValue().url().isBlank())
                .map(java.util.Map.Entry::getKey)
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /**
     * Returns whether PDF signing should be applied after PDF export.
     */
    public boolean isSigningEnabled() {
        return getBoolean("signing.enabled", false);
    }

    /**
     * Returns the filesystem path to the signing keystore.
     */
    public Optional<String> getKeystorePath() {
        return Optional.ofNullable(getString(KEY_SIGNING_KEYSTORE_PATH));
    }

    /**
     * Returns the keystore type, defaulting to {@code PKCS12}.
     */
    public String getKeystoreType() {
        return getString(KEY_SIGNING_KEYSTORE_TYPE, "PKCS12");
    }

    /**
     * Returns the keystore password.
     *
     * <p>Priority: {@code REPORT_KEYSTORE_PASSWORD} environment variable, then
     * {@code signing.keystore.password} property.
     */
    public Optional<String> getKeystorePassword() {
        return Optional.ofNullable(System.getenv(ENV_KEYSTORE_PASSWORD))
                .filter(s -> !s.isEmpty())
                .or(() -> Optional.ofNullable(getString(KEY_SIGNING_KEYSTORE_PASSWORD)));
    }

    /**
     * Returns the certificate alias inside the keystore.
     */
    public Optional<String> getKeystoreAlias() {
        return Optional.ofNullable(getString(KEY_SIGNING_KEYSTORE_ALIAS));
    }

    /**
     * When {@code true} (default), the PDF is certified with DocMDP so viewers restrict modifications.
     */
    public boolean isSigningCertify() {
        return getBoolean("signing.certify", true);
    }

    /**
     * DocMDP permission level when {@link #isSigningCertify()} is true.
     *
     * <ul>
     *   <li>{@code 1} — no changes allowed (default, strongest)</li>
     *   <li>{@code 2} — form filling and signing only</li>
     *   <li>{@code 3} — form filling, signing, and annotation</li>
     * </ul>
     */
    public int getSigningPermissions() {
        int value = getInt("signing.permissions", 1);
        if (value < 1 || value > 3) {
            return 1;
        }
        return value;
    }

    /**
     * When {@code true} (default), draws a visible signature stamp on the PDF page.
     */
    public boolean isSigningVisible() {
        return getBoolean("signing.visible", true);
    }

    /**
     * Page index for the visible signature ({@code 0}-based). {@code -1} (default) uses the last page.
     */
    public int getSigningVisiblePage() {
        return getInt("signing.visible.page", -1);
    }

    /**
     * When {@code true}, the on-screen signature stamp is included when the PDF is printed.
     * Default {@code false} — stamp is visible in Acrobat but omitted from print output.
     */
    public boolean isSigningVisiblePrint() {
        return getBoolean("signing.visible.print", false);
    }

    /**
     * Sets a property at runtime. Primarily used in tests.
     *
     * @param key property name
     * @param value property value
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Returns a multi-line representation with sensitive values redacted.
     */
    @Override
    public String toString() {
        return properties.entrySet().stream()
                .map(entry -> {
                    var key = entry.getKey().toString();
                    var value = isSensitiveKey(key) ? "***REDACTED***" : entry.getValue().toString();
                    return "  %s=%s".formatted(key, value);
                })
                .collect(java.util.stream.Collectors.joining("\n", "Configuration{\n", "\n}"));
    }

    private boolean isSensitiveKey(String key) {
        var lowerKey = key.toLowerCase();
        return lowerKey.contains("password")
                || lowerKey.contains("token")
                || lowerKey.contains("secret")
                || lowerKey.endsWith(".key")
                || lowerKey.contains("keystore.password");
    }

    /**
     * JDBC connection settings for a single database entry ({@code db1}, {@code db2}, …).
     *
     * @param id database identifier referenced by {@code database.id} in report configs
     * @param url JDBC URL
     * @param username database user, may be {@code null}
     * @param password database password, may be {@code null}
     * @param driver fully qualified JDBC driver class name
     */
    public record DatabaseConfig(
            String id,
            String url,
            String username,
            String password,
            String driver
    ) {

        private static final String DEFAULT_DRIVER = "org.postgresql.Driver";

        /**
         * Compact constructor that applies defaults and validates the database ID.
         */
        public DatabaseConfig {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Database ID cannot be null or blank");
            }
            driver = driver != null ? driver : DEFAULT_DRIVER;
        }

        /**
         * Creates an empty placeholder configuration for the given database ID.
         */
        public DatabaseConfig(String id) {
            this(id, null, null, null, DEFAULT_DRIVER);
        }

        /**
         * Returns a debug representation without the password.
         */
        @Override
        public String toString() {
            return "DatabaseConfig{id='%s', url='%s', username='%s', driver='%s'}"
                    .formatted(id, url, username, driver);
        }
    }
}

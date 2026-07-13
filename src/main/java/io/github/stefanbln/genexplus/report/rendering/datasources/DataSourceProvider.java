package io.github.stefanbln.genexplus.report.rendering.datasources;

import io.github.stefanbln.genexplus.report.config.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides JDBC connections and JasperReports data sources for report rendering.
 *
 * <p>This class uses direct {@link DriverManager} connections without pooling, matching the
 * fire-and-quit lifecycle of the CLI tool. At most one active connection is tracked and closed
 * when the provider itself is closed.
 *
 * <p>Database settings are resolved from {@link Configuration#getDatabaseConfig(String)} using
 * IDs such as {@code db1} defined in {@code application.properties}.
 */
public final class DataSourceProvider implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DataSourceProvider.class.getName());

    /** Seconds to wait when validating a newly opened JDBC connection. */
    private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 5;

    private final Configuration configuration;
    private Connection activeConnection;

    /**
     * Creates a provider for the given application configuration.
     *
     * @param configuration shared settings containing {@code dbN.*} properties
     * @throws IllegalArgumentException if {@code configuration} is {@code null}
     */
    public DataSourceProvider(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
        this.configuration = configuration;
    }

    /**
     * Opens a JDBC connection for the given database ID.
     *
     * <p>The connection is validated with {@link Connection#isValid(int)} before being returned.
     * The opened connection is tracked and closed when this provider is closed.
     *
     * @param dbId database identifier such as {@code db1}
     * @return open JDBC connection
     * @throws SQLException when the ID is unknown, the URL is missing, or the connection fails
     */
    public Connection getConnection(String dbId) throws SQLException {
        if (dbId == null || dbId.isBlank()) {
            LOGGER.warning("No database ID provided");
            return null;
        }

        var dbConfig = configuration.getDatabaseConfig(dbId);
        if (dbConfig == null) {
            throw new SQLException("No configuration found for database: " + dbId);
        }
        return getConnection(dbConfig, dbId);
    }

    /**
     * Opens a JDBC connection using an explicit merged database configuration.
     *
     * <p>Used after {@link io.github.stefanbln.genexplus.report.rendering.datasources.DatabaseProfileResolver} has merged
     * {@code application.properties} and optional {@code .jrdax} settings.
     *
     * @param dbConfig merged JDBC configuration (must include a non-blank URL)
     * @param logLabel profile name printed in logs (e.g. {@code db1})
     * @return open, validated JDBC connection
     * @throws SQLException when configuration is invalid or the connection fails
     */
    public Connection getConnection(Configuration.DatabaseConfig dbConfig, String logLabel) throws SQLException {
        if (dbConfig == null) {
            throw new SQLException("Database configuration must not be null");
        }

        if (dbConfig.url() == null || dbConfig.url().isBlank()) {
            throw new SQLException("No URL configured for database: " + logLabel);
        }

        int loginTimeout = configuration.getInt("report.db.loginTimeoutSeconds", 10);
        int previousLoginTimeout = DriverManager.getLoginTimeout();
        try {
            try {
                DriverManager.setLoginTimeout(loginTimeout);
            } catch (Exception ignored) {
                // not supported on all JDBC drivers
            }

            LOGGER.log(Level.INFO, "Connecting to database: {0}", logLabel);
            LOGGER.log(Level.FINE, "URL: {0}", redactJdbcUrl(dbConfig.url()));

            if (dbConfig.driver() != null && !dbConfig.driver().isBlank()) {
                loadDatabaseDriver(dbConfig.driver());
            }

            Connection conn;
            if (dbConfig.username() != null && !dbConfig.username().isBlank()) {
                conn = DriverManager.getConnection(dbConfig.url(), dbConfig.username(), dbConfig.password());
            } else {
                conn = DriverManager.getConnection(dbConfig.url());
            }

            if (!conn.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS)) {
                conn.close();
                throw new SQLException("Connection to " + logLabel + " could not be validated");
            }

            this.activeConnection = conn;
            LOGGER.log(Level.INFO, "Connected to database: {0}", logLabel);
            return conn;
        } finally {
            try {
                DriverManager.setLoginTimeout(previousLoginTimeout);
            } catch (Exception ignored) {
                // not supported on all JDBC drivers
            }
        }
    }

    /**
     * Closes the currently tracked JDBC connection, if any.
     */
    @Override
    public void close() {
        if (activeConnection != null) {
            try {
                if (!activeConnection.isClosed()) {
                    activeConnection.close();
                    LOGGER.fine("Database connection closed");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close database connection: {0}", e.getMessage());
            } finally {
                activeConnection = null;
            }
        }
    }

    private void loadDatabaseDriver(String driverClassName) throws SQLException {
        try {
            Class.forName(driverClassName);
            LOGGER.log(Level.FINE, "Loaded JDBC driver: {0}", driverClassName);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found: " + driverClassName, e);
        }
    }

    /**
     * Redacts credentials and sensitive query parameters from a JDBC URL for safe logging.
     */
    private static String redactJdbcUrl(String url) {
        if (url == null) {
            return null;
        }
        String redacted = url;
        redacted = redacted.replaceAll("(?i)(password)=([^&;]+)", "$1=***");
        redacted = redacted.replaceAll("(?i)(pwd)=([^&;]+)", "$1=***");
        redacted = redacted.replaceAll("(?i)(access_token)=([^&;]+)", "$1=***");
        redacted = redacted.replaceAll("(?i)(secret)=([^&;]+)", "$1=***");
        redacted = redacted.replaceAll("(?i)(key)=([^&;]+)", "$1=***");
        redacted = redacted.replaceAll("(?i)(://[^/:@]+):[^@/]+@", "$1:***@");
        return redacted;
    }
}

package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;

import java.io.IOException;

/**
 * Shared helpers for optional integration and E2E tests that require live JDBC databases.
 */
public final class DatabaseTestSupport {

    private DatabaseTestSupport() {}

    public static boolean isPostgresAvailable() {
        return isDatabaseAvailable("db1");
    }

    public static boolean isMysqlAvailable() {
        return isDatabaseAvailable("db2");
    }

    public static boolean isDatabaseAvailable(String databaseId) {
        try {
            var configuration = resolveTestConfiguration();
            if (configuration.getDatabaseConfig(databaseId) == null
                    || configuration.getString(databaseId + ".url") == null) {
                return false;
            }
            try (var provider = new io.github.stefanbln.genexplus.report.rendering.datasources.DataSourceProvider(
                    configuration)) {
                var connection = provider.getConnection(databaseId);
                return connection != null && connection.isValid(2);
            }
        } catch (IOException | java.sql.SQLException e) {
            return false;
        }
    }

    /**
     * Uses {@code REPORT_DB*} overrides when set; otherwise falls back to {@code e2e/databases.properties}.
     */
    static Configuration resolveTestConfiguration() throws IOException {
        var configuration = new Configuration();
        if (configuration.getString("db1.url") != null || configuration.getString("db2.url") != null) {
            return configuration;
        }
        return new Configuration(TestResources.materializeClasspathResource(
                "e2e/databases.properties", "genexplus-db-test-").toString());
    }
}

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

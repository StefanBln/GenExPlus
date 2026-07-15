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
import io.github.stefanbln.genexplus.report.rendering.datasources.DataSourceProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DataSourceProvider}.
 */
class DataSourceProviderTest {

    private Configuration configuration;
    private DataSourceProvider provider;

    @BeforeEach
    void setUp() {
        configuration = new Configuration();
        provider = new DataSourceProvider(configuration);
    }

    @AfterEach
    void tearDown() {
        if (provider != null) {
            provider.close();
        }
    }

    @Test
    void testConstructorWithNullConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new DataSourceProvider(null));
    }

    @Test
    void testGetConnectionWithNullId() throws SQLException {
        Connection connection = provider.getConnection(null);
        assertNull(connection);

        connection = provider.getConnection("");
        assertNull(connection);
    }

    @Test
    void testGetConnectionWithInvalidId() {
        assertThrows(SQLException.class, () -> provider.getConnection("nonexistent"));
    }

    @Test
    @Tag("integration")
    void testGetConnectionForConfiguredDatabase() throws SQLException {
        Assumptions.assumeTrue(DatabaseTestSupport.isPostgresAvailable(), "PostgreSQL not available");

        var connection = provider.getConnection("db1");

        assertNotNull(connection);
        assertTrue(connection.isValid(2));
        assertFalse(connection.isClosed());

        connection.close();
    }

    @Test
    void testClose() {
        assertDoesNotThrow(() -> provider.close());
        assertDoesNotThrow(() -> provider.close());
    }

    @Test
    @Tag("integration")
    void testCloseWithActiveConnection() throws SQLException {
        Assumptions.assumeTrue(DatabaseTestSupport.isPostgresAvailable(), "PostgreSQL not available");

        var conn = provider.getConnection("db1");
        assertNotNull(conn);
        assertFalse(conn.isClosed());

        provider.close();
        assertTrue(conn.isClosed());
    }

    @Test
    void testAutoCloseable() {
        try (DataSourceProvider autoProvider = new DataSourceProvider(configuration)) {
            assertNotNull(autoProvider);
        }
    }

    @Test
    void testDatabaseConfigValidation() {
        Configuration invalidConfig = new Configuration();
        invalidConfig.setProperty("db3.username", "user");

        DataSourceProvider invalidProvider = new DataSourceProvider(invalidConfig);

        SQLException exception = assertThrows(SQLException.class,
            () -> invalidProvider.getConnection("db3"));

        assertNotNull(exception.getMessage());
    }

    @Test
    void testRestoresLoginTimeoutAfterConnectionFailure() throws SQLException {
        Configuration config = new Configuration();
        config.setProperty("db9.url", "jdbc:postgresql://127.0.0.1:1/none");
        config.setProperty("db9.driver", "org.postgresql.Driver");
        config.setProperty("db9.username", "user");
        config.setProperty("db9.password", "secret");

        int marker = 42;
        DriverManager.setLoginTimeout(marker);

        try (DataSourceProvider failingProvider = new DataSourceProvider(config)) {
            assertThrows(SQLException.class, () -> failingProvider.getConnection("db9"));
            assertEquals(marker, DriverManager.getLoginTimeout());
        }
    }
}

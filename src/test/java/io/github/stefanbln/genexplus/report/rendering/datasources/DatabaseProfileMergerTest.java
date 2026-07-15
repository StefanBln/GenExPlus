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

package io.github.stefanbln.genexplus.report.rendering.datasources;

import io.github.stefanbln.genexplus.report.config.Configuration;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseProfileMergerTest {

  @Test
  void applicationPropertiesOverrideJrdaxCredentials() {
    var adapter = new JdbcAdapterDefinition("db1", "org.postgresql.Driver",
        "jdbc:postgresql://jrdax-host/db", "jrdax-user", "jrdax-pass");
    var props = new Configuration.DatabaseConfig("db1",
        "jdbc:postgresql://prod-host/db", "prod-user", "prod-pass", "org.postgresql.Driver");

    var merged = DatabaseProfileMerger.merge("db1", Optional.of(adapter), props);

    assertEquals("jdbc:postgresql://prod-host/db", merged.url());
    assertEquals("prod-user", merged.username());
    assertEquals("prod-pass", merged.password());
    assertEquals("org.postgresql.Driver", merged.driver());
  }

  @Test
  void jrdaxFillsMissingPropertyFields() {
    var adapter = new JdbcAdapterDefinition("db1", "org.postgresql.Driver",
        "jdbc:postgresql://localhost/db", "user", "pass");
    var props = new Configuration.DatabaseConfig("db1", null, null, null, null);

    var merged = DatabaseProfileMerger.merge("db1", Optional.of(adapter), props);

    assertEquals("jdbc:postgresql://localhost/db", merged.url());
    assertEquals("user", merged.username());
    assertTrue(DatabaseProfileMerger.credentialsFromAdapterOnly(Optional.of(adapter), props));
  }

  @Test
  void credentialsPartiallyFromAdapterWhenUrlInPropertiesOnly() {
    var adapter = new JdbcAdapterDefinition("db1", "org.postgresql.Driver",
        "jdbc:postgresql://jrdax-host/db", "jrdax-user", "jrdax-pass");
    var props = new Configuration.DatabaseConfig("db1",
        "jdbc:postgresql://prod-host/db", null, null, "org.postgresql.Driver");

    assertTrue(DatabaseProfileMerger.credentialsPartiallyFromAdapter(Optional.of(adapter), props));
    assertFalse(DatabaseProfileMerger.credentialsFromAdapterOnly(Optional.of(adapter), props));
  }
}

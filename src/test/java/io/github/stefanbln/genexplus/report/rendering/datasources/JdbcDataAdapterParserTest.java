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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JdbcDataAdapterParserTest {

  @Test
  void parsesClasspathDb1Adapter() throws Exception {
    try (var in = getClass().getClassLoader().getResourceAsStream("db1.jrdax")) {
      assertNotNull(in);
      var adapter = JdbcDataAdapterParser.parse(in);
      assertEquals("db1", adapter.name());
      assertEquals("org.postgresql.Driver", adapter.driver());
      assertTrue(adapter.url().contains("postgresql"));
      assertEquals("postgres", adapter.username());
    }
  }

  @Test
  void rejectsNonJdbcAdapter() {
    var xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <jsonDataAdapter class="net.sf.jasperreports.data.json.JsonDataAdapterImpl">
          <name>json</name>
        </jsonDataAdapter>
        """;
    assertThrows(IOException.class, () -> JdbcDataAdapterParser.parse(
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
  }
}

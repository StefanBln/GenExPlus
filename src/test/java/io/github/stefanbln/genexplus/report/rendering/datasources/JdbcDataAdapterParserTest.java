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

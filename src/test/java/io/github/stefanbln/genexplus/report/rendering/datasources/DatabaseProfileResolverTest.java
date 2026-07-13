package io.github.stefanbln.genexplus.report.rendering.datasources;

import io.github.stefanbln.genexplus.report.config.Configuration;
import io.github.stefanbln.genexplus.report.config.ReportConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseProfileResolverTest {

  @TempDir
  Path tempDir;

  @Test
  void infersProfileFromTemplateWhenDatabaseIdOmitted() throws Exception {
    var appConfig = new Configuration("");
    var resolver = new DatabaseProfileResolver(getClass().getClassLoader(), appConfig);

    var reportConfig = loadConfig("""
        report.database.optional=false
        report.template=test-template.jrxml
        report.output.dir=%s
        report.output.filename=out.pdf
        report.format=PDF
        """.formatted(tempDir));

    var profile = resolver.resolve(reportConfig, appConfig);

    assertEquals("db1", profile.profileId());
    assertEquals(ResolvedDatabaseProfile.ProfileIdSource.TEMPLATE_ADAPTER, profile.idSource());
  }

  @Test
  void explicitDatabaseIdWinsOverTemplate() throws Exception {
    var appConfig = new Configuration("");
    var resolver = new DatabaseProfileResolver(getClass().getClassLoader(), appConfig);

    var reportConfig = loadConfig("""
        database.id=db2
        report.database.optional=false
        report.template=test-template.jrxml
        report.output.dir=%s
        report.output.filename=out.pdf
        report.format=PDF
        """.formatted(tempDir));

    var profile = resolver.resolve(reportConfig, appConfig);

    assertEquals("db2", profile.profileId());
    assertEquals(ResolvedDatabaseProfile.ProfileIdSource.REPORT_CONF, profile.idSource());
  }

  @Test
  void loadsClasspathJrdaxAndMergesProperties() throws Exception {
    var propsFile = tempDir.resolve("app.properties");
    Files.writeString(propsFile, """
            db1.url=jdbc:postgresql://merged-host/postgres
            db1.username=merged-user
            db1.password=merged-pass
            db1.driver=org.postgresql.Driver
            """);
    var appConfig = new Configuration(propsFile.toString());
    var resolver = new DatabaseProfileResolver(getClass().getClassLoader(), appConfig);

    var reportConfig = loadConfig("""
        database.id=db1
        report.template=test-template.jrxml
        report.output.dir=%s
        report.output.filename=out.pdf
        report.format=PDF
        """.formatted(tempDir));

    var profile = resolver.resolve(reportConfig, appConfig);

    assertTrue(profile.adapterFile().isPresent());
    assertEquals("jdbc:postgresql://merged-host/postgres", profile.jdbc().url());
    assertEquals("merged-user", profile.jdbc().username());
    assertFalse(profile.credentialsFromAdapterFile());
  }

  private static ReportConfig loadConfig(String content) throws Exception {
    var file = Files.createTempFile("report-", ".conf");
    Files.writeString(file, content);
    return ReportConfig.load(file.toString());
  }
}

package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Configuration}.
 */
class ConfigurationTest {
    
    @TempDir
    Path tempDir;
    
    private Configuration configuration;
    
    @BeforeEach
    void setUp() {
        // Setze System-Properties zurück
        System.clearProperty("report.test");
        System.clearProperty("db1.url");
    }
 
    
    @Test
    void missingExplicitConfigFileThrows() {
        assertThrows(IOException.class,
                () -> new Configuration("/path/that/does/not/exist/application.properties"));
    }

    @Test
    void testCustomConfigFile() throws Exception {
        // Erstelle temporäre Konfigurationsdatei
        File configFile = new File(tempDir.toFile(), "custom.properties");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("test.property=customValue\n");
            writer.write("test.number=42\n");
            writer.write("test.boolean=true\n");
        }
        
        configuration = new Configuration(configFile.getAbsolutePath());
        
        assertEquals("customValue", configuration.getString("test.property"));
        assertEquals(42, configuration.getInt("test.number", 0));
        assertTrue(configuration.getBoolean("test.boolean"));
    }
    
    @Test
    void testSystemPropertyOverride() {
        // Setze System-Property
        System.setProperty("report.test", "systemValue");
        
        configuration = new Configuration();
        
        // System-Property sollte Vorrang haben
        assertEquals("systemValue", configuration.getString("report.test"));
    }
    
    @Test
    void testEnvironmentVariableOverride() {
        // Hinweis: Umgebungsvariablen können nicht zur Laufzeit gesetzt werden
        // Dieser Test prüft nur die Logik mit einer vordefinierten Variable
        configuration = new Configuration();
        
        // Prüfe ob REPORT_KEYSTORE_PASSWORD korrekt verarbeitet würde
        var password = configuration.getKeystorePassword();
        // Kann leer sein wenn nicht gesetzt
        assertTrue(password.isEmpty() || password.get().isEmpty() == false);
    }
    
    
    @Test
    void testGetStringWithDefault() {
        configuration = new Configuration();

        assertEquals("defaultValue", configuration.getString("non.existing", "defaultValue"));
        assertEquals("default", configuration.getString("db1.url", "default"));
    }
    
    @Test
    void testGetBoolean() {
        configuration = new Configuration();
        configuration.setProperty("test.true", "true");
        configuration.setProperty("test.yes", "yes");
        configuration.setProperty("test.one", "1");
        configuration.setProperty("test.false", "false");
        configuration.setProperty("test.no", "no");
        
        assertTrue(configuration.getBoolean("test.true"));
        assertTrue(configuration.getBoolean("test.yes"));
        assertTrue(configuration.getBoolean("test.one"));
        assertFalse(configuration.getBoolean("test.false"));
        assertFalse(configuration.getBoolean("test.no"));
        assertFalse(configuration.getBoolean("non.existing"));
        assertTrue(configuration.getBoolean("non.existing", true));
    }
    
    @Test
    void testGetInt() {
        configuration = new Configuration();
        configuration.setProperty("test.int", "42");
        configuration.setProperty("test.invalid", "notanumber");
        
        assertEquals(42, configuration.getInt("test.int", 0));
        assertEquals(0, configuration.getInt("test.invalid", 0));
        assertEquals(99, configuration.getInt("non.existing", 99));
    }
    
    @Test
    void testSigningConfiguration() {
        configuration = new Configuration();
        configuration.setProperty("signing.keystore.path", "src/test/resources/test-keystore.p12");
        configuration.setProperty("signing.keystore.type", "PKCS12");
        configuration.setProperty("signing.keystore.alias", "testalias");

        assertEquals("src/test/resources/test-keystore.p12", configuration.getKeystorePath().orElse(null));
        assertEquals("PKCS12", configuration.getKeystoreType());
        assertEquals("testalias", configuration.getKeystoreAlias().orElse(null));
    }
    
    @Test
    void testSetProperty() {
        configuration = new Configuration();
        
        configuration.setProperty("dynamic.property", "dynamicValue");
        assertEquals("dynamicValue", configuration.getString("dynamic.property"));
    }
    
    @Test
    void acceptsDbUserAlias() throws IOException {
        var aliasFile = tempDir.resolve("alias.properties").toFile();
        try (var writer = new FileWriter(aliasFile)) {
            writer.write("""
                    db1.url=jdbc:postgresql://localhost/db
                    db1.user=reportuser
                    """);
        }
        var parsed = new Configuration(aliasFile.getAbsolutePath());
        var dbConfig = parsed.getDatabaseConfig("db1");
        assertNotNull(dbConfig);
        assertEquals("reportuser", dbConfig.username());
    }

    @Test
    void testToStringRedaction() {
        configuration = new Configuration();
        configuration.setProperty("normal.property", "visibleValue");
        configuration.setProperty("database.password", "secretPassword");
        configuration.setProperty("api.token", "secretToken");
        configuration.setProperty("secret.key", "secretKey");
        
        String output = configuration.toString();
        
        // Normal properties should remain visible
        assertTrue(output.contains("visibleValue"));

        // Sensitive values should be redacted
        assertFalse(output.contains("secretPassword"));
        assertFalse(output.contains("secretToken"));
        assertFalse(output.contains("secretKey"));
        assertTrue(output.contains("***REDACTED***"));
    }
    
    @Test
    void testPropertyLayering() throws Exception {
        File configFile = new File(tempDir.toFile(), "overlay.properties");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("report.overlay=fileValue\n");
        }

        System.setProperty("report.overlay", "systemValue");

        try {
            configuration = new Configuration(configFile.getAbsolutePath());
            assertEquals("systemValue", configuration.getString("report.overlay"));
        } finally {
            System.clearProperty("report.overlay");
        }
    }
}
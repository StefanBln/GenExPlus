package io.github.stefanbln.genexplus.report.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateMetadataReaderTest {

    @Test
    void readsDefaultDataAdapterFromClasspathTemplate() {
        var adapter = TemplateMetadataReader.readDefaultDataAdapter(
                "test-template.jrxml", getClass().getClassLoader());
        assertTrue(adapter.isPresent());
        assertEquals("db1", adapter.get());
    }

    @Test
    void detectsSqlQueryInTemplate() {
        assertTrue(TemplateMetadataReader.containsSqlQuery(
                "test-template.jrxml", getClass().getClassLoader()));
        assertFalse(TemplateMetadataReader.containsSqlQuery(
                "simple.jrxml", getClass().getClassLoader()));
    }

    @Test
    void readsMysqlTemplateAdapter() {
        var adapter = TemplateMetadataReader.readDefaultDataAdapter(
                "test-template-mysql.jrxml", getClass().getClassLoader());
        assertEquals("db2", adapter.orElseThrow());
    }
}

package io.github.stefanbln.genexplus.report.rendering.exporters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvExporterTest {

    @Test
    void defaultDelimiterIsSemicolon() {
        assertEquals(";", new CsvExporter().getFieldDelimiter());
    }

    @Test
    void acceptsCustomDelimiter() {
        assertEquals("|", new CsvExporter("|").getFieldDelimiter());
    }

    @Test
    void rejectsEmptyDelimiter() {
        assertThrows(IllegalArgumentException.class, () -> new CsvExporter(""));
        assertThrows(IllegalArgumentException.class, () -> new CsvExporter(null));
    }
}

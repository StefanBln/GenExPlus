package io.github.stefanbln.genexplus.report.rendering.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

import java.io.OutputStream;
import java.util.Objects;

/**
 * Exports reports to delimiter-separated CSV text.
 */
public final class CsvExporter implements Exporter {

    private static final String DEFAULT_FIELD_DELIMITER = ";";
    private static final String RECORD_DELIMITER = "\n";

    private final String fieldDelimiter;

    /** Creates an exporter with the default semicolon delimiter. */
    public CsvExporter() {
        this(DEFAULT_FIELD_DELIMITER);
    }

    /**
     * Creates an exporter with a custom field delimiter.
     *
     * @param fieldDelimiter delimiter between CSV columns
     */
    public CsvExporter(String fieldDelimiter) {
        if (fieldDelimiter == null || fieldDelimiter.isEmpty()) {
            throw new IllegalArgumentException("CSV field delimiter must not be empty");
        }
        this.fieldDelimiter = fieldDelimiter;
    }

    @Override
    public void export(JasperPrint jasperPrint, OutputStream outputStream) throws JRException {
        JRCsvExporter exporter = new JRCsvExporter();

        SimpleCsvExporterConfiguration config = new SimpleCsvExporterConfiguration();
        config.setFieldDelimiter(fieldDelimiter);
        config.setRecordDelimiter(RECORD_DELIMITER);

        exporter.setConfiguration(config);
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
        exporter.exportReport();
    }

    @Override
    public String getFormat() {
        return "CSV";
    }

    String getFieldDelimiter() {
        return fieldDelimiter;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CsvExporter other && Objects.equals(fieldDelimiter, other.fieldDelimiter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldDelimiter);
    }
}

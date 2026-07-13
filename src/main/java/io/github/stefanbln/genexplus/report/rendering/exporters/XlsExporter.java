package io.github.stefanbln.genexplus.report.rendering.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsExporterConfiguration;
import net.sf.jasperreports.poi.export.JRXlsExporter;

import java.io.OutputStream;

/**
 * Exports reports to legacy Excel BIFF format ({@code .xls}).
 *
 * <p>Requires the {@code jasperreports-excel-poi} module on the classpath.
 */
public final class XlsExporter implements Exporter {

    @Override
    public void export(JasperPrint jasperPrint, OutputStream outputStream) throws JRException {
        JRXlsExporter exporter = new JRXlsExporter();

        SimpleXlsExporterConfiguration config = new SimpleXlsExporterConfiguration();
        exporter.setConfiguration(config);
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();
    }

    @Override
    public String getFormat() {
        return "XLS";
    }
}

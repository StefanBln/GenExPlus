package io.github.stefanbln.genexplus.report.rendering.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxExporterConfiguration;

import java.io.OutputStream;

/**
 * Exports reports to Office Open XML spreadsheet format ({@code .xlsx}).
 */
public final class XlsxExporter implements Exporter {

    @Override
    public void export(JasperPrint jasperPrint, OutputStream outputStream) throws JRException {
        JRXlsxExporter exporter = new JRXlsxExporter();

        SimpleXlsxExporterConfiguration config = new SimpleXlsxExporterConfiguration();
        exporter.setConfiguration(config);
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();
    }

    @Override
    public String getFormat() {
        return "XLSX";
    }
}

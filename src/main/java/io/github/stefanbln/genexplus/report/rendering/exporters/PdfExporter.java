package io.github.stefanbln.genexplus.report.rendering.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperExportManager;

import java.io.OutputStream;

/**
 * Exports reports to PDF using JasperReports' built-in PDF exporter.
 */
public final class PdfExporter implements Exporter {

    @Override
    public void export(JasperPrint jasperPrint, OutputStream outputStream) throws JRException {
        JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
    }

    @Override
    public String getFormat() {
        return "PDF";
    }
}

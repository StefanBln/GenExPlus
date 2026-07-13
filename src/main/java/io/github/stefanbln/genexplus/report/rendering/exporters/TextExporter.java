package io.github.stefanbln.genexplus.report.rendering.exporters;

import io.github.stefanbln.genexplus.report.config.ExportSettings;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRTextExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleTextReportConfiguration;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

import java.io.OutputStream;

/**
 * Exports reports to plain text using JasperReports' text layout exporter.
 */
public final class TextExporter implements Exporter {

    private final int pageWidthChars;
    private final int pageHeightChars;
    private final float charWidth;
    private final float charHeight;

    /** Creates an exporter with default layout dimensions. */
    public TextExporter() {
        this(new ExportSettings(";", 120, 60, 6f, 12f, null, null));
    }

    /**
     * Creates an exporter using the given export settings.
     */
    public TextExporter(ExportSettings settings) {
        this.pageWidthChars = settings.textPageWidthChars();
        this.pageHeightChars = settings.textPageHeightChars();
        this.charWidth = settings.textCharWidth();
        this.charHeight = settings.textCharHeight();
    }

    @Override
    public void export(JasperPrint jasperPrint, OutputStream outputStream) throws JRException {
        var textConfig = new SimpleTextReportConfiguration();
        textConfig.setPageWidthInChars(pageWidthChars);
        textConfig.setPageHeightInChars(pageHeightChars);
        textConfig.setCharWidth(charWidth);
        textConfig.setCharHeight(charHeight);

        JRTextExporter exporter = new JRTextExporter();
        exporter.setConfiguration(textConfig);
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
        exporter.exportReport();
    }

    @Override
    public String getFormat() {
        return "TEXT";
    }
}

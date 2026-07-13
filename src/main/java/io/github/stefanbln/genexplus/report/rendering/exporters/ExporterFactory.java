package io.github.stefanbln.genexplus.report.rendering.exporters;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of supported report export formats.
 *
 * <p>All built-in exporters are registered statically at class load time. Format names are
 * compared case-insensitively.
 */
public final class ExporterFactory {

    private static final Map<String, Exporter> EXPORTERS = new HashMap<>();

    static {
        register(new PdfExporter());
        register(new XlsxExporter());
        register(new XlsExporter());
        register(new CsvExporter());
        register(new TextExporter());
    }

    private ExporterFactory() {}

    private static void register(Exporter exporter) {
        EXPORTERS.put(exporter.getFormat().toUpperCase(), exporter);
    }

    /**
     * Returns the exporter for the given format name.
     *
     * @param format export format (case-insensitive)
     * @return matching exporter, or {@code null} when the format is unknown
     */
    public static Exporter getExporter(String format) {
        if (format == null) {
            return null;
        }
        return EXPORTERS.get(format.toUpperCase());
    }

    /**
     * Returns whether the given format is supported.
     *
     * @param format export format (case-insensitive)
     * @return {@code true} when an exporter is registered
     */
    public static boolean isSupported(String format) {
        return format != null && EXPORTERS.containsKey(format.toUpperCase());
    }

    /**
     * Returns the set of supported format names in upper case.
     *
     * @return unmodifiable view of registered format names
     */
    public static Set<String> getSupportedFormats() {
        return Set.copyOf(EXPORTERS.keySet());
    }
}

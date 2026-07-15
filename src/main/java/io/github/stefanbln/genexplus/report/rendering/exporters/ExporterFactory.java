/*
 * Copyright 2026 Stefan Schuetz - Locivera - Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

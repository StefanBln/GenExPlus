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

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

import java.io.OutputStream;

/**
 * Strategy interface for exporting a filled JasperReports document to a binary format.
 *
 * <p>Implementations are registered in {@link ExporterFactory} and selected by format name.
 */
public interface Exporter {

    /**
     * Writes the filled report to the given output stream.
     *
     * @param jasperPrint filled report produced by JasperReports
     * @param outputStream destination stream; the caller owns the stream lifecycle
     * @throws JRException when JasperReports export fails
     */
    void export(JasperPrint jasperPrint, OutputStream outputStream) throws JRException;

    /**
     * Returns the canonical format name handled by this exporter.
     *
     * @return uppercase format identifier such as {@code PDF} or {@code XLSX}
     */
    String getFormat();
}

/**
 * JasperReports output format exporters.
 *
 * <p>Each {@link io.github.stefanbln.genexplus.report.rendering.exporters.Exporter} converts a filled
 * {@code JasperPrint} into a byte stream for a specific format. Supported formats are
 * registered centrally in {@link io.github.stefanbln.genexplus.report.rendering.exporters.ExporterFactory}.
 *
 * <h2>Supported formats</h2>
 * <ul>
 *   <li>{@code PDF} — portable document format</li>
 *   <li>{@code XLSX} — Office Open XML spreadsheet</li>
 *   <li>{@code XLS} — legacy Excel BIFF format</li>
 *   <li>{@code CSV} — delimiter-separated text ({@code report.csv.delimiter}, default {@code ;})</li>
 *   <li>{@code TEXT} — plain text layout ({@code report.text.*} dimensions)</li>
 * </ul>
 */
package io.github.stefanbln.genexplus.report.rendering.exporters;

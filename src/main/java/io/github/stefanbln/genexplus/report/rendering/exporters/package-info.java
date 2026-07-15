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

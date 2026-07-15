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
 * GenExPlus command-line report generator.
 *
 * <p>GenExPlus is a batch tool: each invocation loads a report job file and shared application
 * properties, renders one JasperReports template, writes the output file, optionally signs PDF
 * output and sends email, then exits with a scheduler-friendly status code.
 *
 * <h2>Entry points</h2>
 * <ul>
 *   <li>{@link io.github.stefanbln.genexplus.report.Main#main(String[])} — CLI entry; configures headless
 *       JasperReports defaults via {@link io.github.stefanbln.genexplus.report.JasperRuntimeSupport}</li>
 *   <li>{@link io.github.stefanbln.genexplus.report.ReportExecutor#execute(io.github.stefanbln.genexplus.report.ArgumentParser.Arguments)}
 *       — full orchestration pipeline</li>
 *   <li>{@link io.github.stefanbln.genexplus.report.Renderer} — compile/fill/export for embedding in other tools</li>
 * </ul>
 *
 * <h2>Typical execution flow</h2>
 * <ol>
 *   <li>Parse CLI with {@link io.github.stefanbln.genexplus.report.ArgumentParser}</li>
 *   <li>Load {@link io.github.stefanbln.genexplus.report.config.Configuration} from {@code application.properties}</li>
 *   <li>Load {@link io.github.stefanbln.genexplus.report.config.ReportConfig} from the {@code .conf} job file</li>
 *   <li>Validate via {@link io.github.stefanbln.genexplus.report.ReportConfigValidator}</li>
 *   <li>Render with {@link io.github.stefanbln.genexplus.report.Renderer}; resolve output path with
 *       {@link io.github.stefanbln.genexplus.report.OutputPathResolver}</li>
 *   <li>Optionally sign PDF via {@link io.github.stefanbln.genexplus.report.signing.PdfSigningService}</li>
 *   <li>Optionally deliver via {@link io.github.stefanbln.genexplus.report.delivery.EmailDeliveryService}</li>
 *   <li>Return {@link io.github.stefanbln.genexplus.report.ExitCodes}</li>
 * </ol>
 *
 * <h2>Operator documentation</h2>
 * User-facing guides live in the repository {@code docs/} folder (FAQ, how-tos, configuration
 * reference). Generate HTML JavaDoc with {@code mvn javadoc:javadoc}.
 *
 * @see io.github.stefanbln.genexplus.report.config
 * @see io.github.stefanbln.genexplus.report.delivery
 * @see io.github.stefanbln.genexplus.report.signing
 * @see io.github.stefanbln.genexplus.report.rendering.datasources
 * @see io.github.stefanbln.genexplus.report.rendering.exporters
 */
package io.github.stefanbln.genexplus.report;

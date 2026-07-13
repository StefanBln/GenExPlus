/**
 * Configuration loading for GenExPlus.
 *
 * <p>{@link io.github.stefanbln.genexplus.report.config.Configuration} holds shared application settings
 * (database connections, SMTP, signing, governor limits) loaded from
 * {@code application.properties}, system properties, and {@code REPORT_*} environment
 * variables.
 *
 * <p>{@link io.github.stefanbln.genexplus.report.config.ReportConfig} holds per-report job settings loaded
 * from a {@code .conf} file (template path, output location, format, parameters, email
 * delivery options, and {@link io.github.stefanbln.genexplus.report.config.ExportSettings}).
 *
 * <h2>Configuration files</h2>
 * <ul>
 *   <li>{@code application.properties} — global settings, one file per deployment</li>
 *   <li>{@code report.conf} — one file per scheduled or on-demand report job</li>
 * </ul>
 */
package io.github.stefanbln.genexplus.report.config;

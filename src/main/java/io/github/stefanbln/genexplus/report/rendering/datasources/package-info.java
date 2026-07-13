/**
 * JDBC data source resolution and connection management for JasperReports.
 *
 * <p>This package implements the Tier 1 + Tier 2 database hybrid:
 * <ul>
 *   <li>{@link io.github.stefanbln.genexplus.report.rendering.datasources.DatabaseProfileResolver} — profile name + JDBC merge</li>
 *   <li>{@link io.github.stefanbln.genexplus.report.rendering.datasources.DataAdapterLocator} — {@code .jrdax} discovery</li>
 *   <li>{@link io.github.stefanbln.genexplus.report.rendering.datasources.JdbcDataAdapterParser} — Studio adapter parsing</li>
 *   <li>{@link io.github.stefanbln.genexplus.report.rendering.datasources.DataSourceProvider} — {@link java.sql.Connection} lifecycle</li>
 * </ul>
 *
 * <p>Connections are not pooled; the provider follows a fire-and-quit lifecycle suitable for batch CLI execution.
 */
package io.github.stefanbln.genexplus.report.rendering.datasources;

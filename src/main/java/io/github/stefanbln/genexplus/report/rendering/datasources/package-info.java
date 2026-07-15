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

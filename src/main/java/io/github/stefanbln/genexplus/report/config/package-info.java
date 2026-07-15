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

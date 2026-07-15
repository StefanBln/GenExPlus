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
 * SMTP email delivery for generated reports.
 *
 * <p>Email is optional and controlled by two layers of configuration:
 * <ul>
 *   <li>Global SMTP settings in {@code application.properties} ({@code mail.smtp.*})</li>
 *   <li>Per-report delivery settings in the report {@code .conf} file
 *       ({@code report.email.*})</li>
 * </ul>
 *
 * <p>The main classes are {@link io.github.stefanbln.genexplus.report.delivery.EmailConfig} for SMTP
 * settings, {@link io.github.stefanbln.genexplus.report.delivery.EmailMessage} for the message payload,
 * and {@link io.github.stefanbln.genexplus.report.delivery.EmailDeliveryService} for sending.
 */
package io.github.stefanbln.genexplus.report.delivery;

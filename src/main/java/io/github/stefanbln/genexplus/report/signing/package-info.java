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
 * Post-export PDF digital signatures.
 *
 * <p>When {@code signing.enabled=true} in {@link io.github.stefanbln.genexplus.report.config.Configuration},
 * {@link io.github.stefanbln.genexplus.report.ReportExecutor} renders the PDF first, then
 * {@link io.github.stefanbln.genexplus.report.signing.PdfSigningService} applies a detached CMS signature
 * using Apache PDFBox and BouncyCastle.
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>Job format must be {@code PDF}</li>
 *   <li>PKCS#12 (or configured type) keystore with private key entry</li>
 *   <li>Password via {@code signing.keystore.password} or {@code REPORT_KEYSTORE_PASSWORD}</li>
 * </ul>
 *
 * <h2>Configuration keys</h2>
 * See {@link io.github.stefanbln.genexplus.report.signing.SigningConfig},
 * <a href="../../../../../../docs/PDF_SIGNING.md">PDF_SIGNING.md</a>, and
 * <a href="../../../../../../docs/CONFIGURATION.md">CONFIGURATION.md</a>.
 *
 * @see io.github.stefanbln.genexplus.report.signing.SigningConfig
 * @see io.github.stefanbln.genexplus.report.signing.PdfSigningService
 */
package io.github.stefanbln.genexplus.report.signing;

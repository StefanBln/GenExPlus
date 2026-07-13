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

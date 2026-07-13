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

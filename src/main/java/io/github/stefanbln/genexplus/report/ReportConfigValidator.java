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

package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;
import io.github.stefanbln.genexplus.report.config.DatabaseValidationMessages;
import io.github.stefanbln.genexplus.report.config.ReportConfig;
import io.github.stefanbln.genexplus.report.config.TemplateMetadataReader;
import io.github.stefanbln.genexplus.report.delivery.EmailConfig;
import io.github.stefanbln.genexplus.report.delivery.EmailMessage;
import io.github.stefanbln.genexplus.report.rendering.datasources.DatabaseProfileResolver;
import io.github.stefanbln.genexplus.report.rendering.datasources.ResolvedDatabaseProfile;
import io.github.stefanbln.genexplus.report.rendering.exporters.ExporterFactory;
import io.github.stefanbln.genexplus.report.signing.SigningConfig;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Validates a loaded {@link ReportConfig} against shared {@link Configuration} settings
 * before report generation starts.
 *
 * <p>Validation is fail-fast: errors are printed to {@code System.err} and the caller receives
 * {@code false}. Warnings (adapter name mismatch, {@code .jrdax} credentials) are also printed
 * to {@code System.err} so they are visible in CLI runs without enabling verbose logging.
 */
public final class ReportConfigValidator {

    private final ClassLoader classLoader;

    /** Uses the application class loader for template and adapter lookup. */
    public ReportConfigValidator() {
        this(ReportConfigValidator.class.getClassLoader());
    }

    /**
     * @param classLoader class loader for classpath templates and {@code .jrdax} files (tests may inject)
     */
    public ReportConfigValidator(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    /**
     * Validates report and application configuration.
     *
     * @param reportConfig per-job report settings
     * @param appConfig shared application settings
     * @return {@code true} when validation passes
     */
    public boolean validate(ReportConfig reportConfig, Configuration appConfig) {
        if (!ExporterFactory.isSupported(reportConfig.getFormat())) {
            System.err.println("Unsupported format: " + reportConfig.getFormat());
            System.err.println("Supported formats: " + ExporterFactory.getSupportedFormats());
            return false;
        }

        var normalizedTemplatePath = PathUtils.normalizeSafe(reportConfig.getTemplate());
        if (!Files.exists(normalizedTemplatePath)
                && classLoader.getResource(reportConfig.getTemplate()) == null) {
            System.err.println("Template file not found: " + reportConfig.getTemplate());
            return false;
        }

        if (!validateOutputDirectory(reportConfig.getOutputDir())) {
            return false;
        }

        if (reportConfig.isAutoTimestamp()
                && !OutputPathResolver.isValidTimestampPattern(reportConfig.getTimestampPattern())) {
            System.err.println("Invalid report.timestamp.pattern: " + reportConfig.getTimestampPattern());
            return false;
        }

        var localeOrTimezoneError = ApplicationSettingsValidator.validateLocaleAndTimezone(appConfig);
        if (localeOrTimezoneError != null) {
            System.err.println(localeOrTimezoneError);
            return false;
        }

        if (reportConfig.isEmailEnabled()) {
            if (reportConfig.getEmailTo().isEmpty()) {
                System.err.println("Email delivery is enabled but report.email.to is empty");
                return false;
            }

            var smtpError = EmailConfig.validateForReporting(appConfig);
            if (smtpError != null) {
                System.err.println(smtpError);
                return false;
            }

            if (!validateRecipientAddresses(reportConfig)) {
                return false;
            }
        }

        if (appConfig.isSigningEnabled()) {
            if (!"PDF".equals(reportConfig.getFormat())) {
                System.err.println("PDF signing is enabled but report format is "
                        + reportConfig.getFormat() + " (only PDF is supported)");
                return false;
            }

            var signingError = SigningConfig.validateForReporting(appConfig);
            if (signingError != null) {
                System.err.println(signingError);
                return false;
            }
        }

        if (!validateDatabaseReference(reportConfig, appConfig)) {
            return false;
        }

        warnIfAdapterNameMismatch(reportConfig, appConfig);
        return true;
    }

    /**
     * Warns when JRXML {@code defaultdataadapter} differs from explicit {@code database.id}.
     * Does not fail validation — only prints to stderr when {@code report.database.warnOnAdapterMismatch=true}.
     */
    private void warnIfAdapterNameMismatch(ReportConfig reportConfig, Configuration appConfig) {
        if (!appConfig.getBoolean("report.database.warnOnAdapterMismatch", true)) {
            return;
        }

        var explicitId = reportConfig.getDatabaseId();
        if (explicitId == null || explicitId.isBlank()) {
            return;
        }

        var adapterName = TemplateMetadataReader.readDefaultDataAdapter(reportConfig.getTemplate(), classLoader);
        if (adapterName.isPresent() && !adapterName.get().equals(explicitId.trim())) {
            System.err.println("Warning: "
                    + DatabaseValidationMessages.adapterMismatch(adapterName.get(), explicitId.trim()));
        }
    }

    /**
     * Validates JDBC profile resolution
     *
     * <p>Rejects SQL templates without a resolvable profile, missing JDBC configuration,
     * unparseable {@code .jrdax} files, and compiled {@code .jasper} templates without {@code database.id}.
     */
    private boolean validateDatabaseReference(ReportConfig reportConfig, Configuration appConfig) {
        var template = reportConfig.getTemplate();
        var explicitId = reportConfig.getDatabaseId();
        if (TemplateMetadataReader.isCompiledTemplate(template)
                && (explicitId == null || explicitId.isBlank())) {
            System.err.println(DatabaseValidationMessages.compiledTemplateRequiresDatabaseId());
            return false;
        }

        var resolver = new DatabaseProfileResolver(classLoader, appConfig);
        var profile = resolver.resolve(reportConfig, appConfig);

        if (profile.adapterLoadError().isPresent() && profile.adapterFile().isPresent()) {
            System.err.println(DatabaseValidationMessages.adapterParseFailure(
                    profile.adapterFile().get().lookup(),
                    profile.adapterLoadError().get()));
            return false;
        }

        if (!profile.requiresConnection()) {
            if (TemplateMetadataReader.containsSqlQuery(template, classLoader)) {
                System.err.println(DatabaseValidationMessages.sqlTemplateWithoutDatabase());
                return false;
            }
            if (reportConfig.isDatabaseOptional()) {
                return true;
            }
            System.err.println("database.id is required when report.database.optional=false "
                    + "(or set defaultdataadapter in the JRXML template)");
            return false;
        }

        if (!profile.hasResolvableJdbcUrl()) {
            System.err.println(DatabaseValidationMessages.missingDatabaseConfig(
                    profile.profileId(), appConfig.getConfiguredDatabaseIds()));
            return false;
        }

        emitAdapterCredentialWarnings(profile);

        return true;
    }

    private static void emitAdapterCredentialWarnings(ResolvedDatabaseProfile profile) {
        if (profile.credentialsFromAdapterFile()) {
            System.err.println(DatabaseValidationMessages.credentialsFromAdapterFile(profile.profileId()));
        } else if (profile.credentialsPartiallyFromAdapter()) {
            System.err.println(DatabaseValidationMessages.credentialsPartiallyFromAdapterFile(profile.profileId()));
        }
    }

    /**
     * Checks that the output directory exists or can be created and is writable.
     */
    private static boolean validateOutputDirectory(String outputDir) {
        try {
            Path dir = PathUtils.normalizeSafe(outputDir).toAbsolutePath().normalize();
            if (Files.exists(dir)) {
                if (!Files.isDirectory(dir)) {
                    System.err.println("Invalid output directory: not a directory: " + dir);
                    return false;
                }
                if (!Files.isWritable(dir)) {
                    System.err.println("Invalid output directory: not writable: " + dir);
                    return false;
                }
                return true;
            }

            Path parent = dir.getParent();
            while (parent != null && !Files.exists(parent)) {
                parent = parent.getParent();
            }
            if (parent != null && !Files.isWritable(parent)) {
                System.err.println("Invalid output directory: cannot create " + dir);
                return false;
            }
            return true;
        } catch (InvalidPathException e) {
            System.err.println("Invalid output directory: " + outputDir);
            return false;
        }
    }

    private static boolean validateRecipientAddresses(ReportConfig reportConfig) {
        return validateAddressList("report.email.to", reportConfig.getEmailTo())
                && validateAddressList("report.email.cc", reportConfig.getEmailCc())
                && validateAddressList("report.email.bcc", reportConfig.getEmailBcc());
    }

    private static boolean validateAddressList(String settingName, java.util.List<String> addresses) {
        for (var address : addresses) {
            if (!EmailMessage.isValidAddress(address)) {
                System.err.println("Invalid email address in " + settingName + ": " + address);
                return false;
            }
        }
        return true;
    }
}

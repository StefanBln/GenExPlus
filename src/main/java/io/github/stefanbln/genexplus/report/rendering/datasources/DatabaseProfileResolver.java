package io.github.stefanbln.genexplus.report.rendering.datasources;

import io.github.stefanbln.genexplus.report.config.Configuration;
import io.github.stefanbln.genexplus.report.config.ReportConfig;
import io.github.stefanbln.genexplus.report.config.TemplateMetadataReader;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves JDBC database profiles for a report job (Tier 1 + Tier 2 hybrid).
 *
 * <h2>Tier 1 — profile name</h2>
 * <ol>
 *   <li>{@code database.id} in {@code report.conf} when set</li>
 *   <li>{@code defaultdataadapter} from the JRXML template when {@code database.id} is omitted</li>
 * </ol>
 *
 * <h2>Tier 2 — JDBC settings</h2>
 * <ol>
 *   <li>Load optional {@code .jrdax} for the profile name via {@link DataAdapterLocator}</li>
 *   <li>Overlay {@code dbN.*} from {@code application.properties} (deployment wins per field)</li>
 * </ol>
 *
 * <p>Compiled {@code .jasper} templates cannot use Tier 1 inference; callers must set
 * {@code database.id} explicitly (enforced in {@link io.github.stefanbln.genexplus.report.ReportConfigValidator}).
 */
public final class DatabaseProfileResolver {

    private static final Logger LOGGER = Logger.getLogger(DatabaseProfileResolver.class.getName());

    private final ClassLoader classLoader;
    private final DataAdapterLocator adapterLocator;

    /**
     * Creates a resolver for the given class loader and shared application configuration.
     *
     * @param classLoader used to load classpath templates and {@code .jrdax} files
     * @param configuration shared {@code application.properties} settings
     */
    public DatabaseProfileResolver(ClassLoader classLoader, Configuration configuration) {
        this.classLoader = classLoader;
        this.adapterLocator = new DataAdapterLocator(classLoader, configuration);
    }

    /**
     * Resolves the effective JDBC profile for a report job.
     *
     * @param reportConfig per-job settings from {@code report.conf}
     * @param configuration shared deployment settings
     * @return resolved profile; {@link ResolvedDatabaseProfile#profileId()} may be blank when no JDBC is needed
     */
    public ResolvedDatabaseProfile resolve(ReportConfig reportConfig, Configuration configuration) {
        var explicitId = normalize(reportConfig.getDatabaseId());
        var templateAdapter = TemplateMetadataReader.readDefaultDataAdapter(
                reportConfig.getTemplate(), classLoader);

        String profileId;
        ResolvedDatabaseProfile.ProfileIdSource idSource;
        if (!explicitId.isBlank()) {
            profileId = explicitId;
            idSource = ResolvedDatabaseProfile.ProfileIdSource.REPORT_CONF;
        } else if (templateAdapter.isPresent()) {
            profileId = templateAdapter.get();
            idSource = ResolvedDatabaseProfile.ProfileIdSource.TEMPLATE_ADAPTER;
            LOGGER.log(Level.FINE, "Inferred database profile ''{0}'' from template defaultdataadapter",
                    profileId);
        } else {
            return emptyProfile();
        }

        return resolveForProfileId(profileId, idSource, reportConfig, configuration);
    }

    private ResolvedDatabaseProfile resolveForProfileId(
            String profileId,
            ResolvedDatabaseProfile.ProfileIdSource idSource,
            ReportConfig reportConfig,
            Configuration configuration) {

        Optional<DataAdapterLocator.LocatedAdapter> located = Optional.empty();
        Optional<JdbcAdapterDefinition> adapterDefinition = Optional.empty();
        Optional<String> adapterLoadError = Optional.empty();

        try {
            located = adapterLocator.locateExplicitJrxmlPath(reportConfig.getTemplate());
            if (located.isEmpty()) {
                located = adapterLocator.locate(profileId, reportConfig.getTemplate());
            }
            if (located.isPresent()) {
                adapterDefinition = Optional.of(located.get().parse());
                LOGGER.log(Level.FINE, "Loaded JDBC data adapter: {0}", located.get().lookup());
            }
        } catch (IOException e) {
            adapterLoadError = Optional.of(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            LOGGER.log(Level.WARNING, "Failed to load data adapter for {0}: {1}",
                    new Object[]{profileId, adapterLoadError.get()});
        }

        var applicationConfig = configuration.getDatabaseConfig(profileId);
        var merged = DatabaseProfileMerger.merge(profileId, adapterDefinition, applicationConfig);
        var credentialsFromAdapter = DatabaseProfileMerger.credentialsFromAdapterOnly(
                adapterDefinition, applicationConfig);
        var credentialsPartiallyFromAdapter = DatabaseProfileMerger.credentialsPartiallyFromAdapter(
                adapterDefinition, applicationConfig);

        if (credentialsFromAdapter || credentialsPartiallyFromAdapter) {
            LOGGER.log(Level.INFO,
                    "JDBC settings for ''{0}'' include values from a .jrdax file; prefer dbN.* in application.properties for production",
                    profileId);
        }

        return new ResolvedDatabaseProfile(
                profileId,
                idSource,
                merged,
                located,
                credentialsFromAdapter,
                credentialsPartiallyFromAdapter,
                adapterLoadError);
    }

    private static ResolvedDatabaseProfile emptyProfile() {
        return new ResolvedDatabaseProfile(
                "",
                ResolvedDatabaseProfile.ProfileIdSource.NONE,
                null,
                Optional.empty(),
                false,
                false,
                Optional.empty());
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }
}

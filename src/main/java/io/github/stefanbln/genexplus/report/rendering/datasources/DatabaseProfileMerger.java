package io.github.stefanbln.genexplus.report.rendering.datasources;

import io.github.stefanbln.genexplus.report.config.Configuration;

import java.util.Optional;

/**
 * Merges JDBC settings from {@code .jrdax} adapter files and {@code dbN.*} application properties.
 *
 * <p><strong>Merge rule:</strong> for each field (URL, username, password, driver), the value from
 * {@code application.properties} wins when non-blank; otherwise the adapter file supplies the default.
 *
 * <p>This class is package-private because callers should use {@link DatabaseProfileResolver}
 * rather than merging settings directly.
 */
final class DatabaseProfileMerger {

    private DatabaseProfileMerger() {}

    /**
     * Builds the effective JDBC configuration for a profile.
     *
     * @param profileId database profile name (e.g. {@code db1})
     * @param adapter optional parsed {@code .jrdax} definition
     * @param applicationConfig optional {@code dbN.*} settings from deployment config
     * @return merged configuration ready for {@link DataSourceProvider}
     */
    static Configuration.DatabaseConfig merge(
            String profileId,
            Optional<JdbcAdapterDefinition> adapter,
            Configuration.DatabaseConfig applicationConfig) {

        var fromProps = applicationConfig != null ? applicationConfig : new Configuration.DatabaseConfig(profileId);
        var fromAdapter = adapter.orElse(null);

        var url = firstNonBlank(fromProps.url(), fromAdapter != null ? fromAdapter.url() : null);
        var username = firstNonBlank(fromProps.username(), fromAdapter != null ? fromAdapter.username() : null);
        var password = firstNonBlank(fromProps.password(), fromAdapter != null ? fromAdapter.password() : null);
        var driver = firstNonBlank(fromProps.driver(), fromAdapter != null ? fromAdapter.driver() : null);

        return new Configuration.DatabaseConfig(profileId, url, username, password, driver);
    }

    /**
     * Returns {@code true} when the adapter file is the sole source of JDBC settings (no URL in properties).
     */
    static boolean credentialsFromAdapterOnly(
            Optional<JdbcAdapterDefinition> adapter,
            Configuration.DatabaseConfig applicationConfig) {
        if (adapter.isEmpty()) {
            return false;
        }
        var props = applicationConfig;
        boolean propsHasUrl = props != null && isNonBlank(props.url());
        return !propsHasUrl && adapter.get().hasUrl();
    }

    /**
     * Returns {@code true} when deployment supplies the URL but username or password still fall back to the adapter file.
     *
     * <p>This mixed configuration is valid but risky in production (prod URL + dev credentials from a shipped {@code .jrdax}).
     */
    static boolean credentialsPartiallyFromAdapter(
            Optional<JdbcAdapterDefinition> adapter,
            Configuration.DatabaseConfig applicationConfig) {
        if (adapter.isEmpty()) {
            return false;
        }
        var props = applicationConfig;
        if (props == null || !isNonBlank(props.url())) {
            return false;
        }
        var fromAdapter = adapter.get();
        boolean userFromAdapter = !isNonBlank(props.username()) && isNonBlank(fromAdapter.username());
        boolean passFromAdapter = !isNonBlank(props.password()) && isNonBlank(fromAdapter.password());
        return userFromAdapter || passFromAdapter;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (isNonBlank(primary)) {
            return primary;
        }
        if (isNonBlank(fallback)) {
            return fallback;
        }
        return null;
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }
}

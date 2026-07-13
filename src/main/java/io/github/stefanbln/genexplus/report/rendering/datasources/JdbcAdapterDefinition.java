package io.github.stefanbln.genexplus.report.rendering.datasources;

/**
 * Parsed JDBC fields from a Jaspersoft Studio {@code .jrdax} data adapter file.
 *
 * @param name adapter display name (usually matches {@code database.id})
 * @param driver JDBC driver class
 * @param url JDBC URL
 * @param username database user
 * @param password database password (may originate from the adapter file — override in production)
 */
public record JdbcAdapterDefinition(
        String name,
        String driver,
        String url,
        String username,
        String password
) {
    /**
     * Returns {@code true} when the adapter file declares a non-blank JDBC URL.
     */
    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }
}

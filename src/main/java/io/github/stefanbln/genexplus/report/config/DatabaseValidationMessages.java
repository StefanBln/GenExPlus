package io.github.stefanbln.genexplus.report.config;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * User-facing validation messages for database configuration.
 *
 * <p>All methods return multi-line strings suitable for printing to {@code System.err}.
 * Wording is intentionally actionable: each message states the problem, what to configure,
 * and (when helpful) what is already configured.
 */
public final class DatabaseValidationMessages {

    private DatabaseValidationMessages() {}

    /**
     * Message when a profile name is known but no JDBC URL could be resolved from properties or {@code .jrdax}.
     *
     * @param dbId requested profile name
     * @param configuredIds profile names found in {@code application.properties}
     */
    public static String missingDatabaseConfig(String dbId, Collection<String> configuredIds) {
        var builder = new StringBuilder();
        builder.append("Database '").append(dbId).append("' is referenced but not configured in application.properties")
                .append(System.lineSeparator())
                .append("  Required keys: ").append(dbId).append(".url")
                .append(" (and usually ").append(dbId).append(".username, ")
                .append(dbId).append(".password)").append(System.lineSeparator())
                .append("  Note: use ").append(dbId).append(".username — not ").append(dbId).append(".user");

        if (configuredIds.isEmpty()) {
            builder.append(System.lineSeparator())
                    .append("  Configured database profiles: (none)");
        } else {
            builder.append(System.lineSeparator())
                    .append("  Configured database profiles: ")
                    .append(configuredIds.stream().sorted().collect(Collectors.joining(", ")));
        }

        var suggestion = suggestClosestProfile(dbId, configuredIds);
        if (suggestion != null) {
            builder.append(System.lineSeparator())
                    .append("  Did you mean '").append(suggestion).append("'?");
        }
        return builder.toString();
    }

    /**
     * Message when a SQL template has no resolvable database profile name.
     */
    public static String sqlTemplateWithoutDatabase() {
        return """
                Template contains SQL but no database profile could be resolved.
                  Set database.id=db1 in report.conf, or defaultdataadapter=db1 in the JRXML template,
                  then configure db1.url (and credentials) in application.properties or ship db1.jrdax.""";
    }

    /**
     * Message when a compiled {@code .jasper} template is used without {@code database.id}.
     */
    public static String compiledTemplateRequiresDatabaseId() {
        return """
                Compiled .jasper templates require explicit database.id in report.conf.
                  Tier 1 profile inference (from defaultdataadapter) only works with .jrxml source templates.""";
    }

    /**
     * Message when a {@code .jrdax} file was found but could not be parsed.
     *
     * @param lookup classpath or filesystem path that was located
     * @param error parse failure detail
     */
    public static String adapterParseFailure(String lookup, String error) {
        return "Found JDBC data adapter '" + lookup + "' but failed to parse it: " + error;
    }

    /**
     * Warning when Studio adapter name and explicit {@code database.id} differ.
     */
    public static String adapterMismatch(String adapterName, String databaseId) {
        return "Template defaultdataadapter='" + adapterName + "' but database.id='" + databaseId + "'. "
                + "GenExPlus uses database.id when set; otherwise it infers the profile from defaultdataadapter. "
                + "Align both names for consistent Studio preview and CLI runs.";
    }

    /**
     * Warning when JDBC credentials still come from a shipped {@code .jrdax} file.
     */
    public static String credentialsFromAdapterFile(String profileId) {
        return "Warning: JDBC settings for '" + profileId + "' come from a .jrdax file. "
                + "Configure " + profileId + ".url, .username, and .password in application.properties for production.";
    }

    /**
     * Warning when URL is from properties but username/password still come from {@code .jrdax}.
     */
    public static String credentialsPartiallyFromAdapterFile(String profileId) {
        return "Warning: database '" + profileId + "' uses application.properties for the URL but "
                + profileId + ".username or " + profileId + ".password still come from a .jrdax file. "
                + "Set all credential fields in application.properties for production.";
    }

    private static String suggestClosestProfile(String requestedId, Collection<String> configuredIds) {
        if (configuredIds.isEmpty() || configuredIdEquals(requestedId, configuredIds)) {
            return null;
        }
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (var candidate : configuredIds) {
            int distance = levenshtein(requestedId, candidate);
            if (distance < bestDistance && distance <= 2) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean configuredIdEquals(String requestedId, Collection<String> configuredIds) {
        return configuredIds.stream().anyMatch(id -> id.equals(requestedId));
    }

    private static int levenshtein(String left, String right) {
        int[][] costs = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            costs[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            costs[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int matchCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                costs[i][j] = Math.min(
                        Math.min(costs[i - 1][j] + 1, costs[i][j - 1] + 1),
                        costs[i - 1][j - 1] + matchCost);
            }
        }
        return costs[left.length()][right.length()];
    }
}

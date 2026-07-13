package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.Configuration;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Validates shared {@link Configuration} settings used during report rendering.
 */
final class ApplicationSettingsValidator {

    private static final String KEY_LOCALE = "report.default.locale";
    private static final String KEY_TIMEZONE = "report.default.timezone";
    private static final String DEFAULT_LOCALE = "en_US";
    private static final String DEFAULT_TIMEZONE = "UTC";

    private ApplicationSettingsValidator() {}

    /**
     * Validates locale and timezone settings that affect JasperReports formatting.
     *
     * @return {@code null} when valid, otherwise a human-readable error message
     */
    static String validateLocaleAndTimezone(Configuration appConfig) {
        var localeTag = appConfig.getString(KEY_LOCALE, DEFAULT_LOCALE);
        var localeError = validateLocale(localeTag);
        if (localeError != null) {
            return localeError;
        }

        var timezoneId = appConfig.getString(KEY_TIMEZONE, DEFAULT_TIMEZONE);
        return validateTimezone(timezoneId);
    }

    private static String validateLocale(String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return "Invalid " + KEY_LOCALE + ": must not be blank";
        }

        var normalizedTag = localeTag.replace('_', '-');
        var locale = Locale.forLanguageTag(normalizedTag);
        if (locale.getLanguage().isEmpty()) {
            return "Invalid " + KEY_LOCALE + ": unrecognized language tag '" + localeTag + "'";
        }
        return null;
    }

    private static String validateTimezone(String timezoneId) {
        if (timezoneId == null || timezoneId.isBlank()) {
            return "Invalid " + KEY_TIMEZONE + ": must not be blank";
        }

        var timezone = TimeZone.getTimeZone(timezoneId);
        if ("GMT".equals(timezone.getID())
                && !"GMT".equalsIgnoreCase(timezoneId)
                && !"UTC".equalsIgnoreCase(timezoneId)) {
            return "Invalid " + KEY_TIMEZONE + ": unrecognized timezone ID '" + timezoneId + "'";
        }
        return null;
    }
}

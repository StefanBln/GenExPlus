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

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

package io.github.stefanbln.genexplus.report.config;

import java.util.Properties;

/**
 * Per-report export layout and governor overrides.
 *
 * <p>Loaded from a report job {@code .conf} file with fallbacks to
 * {@link io.github.stefanbln.genexplus.report.config.Configuration}. Passed to
 * {@link io.github.stefanbln.genexplus.report.Renderer} for CSV delimiter, TEXT dimensions, and optional
 * governor limits that override application defaults for one run.
 *
 * @param csvDelimiter field separator for CSV export (default {@code ;})
 * @param textPageWidthChars TEXT export page width in characters
 * @param textPageHeightChars TEXT export page height in characters
 * @param textCharWidth TEXT character cell width in pixels
 * @param textCharHeight TEXT character cell height in pixels
 * @param governorMaxPages per-job max pages, or {@code null} to use application default
 * @param governorTimeoutSeconds per-job timeout in seconds, or {@code null} for app default
 */
public record ExportSettings(
        String csvDelimiter,
        int textPageWidthChars,
        int textPageHeightChars,
        float textCharWidth,
        float textCharHeight,
        Integer governorMaxPages,
        Integer governorTimeoutSeconds
) {

    private static final String DEFAULT_CSV_DELIMITER = ";";
    private static final int DEFAULT_TEXT_PAGE_WIDTH_CHARS = 120;
    private static final int DEFAULT_TEXT_PAGE_HEIGHT_CHARS = 60;
    private static final float DEFAULT_TEXT_CHAR_WIDTH_PX = 6f;
    private static final float DEFAULT_TEXT_CHAR_HEIGHT_PX = 12f;

    /**
     * Loads export settings from report job properties with application defaults.
     *
     * @param reportProps keys from the {@code .conf} file
     * @param appConfig shared deployment configuration for fallbacks
     * @return immutable settings for one render pass
     */
    public static ExportSettings from(Properties reportProps, Configuration appConfig) {
        var csvDelimiter = resolveCsvDelimiter(
                reportProps.getProperty("report.csv.delimiter"),
                appConfig);

        var textPageWidth = parsePositiveInt(reportProps.getProperty("report.text.pageWidthChars"),
                appConfig.getInt("report.text.pageWidthChars", DEFAULT_TEXT_PAGE_WIDTH_CHARS));
        var textPageHeight = parsePositiveInt(reportProps.getProperty("report.text.pageHeightChars"),
                appConfig.getInt("report.text.pageHeightChars", DEFAULT_TEXT_PAGE_HEIGHT_CHARS));
        var charWidth = parsePositiveFloat(reportProps.getProperty("report.text.charWidth"),
                parseFloatProperty(appConfig, "report.text.charWidth", DEFAULT_TEXT_CHAR_WIDTH_PX));
        var charHeight = parsePositiveFloat(reportProps.getProperty("report.text.charHeight"),
                parseFloatProperty(appConfig, "report.text.charHeight", DEFAULT_TEXT_CHAR_HEIGHT_PX));

        Integer governorMaxPages = parseOptionalPositiveInt(reportProps.getProperty("report.governor.maxPages"));
        if (governorMaxPages == null) {
            governorMaxPages = parseOptionalPositiveInt(appConfig.getString("report.governor.maxPages"));
        }

        Integer governorTimeoutSeconds = parseOptionalPositiveInt(
                reportProps.getProperty("report.governor.timeoutSeconds"));
        if (governorTimeoutSeconds == null) {
            governorTimeoutSeconds = parseOptionalPositiveInt(appConfig.getString("report.governor.timeoutSeconds"));
        }

        return new ExportSettings(
                csvDelimiter,
                textPageWidth,
                textPageHeight,
                charWidth,
                charHeight,
                governorMaxPages,
                governorTimeoutSeconds);
    }

    private static float parseFloatProperty(Configuration appConfig, String key, float defaultValue) {
        var value = appConfig.getString(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int parsePositiveInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            var parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static float parsePositiveFloat(String value, float defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            var parsed = Float.parseFloat(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Integer parseOptionalPositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            var parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String resolveCsvDelimiter(String reportValue, Configuration appConfig) {
        if (isUsableDelimiter(reportValue)) {
            return reportValue;
        }
        var appValue = appConfig.getString("report.csv.delimiter");
        if (isUsableDelimiter(appValue)) {
            return appValue;
        }
        return DEFAULT_CSV_DELIMITER;
    }

    private static boolean isUsableDelimiter(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.length() == 1) {
            return true;
        }
        return !value.trim().isEmpty();
    }
}

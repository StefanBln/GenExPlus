package io.github.stefanbln.genexplus.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves {@code {[datePattern]}} placeholders in strings using a given timestamp.
 *
 * <p>The pattern inside the braces must be a valid {@link DateTimeFormatter} pattern.
 * Invalid patterns are left unchanged.
 */
public final class PlaceholderResolver {

    private static final Logger LOGGER = Logger.getLogger(PlaceholderResolver.class.getName());

    private PlaceholderResolver() {}

    /**
     * Replaces all {@code {[pattern]}} placeholders in the input text.
     *
     * @param text input text, may be {@code null}
     * @param timestamp timestamp used for formatting
     * @return text with placeholders resolved, or {@code null} when input is {@code null}
     */
    public static String resolve(String text, LocalDateTime timestamp) {
        if (text == null || !text.contains("{[") || !text.contains("]}")) {
            return text;
        }

        var resolved = text;
        var placeholderStart = 0;

        while ((placeholderStart = resolved.indexOf("{[", placeholderStart)) != -1) {
            var placeholderEnd = resolved.indexOf("]}", placeholderStart);
            if (placeholderEnd == -1) {
                break;
            }

            var pattern = resolved.substring(placeholderStart + 2, placeholderEnd);
            try {
                var formatter = DateTimeFormatter.ofPattern(pattern);
                var formatted = timestamp.format(formatter);
                resolved = resolved.substring(0, placeholderStart) + formatted
                        + resolved.substring(placeholderEnd + 2);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid date pattern: {0}", pattern);
                placeholderStart = placeholderEnd + 2;
            }
        }

        return resolved;
    }

    /**
     * Replaces placeholders using the current date and time.
     */
    public static String resolveNow(String text) {
        return resolve(text, LocalDateTime.now());
    }
}

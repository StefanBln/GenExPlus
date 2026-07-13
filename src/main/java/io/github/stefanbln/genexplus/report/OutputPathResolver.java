package io.github.stefanbln.genexplus.report;

import io.github.stefanbln.genexplus.report.config.ReportConfig;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Resolves the final output file path for a report job.
 *
 * <p>Combines {@code report.output.dir} and {@code report.output.filename}, expands
 * {@code {[datePattern]}} placeholders via {@link io.github.stefanbln.genexplus.report.PlaceholderResolver},
 * and optionally appends a timestamp before the file extension when
 * {@code report.timestamp.auto=true}.
 *
 * @see io.github.stefanbln.genexplus.report.PathUtils#resolveOutputFile(String, String)
 */
public final class OutputPathResolver {

    /**
     * Returns whether the given pattern is valid for {@link DateTimeFormatter}.
     */
    public static boolean isValidTimestampPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        try {
            DateTimeFormatter.ofPattern(pattern);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Resolves the final output path for the given report configuration.
     */
    public Path resolve(ReportConfig config) {
        var now = LocalDateTime.now();
        var filename = PlaceholderResolver.resolve(config.getOutputFilename(), now);

        if (config.isAutoTimestamp()) {
            filename = appendTimestamp(filename, now, config.getTimestampPattern());
        }

        return PathUtils.resolveOutputFile(config.getOutputDir(), filename);
    }

    private String appendTimestamp(String path, LocalDateTime now, String pattern) {
        var formatter = DateTimeFormatter.ofPattern(pattern);
        var timestamp = now.format(formatter);

        var lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            return path.substring(0, lastDot) + "_" + timestamp + path.substring(lastDot);
        }
        return path + "_" + timestamp;
    }
}

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

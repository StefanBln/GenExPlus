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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path normalization and validation helpers used when reading configuration and templates.
 *
 * <p>These utilities reject control characters, expand a leading {@code ~} to the user home
 * directory, and resolve existing files without following symbolic links.
 */
public final class PathUtils {

    /** Lowest ASCII control character considered invalid in paths. */
    private static final int MAX_CONTROL_CHARACTER = 31;

    /** ASCII DELETE character, rejected in paths. */
    private static final int DELETE_CHARACTER = 127;

    private PathUtils() {}

    /**
     * Resolves a user-supplied path to a canonical, existing regular file.
     *
     * <p>Symbolic links are not followed ({@link LinkOption#NOFOLLOW_LINKS}).
     *
     * @param pathString path from configuration or CLI input
     * @return canonical path to an existing regular file
     * @throws IOException if the path does not exist or is not a regular file
     * @throws InvalidPathException if the input contains illegal characters
     */
    public static Path resolveExistingSecureFile(String pathString) throws IOException {
        var p = normalizeSafe(pathString);
        var real = p.toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (!Files.isRegularFile(real)) {
            throw new IOException("Path is not a regular file: " + real);
        }
        return real;
    }

    /**
     * Normalizes a path string without requiring the target to exist.
     *
     * <p>Leading {@code ~} is expanded to {@code user.home}. The returned path is normalized
     * but not necessarily absolute.
     *
     * @param pathString path from configuration or CLI input
     * @return normalized path
     * @throws InvalidPathException if the input is null, blank, or contains control characters
     */
    public static Path normalizeSafe(String pathString) {
        if (pathString == null || pathString.isBlank()) {
            throw new InvalidPathException("", "Path must not be null or blank");
        }

        for (int i = 0; i < pathString.length(); i++) {
            char c = pathString.charAt(i);
            if (c <= MAX_CONTROL_CHARACTER || c == DELETE_CHARACTER) {
                throw new InvalidPathException(pathString, "Control characters are not allowed");
            }
        }

        String s = pathString;
        if (s.startsWith("~")) {
            String home = System.getProperty("user.home", "");
            if (!home.isEmpty()) {
                s = home + s.substring(1);
            }
        }
        return Paths.get(s).normalize();
    }

    /**
     * Resolves an output file path under a base directory.
     *
     * <p>The filename must not contain path separators or parent references. The resolved path
     * is guaranteed to stay within {@code outputDir}.
     *
     * @param outputDir configured output directory
     * @param filename configured output filename (no directory component)
     * @return absolute normalized output path
     * @throws ValidationException if the filename escapes the output directory
     */
    public static Path resolveOutputFile(String outputDir, String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ValidationException("Output filename must not be blank");
        }
        if (filename.contains("..") || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
            throw new ValidationException("Output filename must not contain path separators: " + filename);
        }

        var base = normalizeSafe(outputDir).toAbsolutePath().normalize();
        var resolved = base.resolve(filename).normalize();
        if (!resolved.startsWith(base)) {
            throw new ValidationException("Output path escapes output directory: " + filename);
        }
        return resolved;
    }
}

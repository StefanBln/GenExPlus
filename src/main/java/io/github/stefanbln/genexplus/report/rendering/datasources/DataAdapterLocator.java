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

package io.github.stefanbln.genexplus.report.rendering.datasources;

import io.github.stefanbln.genexplus.report.PathUtils;
import io.github.stefanbln.genexplus.report.config.Configuration;
import io.github.stefanbln.genexplus.report.config.TemplateMetadataReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Locates JasperReports Studio {@code .jrdax} JDBC adapter files for a database profile.
 *
 * <p>Search order for {@link #locate(String, String)}:
 * <ol>
 *   <li>{@code net.sf.jasperreports.data.adapter} path declared in the JRXML (if any)</li>
 *   <li>{@code {name}.jrdax} on the classpath</li>
 *   <li>{@code adapters/{name}.jrdax} on the classpath</li>
 *   <li>{@code data-adapters/{name}.jrdax} on the classpath</li>
 *   <li>{@code report.data.adapter.dir}/{name}.jrdax from {@code application.properties}</li>
 *   <li>{@code {name}.jrdax} next to the template file on disk</li>
 * </ol>
 *
 * <p>Only JDBC adapters are supported. The first matching resource wins.
 */
public final class DataAdapterLocator {

    private final ClassLoader classLoader;
    private final Configuration configuration;

    /**
     * @param classLoader class loader for classpath adapter lookup
     * @param configuration shared settings (for {@code report.data.adapter.dir})
     */
    public DataAdapterLocator(ClassLoader classLoader, Configuration configuration) {
        this.classLoader = classLoader;
        this.configuration = configuration;
    }

    /**
     * Finds a JDBC adapter file for the given profile name and template context.
     *
     * @param adapterName profile / adapter name (e.g. {@code db1})
     * @param templatePath path to the JRXML/Jasper template (classpath or filesystem)
     * @return located adapter resource, or empty when none found
     */
    public Optional<LocatedAdapter> locate(String adapterName, String templatePath) {
        if (adapterName == null || adapterName.isBlank()) {
            return Optional.empty();
        }

        for (var candidate : candidatePaths(adapterName, templatePath)) {
            var located = tryLocate(candidate);
            if (located.isPresent()) {
                return located;
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the adapter path declared via {@code net.sf.jasperreports.data.adapter} in the template.
     *
     * @param templatePath path to the template
     * @return located adapter when the property is set and the file exists
     */
    public Optional<LocatedAdapter> locateExplicitJrxmlPath(String templatePath) {
        return TemplateMetadataReader.readRuntimeDataAdapterPath(templatePath, classLoader)
                .flatMap(this::tryLocate);
    }

    private List<String> candidatePaths(String adapterName, String templatePath) {
        var paths = new ArrayList<String>();

        TemplateMetadataReader.readRuntimeDataAdapterPath(templatePath, classLoader).ifPresent(paths::add);

        paths.add(adapterName + ".jrdax");
        paths.add("adapters/" + adapterName + ".jrdax");
        paths.add("data-adapters/" + adapterName + ".jrdax");

        var adapterDir = configuration.getString("report.data.adapter.dir");
        if (adapterDir != null && !adapterDir.isBlank()) {
            paths.add(PathUtils.normalizeSafe(adapterDir).resolve(adapterName + ".jrdax").toString());
        }

        if (templatePath != null && !templatePath.isBlank()) {
            try {
                var template = PathUtils.normalizeSafe(templatePath);
                if (Files.isRegularFile(template)) {
                    paths.add(template.getParent().resolve(adapterName + ".jrdax").toString());
                }
            } catch (RuntimeException ignored) {
                // template may be classpath-only
            }
        }

        return paths;
    }

    private Optional<LocatedAdapter> tryLocate(String lookup) {
        try (InputStream in = classLoader.getResourceAsStream(lookup)) {
            if (in != null) {
                return Optional.of(LocatedAdapter.classpath(lookup, classLoader));
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }

        try {
            var file = PathUtils.resolveExistingSecureFile(lookup);
            if (Files.isRegularFile(file)) {
                return Optional.of(LocatedAdapter.filesystem(file, classLoader));
            }
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * A resolved {@code .jrdax} resource on the classpath or filesystem.
     *
     * @param lookup classpath resource name or filesystem path string used for resolution
     * @param filesystemFile present when the adapter was loaded from disk
     * @param classLoader class loader for classpath parsing
     */
    public record LocatedAdapter(String lookup, Optional<Path> filesystemFile, ClassLoader classLoader) {

        /**
         * Creates a classpath-backed adapter reference.
         */
        static LocatedAdapter classpath(String lookup, ClassLoader classLoader) {
            return new LocatedAdapter(lookup, Optional.empty(), classLoader);
        }

        /**
         * Creates a filesystem-backed adapter reference.
         */
        static LocatedAdapter filesystem(Path file, ClassLoader classLoader) {
            return new LocatedAdapter(file.toString(), Optional.of(file), classLoader);
        }

        /**
         * Parses the located {@code .jrdax} file into a {@link JdbcAdapterDefinition}.
         *
         * @return parsed JDBC settings
         * @throws IOException when the file cannot be read or is not a supported JDBC adapter
         */
        JdbcAdapterDefinition parse() throws IOException {
            if (filesystemFile.isPresent()) {
                return JdbcDataAdapterParser.parse(filesystemFile.get());
            }
            try (InputStream in = classLoader.getResourceAsStream(lookup)) {
                if (in == null) {
                    throw new IOException("Classpath data adapter not found: " + lookup);
                }
                return JdbcDataAdapterParser.parse(in);
            }
        }
    }
}

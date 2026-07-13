package io.github.stefanbln.genexplus.report.config;

import io.github.stefanbln.genexplus.report.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads lightweight metadata from JasperReports templates for validation and profile resolution.
 *
 * <p>Supports {@code .jrxml} source templates via text inspection. Compiled {@code .jasper} files
 * are binary and cannot be scanned for {@code defaultdataadapter} or SQL queries — use
 * {@link #isCompiledTemplate(String)} and require explicit {@code database.id} for those jobs.
 *
 * <p>This class does not open JDBC connections; it only inspects template content.
 */
public final class TemplateMetadataReader {

    private static final Pattern DEFAULT_ADAPTER = Pattern.compile(
            "com\\.jaspersoft\\.studio\\.data\\.defaultdataadapter\"\\s+value=\"([^\"]+)\"");
    private static final Pattern RUNTIME_ADAPTER = Pattern.compile(
            "net\\.sf\\.jasperreports\\.data\\.adapter\"\\s+value=\"([^\"]+)\"");
    private static final Pattern SQL_QUERY = Pattern.compile("<query\\b", Pattern.CASE_INSENSITIVE);

    private TemplateMetadataReader() {}

    /**
     * Returns {@code true} when the template path refers to a compiled Jasper report ({@code .jasper}).
     *
     * <p>Tier 1 profile inference and SQL detection are not available for compiled templates.
     */
    public static boolean isCompiledTemplate(String templatePath) {
        return templatePath != null && templatePath.toLowerCase().endsWith(".jasper");
    }

    /**
     * Returns the Jaspersoft Studio default data adapter name when declared in a JRXML template.
     *
     * <p>Reads the property {@code com.jaspersoft.studio.data.defaultdataadapter}. Returns empty for
     * {@code .jasper} files and when the property is absent.
     *
     * @param templatePath classpath or filesystem template path
     * @param classLoader class loader for classpath templates
     */
    public static Optional<String> readDefaultDataAdapter(String templatePath, ClassLoader classLoader) {
        if (isCompiledTemplate(templatePath)) {
            return Optional.empty();
        }
        return readTemplateText(templatePath, classLoader)
                .flatMap(TemplateMetadataReader::extractDefaultDataAdapter);
    }

    /**
     * Returns the runtime JasperReports data adapter path when declared via
     * {@code net.sf.jasperreports.data.adapter} in the template.
     *
     * @param templatePath classpath or filesystem template path
     * @param classLoader class loader for classpath templates
     */
    public static Optional<String> readRuntimeDataAdapterPath(String templatePath, ClassLoader classLoader) {
        if (isCompiledTemplate(templatePath)) {
            return Optional.empty();
        }
        return readTemplateText(templatePath, classLoader)
                .flatMap(TemplateMetadataReader::extractRuntimeDataAdapter);
    }

    /**
     * Returns {@code true} when the template declares at least one SQL {@code <query>} block.
     *
     * <p>Always returns {@code false} for {@code .jasper} templates because their content is binary.
     *
     * @param templatePath classpath or filesystem template path
     * @param classLoader class loader for classpath templates
     */
    public static boolean containsSqlQuery(String templatePath, ClassLoader classLoader) {
        if (isCompiledTemplate(templatePath)) {
            return false;
        }
        return readTemplateText(templatePath, classLoader)
                .map(text -> SQL_QUERY.matcher(text).find())
                .orElse(false);
    }

    private static Optional<String> extractDefaultDataAdapter(String templateText) {
        return extractPropertyValue(DEFAULT_ADAPTER, templateText);
    }

    private static Optional<String> extractRuntimeDataAdapter(String templateText) {
        return extractPropertyValue(RUNTIME_ADAPTER, templateText);
    }

    /**
     * Returns the first regex match for a template property value.
     */
    private static Optional<String> extractPropertyValue(Pattern pattern, String templateText) {
        var matcher = pattern.matcher(templateText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        var value = matcher.group(1).trim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    /**
     * Loads template text from the classpath or filesystem.
     */
    private static Optional<String> readTemplateText(String templatePath, ClassLoader classLoader) {
        if (templatePath == null || templatePath.isBlank()) {
            return Optional.empty();
        }

        try (InputStream in = classLoader.getResourceAsStream(templatePath)) {
            if (in != null) {
                return Optional.of(new String(in.readAllBytes()));
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }

        try {
            var path = PathUtils.resolveExistingSecureFile(templatePath);
            return Optional.of(Files.readString(path));
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }
}

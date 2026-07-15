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

package io.github.stefanbln.genexplus.report.e2e;

import io.github.stefanbln.genexplus.report.ExitCodes;
import io.github.stefanbln.genexplus.report.Main;
import io.github.stefanbln.genexplus.report.TestResources;

import org.apache.pdfbox.Loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs E2E scenarios through the real {@link Main} CLI entry point.
 *
 * <p>Generated reports are written to a persistent directory so you can inspect outputs
 * after {@code mvn -Pe2e test}. Default: {@code target/e2e-reports/&lt;scenario-id&gt;/}.
 * Override with {@code -Dgenexplus.e2e.outputDir=/path/to/dir}.
 */
public final class E2eTestSupport {

    private static final String OUTPUT_DIR_PLACEHOLDER = "__OUTPUT_DIR__";
    private static final String OUTPUT_DIR_PROPERTY = "genexplus.e2e.outputDir";
    private static final String DEFAULT_OUTPUT_ROOT = "target/e2e-reports";

    private E2eTestSupport() {}

    /**
     * Result of a single scenario run.
     *
     * @param exitCode process exit code
     * @param outputDir directory where the report was written
     * @param resolvedOutput resolved output file when exactly one match was found
     * @param stderr captured standard error (when captured by caller)
     */
    public record RunResult(int exitCode, Path outputDir, Path resolvedOutput, String stderr) {}

    static List<E2eScenario> tierAScenarios() {
        return List.of(
                new E2eScenario("01-pdf-static", ExitCodes.SUCCESS, "static.pdf", OutputKind.PDF),
                new E2eScenario("02-pdf-parameters", ExitCodes.SUCCESS, "parameters.pdf", OutputKind.PDF),
                new E2eScenario("03-xlsx-export", ExitCodes.SUCCESS, "export.xlsx", OutputKind.XLSX),
                new E2eScenario("04-csv-pipe-delimiter", ExitCodes.SUCCESS, "export.csv", OutputKind.CSV, '|'),
                new E2eScenario("05-text-export", ExitCodes.SUCCESS, "export.txt", OutputKind.TEXT),
                new E2eScenario("06-timestamp-filename", ExitCodes.SUCCESS, "dated.pdf", OutputKind.PDF,
                        "dated_*.pdf"),
                new E2eScenario("07-edge-invalid-format", ExitCodes.VALIDATION_ERROR, null, OutputKind.NONE)
        );
    }

    static E2eScenario signedPdfScenario() {
        return new E2eScenario("08-signed-pdf", ExitCodes.SUCCESS, "signed.pdf", OutputKind.PDF_SIGNED);
    }

    static E2eScenario emailDeliveryScenario() {
        return new E2eScenario("09-email-delivery", ExitCodes.SUCCESS, "emailed.pdf", OutputKind.PDF);
    }

    static E2eScenario emailFailureScenario() {
        return new E2eScenario("10-edge-email-fail", ExitCodes.EMAIL_ERROR, "email-fail.pdf", OutputKind.PDF);
    }

    static E2eScenario signingNonPdfScenario() {
        return new E2eScenario("11-edge-signing-non-pdf", ExitCodes.VALIDATION_ERROR, null, OutputKind.NONE);
    }

    static E2eScenario databaseScenario() {
        return new E2eScenario("12-db-postgres", ExitCodes.SUCCESS, "db-report.pdf", OutputKind.PDF);
    }

    static E2eScenario mysqlDatabaseScenario() {
        return new E2eScenario("13-db-mysql", ExitCodes.SUCCESS, "mysql-report.pdf", OutputKind.PDF);
    }

    static E2eScenario missingDatabaseScenario() {
        return new E2eScenario("14-db-missing-config", ExitCodes.VALIDATION_ERROR, null, OutputKind.NONE);
    }

    static E2eScenario sqlWithoutDatabaseScenario() {
        return new E2eScenario("15-db-sql-without-id", ExitCodes.VALIDATION_ERROR, null, OutputKind.NONE);
    }

    static E2eScenario adapterMismatchScenario() {
        return new E2eScenario("16-db-adapter-mismatch", ExitCodes.SUCCESS, "adapter-mismatch.pdf", OutputKind.PDF);
    }

    static E2eScenario inferredDatabaseScenario() {
        return new E2eScenario("17-db-inferred-from-template", ExitCodes.SUCCESS, "inferred-db.pdf", OutputKind.PDF);
    }

    static E2eScenario jrdaxMergeScenario() {
        return new E2eScenario("18-db-jrdax-merge", ExitCodes.SUCCESS, "jrdax-merge.pdf", OutputKind.PDF);
    }

    public static Path defaultE2eDatabaseProperties() throws IOException {
        return TestResources.materializeClasspathResource("e2e/databases.properties", "genexplus-e2e-db-");
    }

    /**
     * Returns the persistent output directory for a scenario.
     *
     * <p>Reports remain on disk after tests complete under {@code target/e2e-reports/}
     * (or the path set via {@code -Dgenexplus.e2e.outputDir}).
     *
     * @param scenarioId scenario folder name, e.g. {@code 01-pdf-static}
     * @return writable directory for the scenario run
     */
    public static Path outputDirFor(String scenarioId) throws IOException {
        var root = Path.of(System.getProperty(OUTPUT_DIR_PROPERTY, DEFAULT_OUTPUT_ROOT));
        var dir = root.resolve(scenarioId);
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Root directory where E2E tests write report artifacts.
     */
    public static Path outputRoot() {
        return Path.of(System.getProperty(OUTPUT_DIR_PROPERTY, DEFAULT_OUTPUT_ROOT));
    }

    /**
     * Runs a scenario using in-process {@link Main#run(String[])} (full pipeline).
     */
    public static RunResult run(E2eScenario scenario, Path outputDir, Path propertiesFile) throws IOException {
        return run(scenario, outputDir, propertiesFile, false);
    }

    /**
     * Runs a scenario and optionally captures stderr (validation messages, warnings).
     */
    public static RunResult run(E2eScenario scenario, Path outputDir, Path propertiesFile, boolean captureStderr)
            throws IOException {
        Files.createDirectories(outputDir);

        var confFile = materializeConf(scenario, outputDir);
        var errCapture = captureStderr ? new java.io.ByteArrayOutputStream() : null;
        var originalErr = System.err;
        if (captureStderr) {
            System.setErr(new java.io.PrintStream(errCapture));
        }

        int exitCode;
        try {
            exitCode = new Main().run(new String[]{
                    "--config", confFile.toString(),
                    "--properties", propertiesFile.toString()
            });
        } finally {
            if (captureStderr) {
                System.setErr(originalErr);
            }
        }

        var resolved = resolveOutputFile(scenario, outputDir);
        var stderr = captureStderr && errCapture != null ? errCapture.toString() : "";
        return new RunResult(exitCode, outputDir, resolved, stderr);
    }

    public static void assertScenario(E2eScenario scenario, RunResult result) throws IOException {
        assertEquals(scenario.expectedExit(), result.exitCode(),
                () -> "unexpected exit for " + scenario.id());

        if (!scenario.expectsOutputFile()) {
            return;
        }

        var output = result.resolvedOutput();
        assertNotNull(output, () -> "no output file resolved for " + scenario.id());
        assertTrue(Files.exists(output), () -> "missing output: " + output);
        assertTrue(Files.size(output) > 0, () -> "empty output: " + output);

        assertOutputKind(scenario, output);
    }

    public static Path defaultE2eProperties() throws IOException {
        return TestResources.materializeClasspathResource("e2e/application.properties", "genexplus-e2e-");
    }

    public static Path materializeResource(String classpathResource) throws IOException {
        return TestResources.materializeClasspathResource(classpathResource, "genexplus-e2e-");
    }

    public static Path copyKeystoreToTemp() throws IOException {
        return TestResources.copyKeystoreToTemp();
    }

    public static Path writePropertiesWithOverrides(Path baseProperties, Map<String, String> overrides)
            throws IOException {
        var props = new java.util.Properties();
        try (var in = Files.newInputStream(baseProperties)) {
            props.load(in);
        }
        overrides.forEach(props::setProperty);
        var dest = Files.createTempFile("genexplus-e2e-props-", ".properties");
        try (var out = Files.newOutputStream(dest)) {
            props.store(out, "GenExPlus E2E overrides");
        }
        return dest;
    }

    private static Path materializeConf(E2eScenario scenario, Path outputDir) throws IOException {
        var resource = scenario.confClasspath();
        try (InputStream in = E2eTestSupport.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "missing scenario conf: " + resource);
            var content = new String(in.readAllBytes())
                    .replace(OUTPUT_DIR_PLACEHOLDER, outputDir.toAbsolutePath().toString());
            var confFile = outputDir.resolve("report.conf");
            Files.writeString(confFile, content);
            return confFile;
        }
    }

    private static Path resolveOutputFile(E2eScenario scenario, Path outputDir) throws IOException {
        if (scenario.outputFilenamePattern() != null) {
            try (Stream<Path> files = Files.list(outputDir)) {
                var matches = files
                        .filter(Files::isRegularFile)
                        .filter(p -> !p.getFileName().toString().equals("report.conf"))
                        .filter(p -> matchesGlob(p.getFileName().toString(), scenario.outputFilenamePattern()))
                        .toList();
                return matches.size() == 1 ? matches.getFirst() : null;
            }
        }
        if (scenario.outputFile() == null) {
            return null;
        }
        var exact = outputDir.resolve(scenario.outputFile());
        return Files.exists(exact) ? exact : null;
    }

    private static boolean matchesGlob(String name, String pattern) {
        var regex = pattern.replace(".", "\\.").replace("*", ".*");
        return name.matches(regex);
    }

    private static void assertOutputKind(E2eScenario scenario, Path output) throws IOException {
        var bytes = Files.readAllBytes(output);
        switch (scenario.outputKind()) {
            case PDF -> assertMagic(bytes, new byte[]{'%', 'P', 'D', 'F'}, "PDF");
            case PDF_SIGNED -> {
                assertMagic(bytes, new byte[]{'%', 'P', 'D', 'F'}, "PDF");
                io.github.stefanbln.genexplus.report.signing.PdfSignatureVerifier.assertCryptographicallySigned(output);
            }
            case XLSX, XLS -> assertMagic(bytes, new byte[]{0x50, 0x4B}, "ZIP/XLSX");
            case CSV -> {
                var text = new String(bytes);
                assertFalse(text.isBlank(), "CSV should not be blank");
                if (scenario.csvDelimiter() != null) {
                    assertTrue(text.contains(String.valueOf(scenario.csvDelimiter())),
                            "CSV should contain delimiter " + scenario.csvDelimiter());
                }
            }
            case TEXT -> {
                var text = new String(bytes);
                assertFalse(text.isBlank(), "TEXT export should not be blank");
            }
            case NONE -> { }
        }
    }

    private static void assertMagic(byte[] data, byte[] magic, String label) {
        assertTrue(data.length >= magic.length, label + " output too short");
        for (int i = 0; i < magic.length; i++) {
            assertEquals(magic[i], data[i], label + " magic byte mismatch at index " + i);
        }
    }
}

package io.github.stefanbln.genexplus.report.e2e;

/**
 * Describes a single E2E scenario loaded from {@code src/test/resources/e2e/scenarios/}.
 *
 * @param id folder name, e.g. {@code 01-pdf-static}
 * @param expectedExit expected process exit code from {@link io.github.stefanbln.genexplus.report.Main}
 * @param outputFile expected output filename, or {@code null} when no file should be written
 * @param outputKind how to validate the output bytes when present
 * @param outputFilenamePattern glob for timestamped outputs (e.g. {@code dated_*.pdf}); overrides
 *                            exact {@code outputFile} matching when set
 * @param csvDelimiter when validating CSV, assert this delimiter appears in content
 */
public record E2eScenario(
        String id,
        int expectedExit,
        String outputFile,
        OutputKind outputKind,
        String outputFilenamePattern,
        Character csvDelimiter
) {

    public E2eScenario(String id, int expectedExit, String outputFile, OutputKind outputKind) {
        this(id, expectedExit, outputFile, outputKind, null, null);
    }

    public E2eScenario(String id, int expectedExit, String outputFile, OutputKind outputKind,
            String outputFilenamePattern) {
        this(id, expectedExit, outputFile, outputKind, outputFilenamePattern, null);
    }

    public E2eScenario(String id, int expectedExit, String outputFile, OutputKind outputKind,
            Character csvDelimiter) {
        this(id, expectedExit, outputFile, outputKind, null, csvDelimiter);
    }

    /** Classpath path to the scenario's {@code report.conf}. */
    public String confClasspath() {
        return "e2e/scenarios/" + id + "/report.conf";
    }

    public boolean expectsOutputFile() {
        return expectedExit == 0 && outputKind != OutputKind.NONE;
    }
}

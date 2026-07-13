package io.github.stefanbln.genexplus.report.e2e;

/**
 * Output validation strategy for an {@link E2eScenario}.
 */
public enum OutputKind {
    /** No output file expected. */
    NONE,
    PDF,
    XLSX,
    XLS,
    CSV,
    TEXT,
    /** PDF with a cryptographically valid detached PKCS#7 CMS signature. */
    PDF_SIGNED
}

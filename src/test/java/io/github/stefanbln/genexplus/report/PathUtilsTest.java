package io.github.stefanbln.genexplus.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveOutputFileStaysInsideBaseDirectory() {
        Path output = PathUtils.resolveOutputFile(tempDir.toString(), "report.pdf");
        assertTrue(output.startsWith(tempDir.toAbsolutePath().normalize()));
        assertEquals("report.pdf", output.getFileName().toString());
    }

    @Test
    void resolveOutputFileRejectsParentTraversal() {
        assertThrows(ValidationException.class,
                () -> PathUtils.resolveOutputFile(tempDir.toString(), "../escape.pdf"));
    }

    @Test
    void resolveOutputFileRejectsPathSeparators() {
        assertThrows(ValidationException.class,
                () -> PathUtils.resolveOutputFile(tempDir.toString(), "subdir/report.pdf"));
    }
}

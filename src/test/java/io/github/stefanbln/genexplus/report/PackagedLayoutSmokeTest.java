package io.github.stefanbln.genexplus.report;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the production classpath layout via {@code start.sh} after packaging.
 */
@Tag("integration")
class PackagedLayoutSmokeTest {

    @TempDir
    Path tempDir;

    @Test
    void packagedLayoutProducesSamplePdf() throws Exception {
        var projectRoot = Path.of("").toAbsolutePath().normalize();
        var smokeScript = projectRoot.resolve("scripts/smoke-packaged.sh");
        assertTrue(Files.isExecutable(smokeScript), "smoke script must be executable: " + smokeScript);

        var process = new ProcessBuilder("/bin/bash", smokeScript.toString())
                .directory(projectRoot.toFile())
                .redirectErrorStream(true)
                .start();

        var output = new String(process.getInputStream().readAllBytes());
        assertTrue(process.waitFor(5, TimeUnit.MINUTES), "smoke script timed out\n" + output);
        assertEquals(0, process.exitValue(), "smoke script failed\n" + output);

        var pdf = projectRoot.resolve("genexplus-output/sample-report.pdf");
        assertTrue(Files.exists(pdf), "expected PDF at " + pdf);
        assertTrue(Files.size(pdf) > 0);

        var header = Files.readAllBytes(pdf);
        assertTrue(header.length >= 4);
        assertEquals('%', (char) header[0]);
        assertEquals('P', (char) header[1]);
        assertEquals('D', (char) header[2]);
        assertEquals('F', (char) header[3]);
    }
}

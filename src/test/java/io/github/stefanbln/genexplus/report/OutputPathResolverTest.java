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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OutputPathResolverTest {

    private final OutputPathResolver resolver = new OutputPathResolver();

    @TempDir
    Path tempDir;

    @Test
    void isValidTimestampPatternAcceptsKnownPattern() {
        assertTrue(OutputPathResolver.isValidTimestampPattern("yyyyMMdd_HHmmss"));
    }

    @Test
    void isValidTimestampPatternRejectsInvalidPattern() {
        assertFalse(OutputPathResolver.isValidTimestampPattern("not-valid-[["));
    }

    @Test
    void resolvesFilenamePlaceholder() throws IOException {
        var config = loadConfig("""
                report.database.optional=true
                report.template=sample-report.jrxml
                report.output.dir=%s
                report.output.filename=report_{[yyyyMMdd]}.pdf
                report.format=PDF
                report.timestamp.auto=false
                """.formatted(tempDir));

        var outputPath = resolver.resolve(config);
        assertTrue(outputPath.getFileName().toString().matches("report_\\d{8}\\.pdf"));
        assertEquals(tempDir.toAbsolutePath().normalize(), outputPath.getParent().toAbsolutePath().normalize());
    }

    @Test
    void appendsTimestampWhenAutoEnabled() throws IOException {
        var config = loadConfig("""
                report.database.optional=true
                report.template=sample-report.jrxml
                report.output.dir=%s
                report.output.filename=monthly.pdf
                report.format=PDF
                report.timestamp.auto=true
                report.timestamp.pattern=yyyy-MM
                """.formatted(tempDir));

        var outputPath = resolver.resolve(config);
        assertTrue(outputPath.getFileName().toString().matches("monthly_\\d{4}-\\d{2}\\.pdf"));
    }

    private ReportConfig loadConfig(String content) throws IOException {
        var configFile = tempDir.resolve("report.conf");
        Files.writeString(configFile, content);
        return ReportConfig.load(configFile.toString());
    }
}

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Main} (config-based CLI).
 *
 * <p>Only {@link #testWithVerboseFlag()} is tagged as integration because it
 * performs a full end-to-end report render.
 */
class MainTest {

    private Main main;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        main = new Main();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testNoArguments() {
        String[] args = {};

        int exitCode = main.run(args);

        assertEquals(1, exitCode);
        assertTrue(outContent.toString().contains("GenEx"));
        assertTrue(outContent.toString().contains("--config"));
    }

    @Test
    void testHelpArgument() {
        String[] args = {"--help"};

        int exitCode = main.run(args);

        assertEquals(0, exitCode);
        assertTrue(outContent.toString().contains("--config"));
    } 

    @Test
    void testMissingConfigArgument() {
        String[] args = {"--verbose"};

        int exitCode = main.run(args);

        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("--config is required"));
    }

    @Test
    void testUnknownArgument() {
        String[] args = {"--unknown"};

        int exitCode = main.run(args);

        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("Unknown argument"));
    }

    @Test
    void testMissingConfigValue() {
        String[] args = {"--config"};

        int exitCode = main.run(args);

        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("Missing value for --config"));
    }

    @Test
    void testNonExistentConfigFile() {
        String[] args = {"--config", "nonexistent.conf"};

        int exitCode = main.run(args);

        assertEquals(1, exitCode);
    }

    @Test
    @Tag("integration")
    void testWithVerboseFlag() throws Exception {
        Path configFile = tempDir.resolve("verbose-test.conf");
        Files.writeString(configFile, """
                report.database.optional=true
                report.template=test-template.jrxml
                report.output.dir=%s
                report.output.filename=verbose-test.pdf
                report.format=PDF
                verbose=true
                """.formatted(tempDir.toString()));

        int exitCode = main.run(new String[]{"--config", configFile.toString(), "--verbose"});
        assertEquals(0, exitCode);
    }

    @Test
    void testConfigWithMissingRequiredFields() throws Exception {
        // Create invalid config (missing required fields)
        Path configFile = tempDir.resolve("invalid.conf");
        Files.writeString(configFile, """
                # Missing database.id, report.template, report.output
                report.format=PDF
                """);

        String[] args = {"--config", configFile.toString()};

        int exitCode = main.run(args);

        assertEquals(1, exitCode);
    }
}

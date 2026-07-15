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

import io.github.stefanbln.genexplus.report.DatabaseTestSupport;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.assertScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.databaseScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.defaultE2eDatabaseProperties;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.inferredDatabaseScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.jrdaxMergeScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.mysqlDatabaseScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.outputDirFor;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.run;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("e2e")
@Tag("e2e-db")
class E2eDatabaseTest {

    @Test
    void fillsReportFromPostgres() throws Exception {
        Assumptions.assumeTrue(DatabaseTestSupport.isPostgresAvailable(),
                "PostgreSQL not available — run ./scripts/test-db-up.sh or set REPORT_DB1_*");

        var properties = defaultE2eDatabaseProperties();
        var scenario = databaseScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties);
        assertScenario(scenario, result);
        assertPdfContains(result.resolvedOutput(), "postgres");
    }

    @Test
    void fillsReportFromMysql() throws Exception {
        Assumptions.assumeTrue(DatabaseTestSupport.isMysqlAvailable(),
                "MySQL not available — run ./scripts/test-db-up.sh or set REPORT_DB2_*");

        var properties = defaultE2eDatabaseProperties();
        var scenario = mysqlDatabaseScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties);
        assertScenario(scenario, result);
        assertPdfContains(result.resolvedOutput(), "genexplus_test");
    }

    @Test
    void infersDatabaseIdFromTemplateAdapter() throws Exception {
        Assumptions.assumeTrue(DatabaseTestSupport.isPostgresAvailable(),
                "PostgreSQL not available — run ./scripts/test-db-up.sh or set REPORT_DB1_*");

        var properties = defaultE2eDatabaseProperties();
        var scenario = inferredDatabaseScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties);
        assertScenario(scenario, result);
        assertPdfContains(result.resolvedOutput(), "postgres");
    }

    @Test
    void mergesJrdaxWithApplicationProperties() throws Exception {
        Assumptions.assumeTrue(DatabaseTestSupport.isPostgresAvailable(),
                "PostgreSQL not available — run ./scripts/test-db-up.sh or set REPORT_DB1_*");

        var properties = defaultE2eDatabaseProperties();
        var scenario = jrdaxMergeScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties);
        assertScenario(scenario, result);
    }

    private static void assertPdfContains(Path pdf, String expectedText) throws Exception {
        try (var document = Loader.loadPDF(pdf.toFile())) {
            var text = new PDFTextStripper().getText(document);
            assertTrue(text.toLowerCase().contains(expectedText.toLowerCase()),
                    () -> "PDF text should contain '" + expectedText + "' but was: " + text);
        }
    }
}

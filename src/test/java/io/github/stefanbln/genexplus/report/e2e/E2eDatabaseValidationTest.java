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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.adapterMismatchScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.assertScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.defaultE2eDatabaseProperties;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.missingDatabaseScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.outputDirFor;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.run;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.sqlWithoutDatabaseScenario;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("e2e")
@Tag("e2e-db")
class E2eDatabaseValidationTest {

    @Test
    void failsWhenDatabaseProfileMissing() throws Exception {
        var properties = defaultE2eDatabaseProperties();
        var scenario = missingDatabaseScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties, true);
        assertScenario(scenario, result);
        assertTrue(result.stderr().contains("db99"),
                () -> "stderr should mention missing profile: " + result.stderr());
        assertTrue(result.stderr().contains("Configured database profiles"),
                () -> "stderr should list configured profiles: " + result.stderr());
    }

    @Test
    void failsWhenSqlTemplateHasNoDatabaseId() throws Exception {
        var properties = defaultE2eDatabaseProperties();
        var scenario = sqlWithoutDatabaseScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties, true);
        assertScenario(scenario, result);
        assertTrue(result.stderr().contains("Template contains SQL but no database profile"),
                () -> "stderr should explain SQL without JDBC: " + result.stderr());
    }

    @Test
    void warnsWhenStudioAdapterDiffersFromDatabaseId() throws Exception {
        Assumptions.assumeTrue(DatabaseTestSupport.isPostgresAvailable(),
                "PostgreSQL not available — run ./scripts/test-db-up.sh or set REPORT_DB1_*");

        var properties = defaultE2eDatabaseProperties();
        var scenario = adapterMismatchScenario();
        var result = run(scenario, outputDirFor(scenario.id()), properties, true);
        assertScenario(scenario, result);
        assertTrue(result.stderr().contains("defaultdataadapter='db2'"),
                () -> "stderr should warn about adapter mismatch: " + result.stderr());
        assertTrue(result.stderr().contains("database.id='db1'"),
                () -> "stderr should mention database.id: " + result.stderr());
    }
}

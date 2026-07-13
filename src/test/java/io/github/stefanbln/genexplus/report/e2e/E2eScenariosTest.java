package io.github.stefanbln.genexplus.report.e2e;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.assertScenario;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.defaultE2eProperties;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.outputDirFor;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.run;
import static io.github.stefanbln.genexplus.report.e2e.E2eTestSupport.tierAScenarios;

/**
 * Tier-A E2E scenarios: full CLI pipeline, no external services.
 */
@Tag("e2e")
class E2eScenariosTest {

    static Stream<E2eScenario> scenarios() {
        return tierAScenarios().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void runsScenario(E2eScenario scenario) throws Exception {
        var properties = defaultE2eProperties();
        var result = run(scenario, outputDirFor(scenario.id()), properties);
        assertScenario(scenario, result);
    }
}

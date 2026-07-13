package io.github.stefanbln.genexplus.report;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command-line entry point for the GenExPlus report generator.
 *
 * <p>GenExPlus is a batch-oriented tool: it loads configuration, renders a single JasperReports
 * template, writes the output file, and optionally sends the result by email. Each invocation
 * is independent; there is no long-running server process.
 *
 * <h2>Exit codes</h2>
 * <ul>
 *   <li>{@code 0} — success</li>
 *   <li>{@code 1} — validation error (arguments, report.conf, preflight checks)</li>
 *   <li>{@code 2} — application.properties or configuration I/O error</li>
 *   <li>{@code 3} — database connection error</li>
 *   <li>{@code 4} — JasperReports compile, fill, or export error</li>
 *   <li>{@code 5} — email delivery failed (report written to disk); set {@code GENEXPLUS_STRICT_EMAIL=false} for legacy exit {@code 0}</li>
 * </ul>
 */
public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        JasperRuntimeSupport.configureHeadlessDefaults();
        System.exit(new Main().run(args));
    }

    public int run(String[] args) {
        try {
            var parser = new ArgumentParser();
            var parseResult = parser.parse(args);

            return switch (parseResult) {
                case ArgumentParser.Exit(int code) -> code;
                case ArgumentParser.Error(String message) -> {
                    System.err.println("Error: " + message);
                    yield ExitCodes.VALIDATION_ERROR;
                }
                case ArgumentParser.Success(var arguments) -> {
                    LoggingConfigurer.configure(arguments.verbose());
                    yield new ReportExecutor().execute(arguments);
                }
            };
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error", e);
            return ExitCodes.RENDER_ERROR;
        }
    }
}

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

import io.github.stefanbln.genexplus.report.rendering.exporters.ExporterFactory;

/**
 * Parses and validates command-line arguments for the GenExPlus report generator.
 *
 * <p>The parser returns a sealed {@link ParseResult} rather than throwing on user input errors,
 * allowing the caller to print a friendly message and choose an exit code.
 *
 * <h2>Supported flags</h2>
 * <ul>
 *   <li>{@code --config <path>} — required path to a report {@code .conf} file</li>
 *   <li>{@code --properties <path>} — optional path to {@code application.properties}</li>
 *   <li>{@code --verbose} — enable fine-grained logging</li>
 *   <li>{@code --version}, {@code -V} — print version and exit with code {@code 0}</li>
 *   <li>{@code --help}, {@code -h} — print usage and exit with code {@code 0}</li>
 * </ul>
 */
public final class ArgumentParser {

    /**
     * Outcome of parsing a command line.
     */
    public sealed interface ParseResult permits Success, Error, Exit {}

    /**
     * Parsing succeeded and the report can be executed.
     *
     * @param arguments resolved CLI arguments
     */
    public record Success(Arguments arguments) implements ParseResult {}

    /**
     * Parsing failed because of invalid or incomplete user input.
     *
     * @param message human-readable error description printed to stderr
     */
    public record Error(String message) implements ParseResult {}

    /**
     * Parsing completed with an explicit exit request (for example {@code --help}).
     *
     * @param code process exit code
     */
    public record Exit(int code) implements ParseResult {}

    /**
     * Resolved command-line arguments for a single report run.
     *
     * @param configFile path to the report {@code .conf} file
     * @param propertiesFile path to {@code application.properties}, or {@code null} to use the
     *                       classpath default
     * @param verbose whether verbose logging was requested on the command line
     */
    public record Arguments(
        String configFile,
        String propertiesFile,
        boolean verbose
    ) {}

    /**
     * Parses the given command-line arguments.
     *
     * @param args raw CLI arguments
     * @return a {@link Success}, {@link Error}, or {@link Exit} result
     */
    public ParseResult parse(String[] args) {
        if (args.length == 0) {
            printUsage();
            return new Error("No arguments provided");
        }

        String configFile = null;
        String propertiesFile = null;
        boolean verbose = false;

        for (int i = 0; i < args.length; i++) {
            var token = args[i];
            switch (token) {
                case "--help", "-h" -> {
                    printUsage();
                    return new Exit(0);
                }
                case "--version", "-V" -> {
                    System.out.println("GenExPlus " + resolveVersion());
                    return new Exit(0);
                }
                case "--config" -> {
                    if (++i >= args.length) {
                        return new Error("Missing value for --config");
                    }
                    configFile = args[i];
                }
                case "--properties" -> {
                    if (++i >= args.length) {
                        return new Error("Missing value for --properties");
                    }
                    propertiesFile = args[i];
                }
                case "--verbose" -> verbose = true;
                default -> {
                    return new Error("Unknown argument: " + token);
                }
            }
        }

        if (configFile == null) {
            return new Error("--config is required");
        }

        return new Success(new Arguments(configFile, propertiesFile, verbose));
    }

    private void printUsage() {
        var formats = String.join(", ", ExporterFactory.getSupportedFormats());
        System.out.println("""

            GenExPlus Report Generator
            Usage: java -jar genexplus.jar --config <report.conf> --properties <application.properties> [OPTIONS]

            Required arguments:
              --config <path>       Path to the report configuration file (.conf)
              --properties <path>   Path to application.properties (optional if on classpath)

            Optional arguments:
              --verbose             Enable detailed logging
              --version, -V         Show version information
              --help, -h            Show this help message

            Report configuration file (report.conf):
              database.id=db1                           # Optional when report.database.optional=true
              report.database.optional=true             # Allow DB-less reports (omit database.id)
              report.template=template.jrxml            # Required: template path or classpath resource
              report.output.dir=/path/to/output         # Required: output directory
              report.output.filename=report.pdf         # Required: output filename
              report.format=PDF                         # Optional: %s (default: PDF)
              report.parameter.Title=Monthly Report     # Optional: JasperReports parameters
              report.parameter.Year=2025                # Optional: JasperReports parameters
              verbose=false                             # Optional: verbose logging for this report

            Notes:
              - Exit codes: 0 success, 1 validation, 2 config, 3 database, 4 render, 5 email failed
              - signing.enabled=true requires PDF output and a configured PKCS12 keystore
              - Output filenames support timestamp placeholders: {[yyyyMMdd_HHmm]}
              - See report.conf.example for a complete sample

            Example:
              java -jar genexplus.jar --config reports/monthly-report.conf --properties /path/to/application.properties
              java -jar genexplus.jar --config reports/monthly-report.conf --properties /path/to/application.properties --verbose

            """.formatted(formats));
    }

    private static String resolveVersion() {
        var version = ArgumentParser.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }
}

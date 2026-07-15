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

/**
 * Process exit codes returned by {@link io.github.stefanbln.genexplus.report.Main} and
 * {@link io.github.stefanbln.genexplus.report.ReportExecutor}.
 *
 * <p>Schedulers and shell scripts can branch on these values. Scripts that only test
 * {@code exit != 0} remain compatible with earlier releases that used a single non-zero code.
 *
 * <h2>Usage in cron</h2>
 * <pre>{@code
 * ./start.sh nightly.conf || case $? in
 *   5) echo "Report written; email failed" ;;
 *   *) echo "Hard failure" ;;
 * esac
 * }</pre>
 *
 * @see io.github.stefanbln.genexplus.report.ReportExecutor#execute(io.github.stefanbln.genexplus.report.ArgumentParser.Arguments)
 */
public final class ExitCodes {

    /** Report generated successfully; email succeeded or was disabled. */
    public static final int SUCCESS = 0;

    /** Invalid CLI arguments, report job file, or preflight validation failure. */
    public static final int VALIDATION_ERROR = 1;

    /** {@code application.properties} missing, unreadable, or corrupt. */
    public static final int CONFIG_ERROR = 2;

    /** JDBC connection or query failure during fill. */
    public static final int DATABASE_ERROR = 3;

    /** JasperReports compile, fill, export failure, or PDF signing error. */
    public static final int RENDER_ERROR = 4;

    /**
     * Email delivery failed after successful write to disk.
     * Override with {@code GENEXPLUS_STRICT_EMAIL=false} for legacy exit {@link #SUCCESS}.
     */
    public static final int EMAIL_ERROR = 5;

    private ExitCodes() {}
}

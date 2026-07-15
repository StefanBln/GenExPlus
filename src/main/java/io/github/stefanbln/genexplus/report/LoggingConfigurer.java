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

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configures logging for the GenExPlus application.
 */
public final class LoggingConfigurer {

    private static final String LOGGER_PREFIX = "io.github.stefanbln.genexplus";

    private LoggingConfigurer() {}

    /**
     * Configures logging for the {@code io.github.stefanbln.genexplus} logger hierarchy only.
     */
    public static void configure(boolean verbose) {
        var level = verbose ? Level.FINE : Level.INFO;
        var appLogger = Logger.getLogger(LOGGER_PREFIX);
        appLogger.setLevel(level);

        if (appLogger.getHandlers().length == 0) {
            var handler = new ConsoleHandler();
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(level);
            appLogger.addHandler(handler);
            appLogger.setUseParentHandlers(false);
        } else {
            for (Handler handler : appLogger.getHandlers()) {
                handler.setLevel(level);
            }
        }
    }
}

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

package io.github.stefanbln.genexplus.report.rendering.datasources;

/**
 * Parsed JDBC fields from a Jaspersoft Studio {@code .jrdax} data adapter file.
 *
 * @param name adapter display name (usually matches {@code database.id})
 * @param driver JDBC driver class
 * @param url JDBC URL
 * @param username database user
 * @param password database password (may originate from the adapter file — override in production)
 */
public record JdbcAdapterDefinition(
        String name,
        String driver,
        String url,
        String username,
        String password
) {
    /**
     * Returns {@code true} when the adapter file declares a non-blank JDBC URL.
     */
    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }
}

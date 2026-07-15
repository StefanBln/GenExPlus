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

import io.github.stefanbln.genexplus.report.config.Configuration;

import java.util.Optional;

/**
 * Result of resolving the JDBC profile for a single report job.
 *
 * <p>Produced by {@link DatabaseProfileResolver} after applying Tier 1 (profile name) and
 * Tier 2 (JDBC settings from {@code .jrdax} + {@code application.properties}) rules.
 *
 * @param profileId effective database profile name (e.g. {@code db1}), or blank when none
 * @param idSource how the profile name was determined
 * @param jdbc merged JDBC settings used for the connection
 * @param adapterFile optional {@code .jrdax} resource that contributed defaults
 * @param credentialsFromAdapterFile when true, JDBC URL and credentials all came from the adapter file
 * @param credentialsPartiallyFromAdapter when true, URL is from properties but username/password still come from the adapter file
 * @param adapterLoadError present when a {@code .jrdax} file was found but could not be parsed
 */
public record ResolvedDatabaseProfile(
        String profileId,
        ProfileIdSource idSource,
        Configuration.DatabaseConfig jdbc,
        Optional<DataAdapterLocator.LocatedAdapter> adapterFile,
        boolean credentialsFromAdapterFile,
        boolean credentialsPartiallyFromAdapter,
        Optional<String> adapterLoadError
) {
    /**
     * Indicates how the effective profile name was chosen.
     */
    public enum ProfileIdSource {
        /** {@code database.id} set explicitly in {@code report.conf}. */
        REPORT_CONF,
        /** Inferred from {@code defaultdataadapter} in the JRXML template (Tier 1). */
        TEMPLATE_ADAPTER,
        /** No JDBC profile applies to this job. */
        NONE
    }

    /**
     * Returns {@code true} when a non-blank profile name was resolved and a JDBC connection may be needed.
     */
    public boolean requiresConnection() {
        return profileId != null && !profileId.isBlank();
    }

    /**
     * Returns {@code true} when merged JDBC settings include a non-blank URL.
     */
    public boolean hasResolvableJdbcUrl() {
        return jdbc != null && jdbc.url() != null && !jdbc.url().isBlank();
    }

    /**
     * Returns {@code true} when any credential field (username or password) still originates from a
     * {@code .jrdax} file rather than {@code application.properties}.
     */
    public boolean usesAdapterCredentials() {
        return credentialsFromAdapterFile || credentialsPartiallyFromAdapter;
    }
}

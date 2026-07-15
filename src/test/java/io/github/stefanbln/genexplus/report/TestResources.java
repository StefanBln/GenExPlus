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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Shared classpath and filesystem helpers for unit, integration, and E2E tests.
 */
public final class TestResources {

    private TestResources() {}

    public static Path testKeystorePath() throws Exception {
        URL resource = TestResources.class.getResource("/test-keystore.p12");
        assertNotNull(resource, "test-keystore.p12 must exist on the test classpath");
        return Path.of(resource.toURI());
    }

    public static Path copyKeystoreToTemp() throws IOException {
        try (InputStream input = TestResources.class.getResourceAsStream("/test-keystore.p12")) {
            assertNotNull(input, "test-keystore.p12 must exist on the test classpath");
            var destination = Files.createTempFile("genexplus-test-keystore-", ".p12");
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        }
    }

    public static Path materializeClasspathResource(String classpathResource, String tempPrefix) throws IOException {
        try (InputStream input = TestResources.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (input == null) {
                throw new IOException("missing classpath resource: " + classpathResource);
            }
            var temp = Files.createTempFile(tempPrefix, ".properties");
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }
}

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

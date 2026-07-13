package io.github.stefanbln.genexplus.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JasperRuntimeSupportTest {

    @Test
    void configuresHeadlessDefaultsWhenUnset() {
        var previousHeadless = System.clearProperty("java.awt.headless");
        var previousFonts = System.clearProperty("net.sf.jasperreports.awt.ignore.missing.font");

        try {
            JasperRuntimeSupport.configureHeadlessDefaults();

            assertEquals("true", System.getProperty("java.awt.headless"));
            assertEquals("true", System.getProperty("net.sf.jasperreports.awt.ignore.missing.font"));
        } finally {
            restoreProperty("java.awt.headless", previousHeadless);
            restoreProperty("net.sf.jasperreports.awt.ignore.missing.font", previousFonts);
        }
    }

    @Test
    void doesNotOverrideExistingProperties() {
        System.setProperty("java.awt.headless", "false");

        try {
            JasperRuntimeSupport.configureHeadlessDefaults();
            assertEquals("false", System.getProperty("java.awt.headless"));
        } finally {
            System.clearProperty("java.awt.headless");
        }
    }

    private static void restoreProperty(String key, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }
}

package io.github.stefanbln.genexplus.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentParserTest {

    private final ArgumentParser parser = new ArgumentParser();

    @Test
    void parseHelpReturnsExitZero() {
        var result = parser.parse(new String[]{"--help"});
        assertInstanceOf(ArgumentParser.Exit.class, result);
        assertEquals(0, ((ArgumentParser.Exit) result).code());
    }

    @Test
    void parseRequiresConfig() {
        var result = parser.parse(new String[]{"--verbose"});
        assertInstanceOf(ArgumentParser.Error.class, result);
        assertTrue(((ArgumentParser.Error) result).message().contains("--config is required"));
    }

    @Test
    void parseSuccessWithConfigOnly() {
        var result = parser.parse(new String[]{"--config", "report.conf"});
        assertInstanceOf(ArgumentParser.Success.class, result);
        var args = ((ArgumentParser.Success) result).arguments();
        assertEquals("report.conf", args.configFile());
        assertNull(args.propertiesFile());
        assertFalse(args.verbose());
    }

    @Test
    void parseSuccessWithAllFlags() {
        var result = parser.parse(new String[]{
                "--config", "report.conf",
                "--properties", "app.properties",
                "--verbose"
        });
        assertInstanceOf(ArgumentParser.Success.class, result);
        var args = ((ArgumentParser.Success) result).arguments();
        assertEquals("report.conf", args.configFile());
        assertEquals("app.properties", args.propertiesFile());
        assertTrue(args.verbose());
    }

    @Test
    void parseVersionReturnsExitZero() {
        var result = parser.parse(new String[]{"--version"});
        assertInstanceOf(ArgumentParser.Exit.class, result);
        assertEquals(0, ((ArgumentParser.Exit) result).code());
    }

    @Test
    void parseRejectsUnknownFlag() {
        var result = parser.parse(new String[]{"--config", "report.conf", "--unknown"});
        assertInstanceOf(ArgumentParser.Error.class, result);
    }
}

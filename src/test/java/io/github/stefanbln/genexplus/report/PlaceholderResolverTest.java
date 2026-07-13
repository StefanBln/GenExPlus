package io.github.stefanbln.genexplus.report;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderResolverTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, 7, 11, 14, 30, 0);

    @Test
    void nullInputReturnsNull() {
        assertNull(PlaceholderResolver.resolve(null, FIXED_TIME));
    }

    @Test
    void textWithoutPlaceholdersIsUnchanged() {
        assertEquals("report.pdf", PlaceholderResolver.resolve("report.pdf", FIXED_TIME));
    }

    @Test
    void validPlaceholderIsResolved() {
        var result = PlaceholderResolver.resolve("report_{[yyyyMMdd]}.pdf", FIXED_TIME);
        assertEquals("report_20250711.pdf", result);
    }

    @Test
    void invalidPlaceholderIsLeftUnchanged() {
        var input = "report_{[not-a-valid-pattern]]}.pdf";
        assertEquals(input, PlaceholderResolver.resolve(input, FIXED_TIME));
    }

    @Test
    void multiplePlaceholdersAreResolved() {
        var result = PlaceholderResolver.resolve("report_{[yyyy]}_{[MMdd]}.txt", FIXED_TIME);
        assertEquals("report_2025_0711.txt", result);
    }

    @Test
    void resolveNowUsesCurrentTime() {
        var result = PlaceholderResolver.resolveNow("prefix_{[yyyy]}");
        assertTrue(result.startsWith("prefix_"));
    }
}

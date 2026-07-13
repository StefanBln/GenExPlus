package io.github.stefanbln.genexplus.report.delivery;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für EmailMessage.
 *
 */
class EmailMessageTest {

    @Test
    void testBuilderWithValidData() {
        var message = EmailMessage.builder()
                .to("user1@example.com", "user2@example.com")
                .cc("manager@example.com")
                .bcc("archive@example.com")
                .subject("Test Report")
                .body("Please find the report attached")
                .build();

        assertEquals(2, message.getTo().size());
        assertTrue(message.getTo().contains("user1@example.com"));
        assertTrue(message.getTo().contains("user2@example.com"));
        assertEquals(1, message.getCc().size());
        assertEquals("manager@example.com", message.getCc().get(0));
        assertEquals(1, message.getBcc().size());
        assertEquals("Test Report", message.getSubject());
        assertEquals("Please find the report attached", message.getBody());
        assertFalse(message.hasAttachment());
    }

    @Test
    void testBuilderWithAttachment() {
        var attachmentPath = Path.of("/tmp/report.pdf");

        var message = EmailMessage.builder()
                .to("user@example.com")
                .subject("Test")
                .body("Body")
                .attachment(attachmentPath)
                .build();

        assertTrue(message.hasAttachment());
        assertEquals(attachmentPath, message.getAttachment());
        assertEquals("report.pdf", message.getAttachmentName());
    }

    @Test
    void testBuilderWithCustomAttachmentName() {
        var attachmentPath = Path.of("/tmp/report.pdf");

        var message = EmailMessage.builder()
                .to("user@example.com")
                .subject("Test")
                .body("Body")
                .attachment(attachmentPath, "custom-name.pdf")
                .build();

        assertTrue(message.hasAttachment());
        assertEquals("custom-name.pdf", message.getAttachmentName());
    }

    @Test
    void testBuilderWithNoRecipients() {
        assertThrows(IllegalArgumentException.class, () ->
                EmailMessage.builder()
                        .subject("Test")
                        .body("Body")
                        .build()
        );
    }

    @Test
    void testBuilderWithInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                EmailMessage.builder()
                        .to("invalid-email")
                        .subject("Test")
                        .body("Body")
                        .build()
        );
    }

    @Test
    void testBuilderWithNullSubject() {
        assertThrows(NullPointerException.class, () ->
                EmailMessage.builder()
                        .to("user@example.com")
                        .subject(null)
                        .body("Body")
                        .build()
        );
    }

    @Test
    void testBuilderWithNullBody() {
        var message = EmailMessage.builder()
                .to("user@example.com")
                .subject("Test")
                .body(null)
                .build();

        assertEquals("", message.getBody());
        assertEquals("", message.getBodyPlain());
    }

    @Test
    void testBuilderWithListRecipients() {
        var toList = List.of("user1@example.com", "user2@example.com");
        var ccList = List.of("cc@example.com");

        var message = EmailMessage.builder()
                .to(toList)
                .cc(ccList)
                .subject("Test")
                .body("Body")
                .build();

        assertEquals(2, message.getTo().size());
        assertEquals(1, message.getCc().size());
    }

    @Test
    void testResolvePlaceholders() {
        // Test mit gültigem Platzhalter
        var text = "Report {[yyyy-MM-dd]}";
        var resolved = EmailMessage.resolvePlaceholders(text);

        var now = LocalDateTime.now();
        var expected = "Report " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(expected, resolved);
    }

    @Test
    void testResolvePlaceholdersMultiple() {
        var text = "Report {[yyyy-MM]} - Generated at {[HH:mm]}";
        var resolved = EmailMessage.resolvePlaceholders(text);

        assertFalse(resolved.contains("{["));
        assertFalse(resolved.contains("]}"));
    }

    @Test
    void testResolvePlaceholdersInvalidPattern() {
        var text = "Report {[invalid-pattern]}";
        var resolved = EmailMessage.resolvePlaceholders(text);

        // Ungültiges Muster bleibt unverändert
        assertEquals(text, resolved);
    }

    @Test
    void testResolvePlaceholdersNoPlaceholders() {
        var text = "Simple text without placeholders";
        var resolved = EmailMessage.resolvePlaceholders(text);

        assertEquals(text, resolved);
    }

    @Test
    void testResolvePlaceholdersNull() {
        assertNull(EmailMessage.resolvePlaceholders(null));
    }

    @Test
    void testEmailValidation() {
        // Gültige E-Mail-Adressen
        assertDoesNotThrow(() ->
                EmailMessage.builder()
                        .to("user@example.com")
                        .subject("Test")
                        .body("Body")
                        .build()
        );

        assertDoesNotThrow(() ->
                EmailMessage.builder()
                        .to("user.name+tag@example.co.uk")
                        .subject("Test")
                        .body("Body")
                        .build()
        );

        // Ungültige E-Mail-Adressen
        assertThrows(IllegalArgumentException.class, () ->
                EmailMessage.builder()
                        .to("invalid")
                        .subject("Test")
                        .body("Body")
                        .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
                EmailMessage.builder()
                        .to("@example.com")
                        .subject("Test")
                        .body("Body")
                        .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
                EmailMessage.builder()
                        .to("user@")
                        .subject("Test")
                        .body("Body")
                        .build()
        );
    }

    @Test
    void testImmutableRecipientLists() {
        var message = EmailMessage.builder()
                .to("user@example.com")
                .cc("cc@example.com")
                .bcc("bcc@example.com")
                .subject("Test")
                .body("Body")
                .build();

        // Listen sollten unveränderlich sein
        assertThrows(UnsupportedOperationException.class, () ->
                message.getTo().add("another@example.com")
        );

        assertThrows(UnsupportedOperationException.class, () ->
                message.getCc().clear()
        );

        assertThrows(UnsupportedOperationException.class, () ->
                message.getBcc().add("another@example.com")
        );
    }

    @Test
    void testEscapeHtml() {
        assertEquals("Tom &amp; Jerry", EmailMessage.escapeHtml("Tom & Jerry"));
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", EmailMessage.escapeHtml("<script>alert(1)</script>"));
        assertNull(EmailMessage.escapeHtml(null));
    }

    @Test
    void testBuilderWithEscapedBody() {
        var message = EmailMessage.builder()
                .to("user@example.com")
                .subject("Test")
                .bodyEscaped("<b>Safe</b> &amp; plain")
                .build();

        assertEquals("&lt;b&gt;Safe&lt;/b&gt; &amp;amp; plain", message.getBody());
    }

    @Test
    void testToString() {
        var message = EmailMessage.builder()
                .to("user@example.com")
                .cc("cc@example.com")
                .subject("Test Report")
                .body("Body text")
                .build();

        var str = message.toString();
        assertTrue(str.contains("user@example.com"));
        assertTrue(str.contains("cc@example.com"));
        assertTrue(str.contains("Test Report"));
    }
}

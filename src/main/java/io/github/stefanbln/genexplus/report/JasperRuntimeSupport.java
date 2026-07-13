package io.github.stefanbln.genexplus.report;

/**
 * Applies JasperReports runtime defaults safe for headless CLI use on JDK 21+.
 *
 * <p>JasperReports initializes Java2D and font subsystems during compile/fill/export. On server
 * JDKs and in restricted environments (containers, sandboxes), missing display or font files can
 * cause native aborts unless headless mode and font fallbacks are configured <em>before</em> the
 * first render.
 *
 * <p>{@link io.github.stefanbln.genexplus.report.Main#main(String[])} invokes this method at startup. Embedders
 * calling {@link io.github.stefanbln.genexplus.report.Renderer} directly should do the same.
 *
 * <p>Existing system properties are never overwritten — operators can set values via
 * {@code -D...} before launch.
 *
 * @see io.github.stefanbln.genexplus.report.Main
 * @see io.github.stefanbln.genexplus.report.Renderer
 */
public final class JasperRuntimeSupport {

    private JasperRuntimeSupport() {
    }

    /**
     * Configures headless AWT and JasperReports font fallbacks when not already set.
     *
     * <p>Sets {@code java.awt.headless=true},
     * {@code net.sf.jasperreports.awt.ignore.missing.font=true}, and default PDF/font names.
     */
    public static void configureHeadlessDefaults() {
        setPropertyIfAbsent("java.awt.headless", "true");
        setPropertyIfAbsent("net.sf.jasperreports.awt.ignore.missing.font", "true");
        setPropertyIfAbsent("net.sf.jasperreports.default.font.name", "SansSerif");
        setPropertyIfAbsent("net.sf.jasperreports.default.pdf.font.name", "Helvetica");
    }

    private static void setPropertyIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}

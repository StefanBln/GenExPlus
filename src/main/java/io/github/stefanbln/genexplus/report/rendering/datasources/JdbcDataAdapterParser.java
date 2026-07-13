package io.github.stefanbln.genexplus.report.rendering.datasources;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads JDBC settings from JasperReports Studio {@code .jrdax} adapter files.
 *
 * <p>Only {@code jdbcDataAdapter} documents with implementation class
 * {@code net.sf.jasperreports.data.jdbc.JdbcDataAdapterImpl} are supported. JSON, Excel, HTTP,
 * and other adapter types must be replaced with explicit {@code dbN.*} properties.
 *
 * <p>XML parsing is hardened against XXE (external entities and DTDs are disabled).
 */
public final class JdbcDataAdapterParser {

    private static final String JDBC_ADAPTER_IMPL = "net.sf.jasperreports.data.jdbc.JdbcDataAdapterImpl";

    private JdbcDataAdapterParser() {}

    /**
     * Parses a JDBC adapter file from disk.
     *
     * @param file path to a {@code .jrdax} file
     * @return parsed adapter definition
     * @throws IOException when the file cannot be read or parsed
     */
    public static JdbcAdapterDefinition parse(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return parse(in);
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse JDBC data adapter: " + file, e);
        }
    }

    /**
     * Parses a JDBC adapter from an input stream (classpath or in-memory).
     *
     * @param inputStream XML content; closed by this method
     * @return parsed adapter definition
     * @throws IOException when parsing fails or the document is not a JDBC adapter
     */
    public static JdbcAdapterDefinition parse(InputStream inputStream) throws IOException {
        try {
            return parseDocument(inputStream);
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse JDBC data adapter", e);
        }
    }

    private static JdbcAdapterDefinition parseDocument(InputStream inputStream) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(inputStream);
            Element root = document.getDocumentElement();
            if (root == null) {
                throw new IllegalArgumentException("Empty data adapter document");
            }

            var rootName = root.getNodeName();
            if (!"jdbcDataAdapter".equals(rootName)) {
                throw new IllegalArgumentException("Unsupported data adapter type: " + rootName
                        + " (only JDBC adapters are supported)");
            }

            var implClass = root.getAttribute("class");
            if (!implClass.isBlank() && !JDBC_ADAPTER_IMPL.equals(implClass)) {
                throw new IllegalArgumentException("Unsupported JDBC adapter class: " + implClass);
            }

            return new JdbcAdapterDefinition(
                    textOf(root, "name"),
                    textOf(root, "driver"),
                    textOf(root, "url"),
                    textOf(root, "username"),
                    textOf(root, "password"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String textOf(Element parent, String tag) {
        var nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        var content = nodes.item(0).getTextContent();
        return content != null ? content.trim() : null;
    }
}

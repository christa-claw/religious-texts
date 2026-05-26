package org.religioustext.ingestion.store.xmldb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Stores and retrieves XML documents in BaseX via its REST API.
 *
 * Uses curl for PUT and XQuery POST to avoid Spring RestTemplate encoding issues.
 *
 * Strategy: one document per translation, books inserted incrementally.
 *   1. createRootDocument() — PUT empty root with metadata attributes
 *   2. insertBook()         — XQuery update inserting one book at a time
 */
@Component
public class BaseXStore {

    private static final Logger log = LoggerFactory.getLogger(BaseXStore.class);
    private static final String NS = "http://religioustext.org/schema/1.0";

    @Value("${basex.uri}")
    private String uri;

    @Value("${basex.username}")
    private String username;

    @Value("${basex.password}")
    private String password;

    @Value("${basex.database}")
    private String database;

    private final RestTemplate restTemplate;

    public BaseXStore(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Creates an empty root document with translation metadata.
     */
    public void createRootDocument(
             final String documentId
            , final String type
            , final String translation
            , final String abbreviation
            , final String bcp47Language
            , final String iso639_3
            , final String direction
            , final String license
            , final Integer year
            , final String region) {

        log.info("Creating root document: {}", documentId);

        final StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<text xmlns=\"").append(NS).append("\"");
        xml.append(" id=\"").append(documentId).append("\"");
        xml.append(" type=\"").append(type).append("\"");
        xml.append(" translation=\"").append(escapeXml(translation)).append("\"");
        xml.append(" abbreviation=\"").append(escapeXml(abbreviation)).append("\"");
        xml.append(" bcp47Language=\"").append(bcp47Language).append("\"");
        xml.append(" iso639_3=\"").append(iso639_3).append("\"");
        xml.append(" direction=\"").append(direction).append("\"");
        xml.append(" license=\"").append(escapeXml(license != null ? license : "Public Domain")).append("\"");
        if (year != null) xml.append(" year=\"").append(year).append("\"");
        if (region != null) xml.append(" region=\"").append(escapeXml(region)).append("\"");
        xml.append("/>");

        put(documentId, xml.toString());
        log.info("Root document created: {}.xml", documentId);
    }

    /**
     * Inserts a serialised book XML fragment into the root document.
     */
    public void insertBook(final String documentId, final String bookXml) {
        final String xquery =
            "declare namespace rt='" + NS + "'; " +
            "insert node " + bookXml + " " +
            "into db:open('" + database + "','" + documentId + "')/rt:text";
        postXQuery(xquery);
    }

    /**
     * Updates the totalVerses attribute on the root document.
     */
    public void setTotalVerses(final String documentId, final int totalVerses) {
        final String xquery =
            "declare namespace rt='" + NS + "'; " +
            "insert node attribute totalVerses {'" + totalVerses + "'} " +
            "into db:open('" + database + "','" + documentId + "')/rt:text";
        try {
            postXQuery(xquery);
        } catch (final Exception e) {
            log.warn("Could not set totalVerses: {}", e.getMessage());
        }
    }

    /**
     * Checks whether a document already exists in BaseX.
     */
    public boolean exists(final String documentId) {
        try {
            final String url = uri + "/" + database + "/" + documentId + ".xml";
            restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(buildAuthHeaders()), String.class);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Deletes a document from BaseX.
     */
    public void delete(final String documentId) {
        log.info("Deleting document: {}", documentId);
        final String url = uri + "/" + database + "/" + documentId + ".xml";
        restTemplate.exchange(url, HttpMethod.DELETE,
            new HttpEntity<>(buildAuthHeaders()), String.class);
    }

    // ── Internal helpers ──────────────────────────────────────────────

    /**
     * PUT via curl — avoids Spring RestTemplate encoding issues with BaseX.
     */
    private void put(final String path, final String xmlContent) {
        try {
            final String urlStr = uri + "/" + database + "/" + path;
            final java.io.File tmp = java.io.File.createTempFile("basex-put-", ".xml");
            try {
                Files.writeString(tmp.toPath(), xmlContent, StandardCharsets.UTF_8);
                final ProcessBuilder pb = new ProcessBuilder(
                     "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}"
                    , "-u", username + ":" + password
                    , "-X", "PUT"
                    , "-H", "Content-Type: application/xml; charset=utf-8"
                    , "--data-binary", "@" + tmp.getAbsolutePath()
                    , urlStr);
                pb.redirectErrorStream(true);
                final Process proc = pb.start();
                final String code = new String(proc.getInputStream().readAllBytes()).trim();
                proc.waitFor();
                if (!code.startsWith("2")) {
                    throw new RuntimeException("PUT failed with HTTP " + code);
                }
                log.info("PUT {} -> HTTP {}", path, code);
            } finally {
                tmp.delete();
            }
        } catch (final Exception e) {
            throw new RuntimeException("PUT failed: " + e.getMessage(), e);
        }
    }

    /**
     * XQuery POST via curl — avoids 414 URI Too Long from URL params.
     */
    private void postXQuery(final String xquery) {
        try {
            final String wrapped =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<query xmlns=\"http://basex.org/rest\">" +
                "<text>" + escapeXml(xquery) + "</text>" +
                "</query>";
            final java.io.File tmp = java.io.File.createTempFile("basex-xq-", ".xml");
            try {
                Files.writeString(tmp.toPath(), wrapped, StandardCharsets.UTF_8);
                final String url = uri + "/" + database;
                final ProcessBuilder pb = new ProcessBuilder(
                     "curl", "-s", "-o", "/tmp/basex-xq-response.txt", "-w", "%{http_code}"
                    , "-u", username + ":" + password
                    , "-X", "POST"
                    , "-H", "Content-Type: application/xml"
                    , "--data-binary", "@" + tmp.getAbsolutePath()
                    , url);
                pb.redirectErrorStream(true);
                final Process proc = pb.start();
                final String code = new String(proc.getInputStream().readAllBytes()).trim();
                proc.waitFor();
                if (!code.startsWith("2")) {
                    final String response = Files.readString(Path.of("/tmp/basex-xq-response.txt"));
                    throw new RuntimeException("XQuery POST failed HTTP " + code + ": " + response);
                }
            } finally {
                tmp.delete();
            }
        } catch (final Exception e) {
            throw new RuntimeException("XQuery failed: " + e.getMessage(), e);
        }
    }

    private HttpHeaders buildAuthHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        final String encoded = Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }

    private String escapeXml(final String value) {
        if (value == null) return "";
        return value
            .replace("&",  "&amp;")
            .replace("\"", "&quot;")
            .replace("<",  "&lt;")
            .replace(">",  "&gt;");
    }
}

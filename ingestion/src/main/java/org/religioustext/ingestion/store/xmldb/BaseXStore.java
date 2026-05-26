package org.religioustext.ingestion.store.xmldb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Stores XML documents in BaseX via its REST API.
 *
 * Strategy: one document per translation, books inserted one at a time.
 *   1. createRootDocument() — PUT empty root with metadata attributes
 *   2. insertBook()         — POST XQuery update inserting one book at a time
 *
 * This avoids sending one giant XML document and allows resuming
 * if ingestion fails partway through.
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
     * Called once per ingestion before any books are inserted.
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
        if (year != null) {
            xml.append(" year=\"").append(year).append("\"");
        }
        if (region != null) {
            xml.append(" region=\"").append(escapeXml(region)).append("\"");
        }
        xml.append("/>");

        put(documentId + ".xml", xml.toString());
        log.info("Root document created: {}.xml", documentId);
    }

    /**
     * Inserts a serialised book XML fragment into the root document.
     * The bookXml must be a complete <book> element string.
     */
    public void insertBook(final String documentId, final String bookXml) {
        final String xquery =
            "declare namespace rt='" + NS + "'; " +
            "insert node " + bookXml + " " +
            "into doc('" + database + "/" + documentId + ".xml')/rt:text";

        postXQuery(xquery);
    }

    /**
     * Updates the totalVerses attribute on the root document.
     */
    public void setTotalVerses(final String documentId, final int totalVerses) {
        final String xquery =
            "declare namespace rt='" + NS + "'; " +
            "replace value of node " +
            "doc('" + database + "/" + documentId + ".xml')/rt:text/@totalVerses " +
            "with '" + totalVerses + "'";

        // First insert the attribute if it doesn't exist
        final String xqueryInsert =
            "declare namespace rt='" + NS + "'; " +
            "insert node attribute totalVerses {'" + totalVerses + "'} " +
            "into doc('" + database + "/" + documentId + ".xml')/rt:text";

        try {
            postXQuery(xqueryInsert);
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
            restTemplate.exchange(
                 url
                , HttpMethod.GET
                , new HttpEntity<>(buildAuthHeaders())
                , String.class);
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
        restTemplate.exchange(
             url
            , HttpMethod.DELETE
            , new HttpEntity<>(buildAuthHeaders())
            , String.class);
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private void put(final String path, final String xmlContent) {
        final String url = uri + "/" + database + "/" + path;
        final HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        restTemplate.exchange(
             url
            , HttpMethod.PUT
            , new HttpEntity<>(xmlContent.getBytes(StandardCharsets.UTF_8), headers)
            , String.class);
    }

    private void postXQuery(final String xquery) {
        final String url = uri + "/" + database;
        final HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("X-Method", "POST");
        restTemplate.exchange(
             url + "?query=" + java.net.URLEncoder.encode(xquery, StandardCharsets.UTF_8)
            , HttpMethod.GET
            , new HttpEntity<>(headers)
            , String.class);
    }

    private HttpHeaders buildAuthHeaders() {
        final HttpHeaders headers  = new HttpHeaders();
        final String      creds    = username + ":" + password;
        final String      encoded  = Base64.getEncoder()
            .encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }

    private String escapeXml(final String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}

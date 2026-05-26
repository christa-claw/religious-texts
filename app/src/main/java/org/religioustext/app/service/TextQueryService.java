package org.religioustext.app.service;

import org.religioustext.app.config.BaseXConfig.BaseXProperties;
import org.religioustext.app.model.DisplayOptions;
import org.religioustext.app.model.VerseRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Queries religious texts from BaseX via its REST API using XQuery.
 * All display logic (ordering, CAPS) is applied here
 * before returning results to the Vaadin view layer.
 */
@Service
public class TextQueryService {

    private static final Logger log = LoggerFactory.getLogger(TextQueryService.class);

    private static final String NS_DECL =
        "declare namespace rt='http://religioustext.org/schema/1.0'; ";

    private final BaseXProperties baseXProperties;
    private final RestTemplate    restTemplate;

    public TextQueryService(
             final BaseXProperties baseXProperties
            , final RestTemplate    restTemplate) {
        this.baseXProperties = baseXProperties;
        this.restTemplate    = restTemplate;
    }

    // ── Source listing ────────────────────────────────────────────────

    public List<String[]> listSources() {
        final String xquery = NS_DECL
            + "for $t in db:open('" + baseXProperties.database() + "')//rt:text "
            + "return string-join(($t/@id,$t/@translation,$t/@abbreviation,$t/@direction),'|')";

        return executeQuery(xquery).stream()
            .map(row -> row.split("\\|"))
            .toList();
    }

    // ── Drill-down mode ───────────────────────────────────────────────

    public List<String> listBooks(final String sourceId) {
        final String xquery = NS_DECL
            + "for $b in db:open('" + baseXProperties.database() + "')"
            + "//rt:text[@id='" + sourceId + "']//rt:book "
            + "order by xs:integer($b/@canonicalOrder) "
            + "return string($b/@name)";

        return executeQuery(xquery);
    }

    public List<VerseRef> getVerses(
             final String         sourceId
            , final String         bookName
            , final int            chapterNumber
            , final DisplayOptions options) {

        final String xquery = NS_DECL
            + "for $v in db:open('" + baseXProperties.database() + "')"
            + "//rt:text[@id='" + sourceId + "']"
            + "//rt:book[@name='" + bookName + "']"
            + "//rt:chapter[@number='" + chapterNumber + "']"
            + "//rt:verse "
            + "order by xs:integer($v/@number) "
            + "return string-join(("
            + "string($v/@number),string($v/@bookName),string($v/@bookAltName),"
            + "string($v/@chapterNumber),string($v/@chapterTitle),"
            + "string($v/@globalCanonicalSeq),string($v/@globalChronologicalSeq),"
            + "string($v/@globalNarrativeSeq),string($v/@note),string($v)"
            + "),'|||')";

        return executeQuery(xquery).stream()
            .map(row -> parseVerseRow(row, sourceId))
            .toList();
    }

    // ── Streaming mode ────────────────────────────────────────────────

    public List<VerseRef> streamVerses(
             final String         sourceId
            , final DisplayOptions options) {

        final String orderAttr = switch (options.getOrderMode()) {
            case CHRONOLOGICAL -> "@globalChronologicalSeq";
            case NARRATIVE     -> "@globalNarrativeSeq";
            default            -> "@globalCanonicalSeq";
        };

        final String xquery = NS_DECL
            + "for $v in db:open('" + baseXProperties.database() + "')"
            + "//rt:text[@id='" + sourceId + "']//rt:verse "
            + "let $seq := if (exists($v/" + orderAttr + ")) "
            + "            then xs:integer($v/" + orderAttr + ") "
            + "            else xs:integer($v/@globalCanonicalSeq) "
            + "order by $seq "
            + "return string-join(("
            + "string($v/@number),string($v/@bookName),string($v/@bookAltName),"
            + "string($v/@chapterNumber),string($v/@chapterTitle),"
            + "string($v/@globalCanonicalSeq),string($v/@globalChronologicalSeq),"
            + "string($v/@globalNarrativeSeq),string($v/@note),string($v)"
            + "),'|||')";

        return executeQuery(xquery).stream()
            .map(row -> parseVerseRow(row, sourceId))
            .toList();
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private List<String> executeQuery(final String xquery) {
        final List<String> results = new ArrayList<>();
        try {
            final String url = baseXProperties.uri()
                + "/" + baseXProperties.database()
                + "?query=" + java.net.URLEncoder.encode(xquery, StandardCharsets.UTF_8);

            final HttpHeaders headers = new HttpHeaders();
            final String credentials = baseXProperties.username()
                + ":" + baseXProperties.password();
            final String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encoded);
            headers.set("Accept", "text/plain");

            final ResponseEntity<String> response = restTemplate.exchange(
                 url
                , HttpMethod.GET
                , new HttpEntity<>(headers)
                , String.class);

            if (response.getBody() != null && !response.getBody().isBlank()) {
                for (final String line : response.getBody().split("\n")) {
                    if (!line.isBlank()) results.add(line.trim());
                }
            }
        } catch (final Exception e) {
            log.error("XQuery execution failed: {}", e.getMessage(), e);
        }
        return results;
    }

    private VerseRef parseVerseRow(final String row, final String sourceId) {
        final String[] parts = row.split("\\|\\|\\|", -1);
        return VerseRef.builder()
            .sourceId(sourceId)
            .verseNumber(parseIntSafe(parts, 0))
            .bookName(partAt(parts, 1))
            .bookAltName(partAt(parts, 2))
            .chapterNumber(parseIntSafe(parts, 3))
            .chapterTitle(partAt(parts, 4))
            .globalCanonicalSeq(parseIntegerSafe(parts, 5))
            .globalChronologicalSeq(parseIntegerSafe(parts, 6))
            .globalNarrativeSeq(parseIntegerSafe(parts, 7))
            .note(partAt(parts, 8))
            .content(partAt(parts, 9))
            .build();
    }

    private String partAt(final String[] parts, final int index) {
        return (index < parts.length && !parts[index].isBlank()) ? parts[index] : null;
    }

    private int parseIntSafe(final String[] parts, final int index) {
        try { return Integer.parseInt(parts[index].trim()); }
        catch (final Exception e) { return 0; }
    }

    private Integer parseIntegerSafe(final String[] parts, final int index) {
        try {
            final String val = parts[index].trim();
            return val.isEmpty() ? null : Integer.parseInt(val);
        } catch (final Exception e) { return null; }
    }
}

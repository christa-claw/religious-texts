package org.religioustext.ingestion.crawler;

import org.religioustext.ingestion.model.IngestionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Crawls individual Bible verses from jsDelivr CDN.
 * URL pattern:
 *   https://cdn.jsdelivr.net/gh/wldeh/bible-api/bibles/en-kjv/books/{book}/chapters/{chapter}/verses/{verse}.json
 * Returns: {"verse":"1","text":"In the beginning..."}
 *
 * Three separate canonical book lists are maintained:
 *   PROTESTANT_BOOKS : 66 books
 *   CATHOLIC_BOOKS   : 73 books — DC books interspersed in correct OT positions
 *   ORTHODOX_BOOKS   : 78 books — additional Orthodox books
 *
 * Each list has its own canonicalOrder numbering reflecting the actual
 * position within that canon, so there are no numbering conflicts.
 */
@Component
public class BibleApiCrawler {

    private static final Logger log = LoggerFactory.getLogger(BibleApiCrawler.class);

    private static final String CHAPTER_URL =
        "https://cdn.jsdelivr.net/gh/wldeh/bible-api/bibles/{translation}"
        + "/books/{book}/chapters/{chapter}.json";

    // ── Protestant canon: 66 books ────────────────────────────────────
    private static final List<BookDef> PROTESTANT_BOOKS = List.of(
        // Pentateuch
         new BookDef("genesis", "Genesis", "OT", 1, 50, "en-kjv")
        , new BookDef("exodus", "Exodus", "OT", 2, 40, "en-kjv")
        , new BookDef("leviticus", "Leviticus", "OT", 3, 27, "en-kjv")
        , new BookDef("numbers", "Numbers", "OT", 4, 36, "en-kjv")
        , new BookDef("deuteronomy", "Deuteronomy", "OT", 5, 34, "en-kjv")
        // Historical
        , new BookDef("joshua", "Joshua", "OT", 6, 24, "en-kjv")
        , new BookDef("judges", "Judges", "OT", 7, 21, "en-kjv")
        , new BookDef("ruth", "Ruth", "OT", 8, 4, "en-kjv")
        , new BookDef("1-samuel", "1 Samuel", "OT", 9, 31, "en-kjv")
        , new BookDef("2-samuel", "2 Samuel", "OT", 10, 24, "en-kjv")
        , new BookDef("1-kings", "1 Kings", "OT", 11, 22, "en-kjv")
        , new BookDef("2-kings", "2 Kings", "OT", 12, 25, "en-kjv")
        , new BookDef("1-chronicles", "1 Chronicles", "OT", 13, 29, "en-kjv")
        , new BookDef("2-chronicles", "2 Chronicles", "OT", 14, 36, "en-kjv")
        , new BookDef("ezra", "Ezra", "OT", 15, 10, "en-kjv")
        , new BookDef("nehemiah", "Nehemiah", "OT", 16, 13, "en-kjv")
        , new BookDef("esther", "Esther", "OT", 17, 10, "en-kjv")
        // Poetic
        , new BookDef("job", "Job", "OT", 18, 42, "en-kjv")
        , new BookDef("psalms", "Psalms", "OT", 19, 150, "en-kjv")
        , new BookDef("proverbs", "Proverbs", "OT", 20, 31, "en-kjv")
        , new BookDef("ecclesiastes", "Ecclesiastes", "OT", 21, 12, "en-kjv")
        , new BookDef("song-of-songs", "Song of Songs", "OT", 22, 8, "en-kjv")
        // Prophetic
        , new BookDef("isaiah", "Isaiah", "OT", 23, 66, "en-kjv")
        , new BookDef("jeremiah", "Jeremiah", "OT", 24, 52, "en-kjv")
        , new BookDef("lamentations", "Lamentations", "OT", 25, 5, "en-kjv")
        , new BookDef("ezekiel", "Ezekiel", "OT", 26, 48, "en-kjv")
        , new BookDef("daniel", "Daniel", "OT", 27, 12, "en-kjv")
        , new BookDef("hosea", "Hosea", "OT", 28, 14, "en-kjv")
        , new BookDef("joel", "Joel", "OT", 29, 3, "en-kjv")
        , new BookDef("amos", "Amos", "OT", 30, 9, "en-kjv")
        , new BookDef("obadiah", "Obadiah", "OT", 31, 1, "en-kjv")
        , new BookDef("jonah", "Jonah", "OT", 32, 4, "en-kjv")
        , new BookDef("micah", "Micah", "OT", 33, 7, "en-kjv")
        , new BookDef("nahum", "Nahum", "OT", 34, 3, "en-kjv")
        , new BookDef("habakkuk", "Habakkuk", "OT", 35, 3, "en-kjv")
        , new BookDef("zephaniah", "Zephaniah", "OT", 36, 3, "en-kjv")
        , new BookDef("haggai", "Haggai", "OT", 37, 2, "en-kjv")
        , new BookDef("zechariah", "Zechariah", "OT", 38, 14, "en-kjv")
        , new BookDef("malachi", "Malachi", "OT", 39, 4, "en-kjv")
        // New Testament
        , new BookDef("matthew", "Matthew", "NT", 40, 28, "en-kjv")
        , new BookDef("mark", "Mark", "NT", 41, 16, "en-kjv")
        , new BookDef("luke", "Luke", "NT", 42, 24, "en-kjv")
        , new BookDef("john", "John", "NT", 43, 21, "en-kjv")
        , new BookDef("acts", "Acts", "NT", 44, 28, "en-kjv")
        , new BookDef("romans", "Romans", "NT", 45, 16, "en-kjv")
        , new BookDef("1-corinthians", "1 Corinthians", "NT", 46, 16, "en-kjv")
        , new BookDef("2-corinthians", "2 Corinthians", "NT", 47, 13, "en-kjv")
        , new BookDef("galatians", "Galatians", "NT", 48, 6, "en-kjv")
        , new BookDef("ephesians", "Ephesians", "NT", 49, 6, "en-kjv")
        , new BookDef("philippians", "Philippians", "NT", 50, 4, "en-kjv")
        , new BookDef("colossians", "Colossians", "NT", 51, 4, "en-kjv")
        , new BookDef("1-thessalonians", "1 Thessalonians", "NT", 52, 5, "en-kjv")
        , new BookDef("2-thessalonians", "2 Thessalonians", "NT", 53, 3, "en-kjv")
        , new BookDef("1-timothy", "1 Timothy", "NT", 54, 6, "en-kjv")
        , new BookDef("2-timothy", "2 Timothy", "NT", 55, 4, "en-kjv")
        , new BookDef("titus", "Titus", "NT", 56, 3, "en-kjv")
        , new BookDef("philemon", "Philemon", "NT", 57, 1, "en-kjv")
        , new BookDef("hebrews", "Hebrews", "NT", 58, 13, "en-kjv")
        , new BookDef("james", "James", "NT", 59, 5, "en-kjv")
        , new BookDef("1-peter", "1 Peter", "NT", 60, 5, "en-kjv")
        , new BookDef("2-peter", "2 Peter", "NT", 61, 3, "en-kjv")
        , new BookDef("1-john", "1 John", "NT", 62, 5, "en-kjv")
        , new BookDef("2-john", "2 John", "NT", 63, 1, "en-kjv")
        , new BookDef("3-john", "3 John", "NT", 64, 1, "en-kjv")
        , new BookDef("jude", "Jude", "NT", 65, 1, "en-kjv")
        , new BookDef("revelation", "Revelation", "NT", 66, 22, "en-kjv")
    );

    // ── Catholic canon: 73 books ──────────────────────────────────────
    // DC books interspersed in their correct canonical positions:
    //   Historical : ...Nehemiah(16), Tobit(17), Judith(18), Esther(19),
    //                   1Macc(20), 2Macc(21)
    //   Poetic     : ...Song of Songs(27), Wisdom(28), Sirach(29)
    //   Prophetic  : ...Lamentations(32), Baruch(33), Ezekiel(34)...
    private static final List<BookDef> CATHOLIC_BOOKS = List.of(
        // Pentateuch (1-5)
         new BookDef("genesis", "Genesis", "OT", 1, 50, "en-kjv")
        , new BookDef("exodus", "Exodus", "OT", 2, 40, "en-kjv")
        , new BookDef("leviticus", "Leviticus", "OT", 3, 27, "en-kjv")
        , new BookDef("numbers", "Numbers", "OT", 4, 36, "en-kjv")
        , new BookDef("deuteronomy", "Deuteronomy", "OT", 5, 34, "en-kjv")
        // Historical (6-21, includes Tobit, Judith, 1-2 Maccabees)
        , new BookDef("joshua", "Joshua", "OT", 6, 24, "en-kjv")
        , new BookDef("judges", "Judges", "OT", 7, 21, "en-kjv")
        , new BookDef("ruth", "Ruth", "OT", 8, 4, "en-kjv")
        , new BookDef("1-samuel", "1 Samuel", "OT", 9, 31, "en-kjv")
        , new BookDef("2-samuel", "2 Samuel", "OT", 10, 24, "en-kjv")
        , new BookDef("1-kings", "1 Kings", "OT", 11, 22, "en-kjv")
        , new BookDef("2-kings", "2 Kings", "OT", 12, 25, "en-kjv")
        , new BookDef("1-chronicles", "1 Chronicles", "OT", 13, 29, "en-kjv")
        , new BookDef("2-chronicles", "2 Chronicles", "OT", 14, 36, "en-kjv")
        , new BookDef("ezra", "Ezra", "OT", 15, 10, "en-kjv")
        , new BookDef("nehemiah", "Nehemiah", "OT", 16, 13, "en-kjv")
        , new BookDef("tobit", "Tobit", "DC", 17, 14, "en-dra")
        , new BookDef("judith", "Judith", "DC", 18, 16, "en-dra")
        , new BookDef("esther", "Esther", "OT", 19, 10, "en-kjv")
        , new BookDef("1-maccabees", "1 Maccabees", "DC", 20, 16, "en-dra")
        , new BookDef("2-maccabees", "2 Maccabees", "DC", 21, 15, "en-dra")
        // Poetic (22-29, includes Wisdom and Sirach)
        , new BookDef("job", "Job", "OT", 22, 42, "en-kjv")
        , new BookDef("psalms", "Psalms", "OT", 23, 150, "en-kjv")
        , new BookDef("proverbs", "Proverbs", "OT", 24, 31, "en-kjv")
        , new BookDef("ecclesiastes", "Ecclesiastes", "OT", 25, 12, "en-kjv")
        , new BookDef("song-of-songs", "Song of Songs", "OT", 26, 8, "en-kjv")
        , new BookDef("wisdom", "Wisdom", "DC", 27, 19, "en-dra")
        , new BookDef("sirach", "Sirach", "DC", 28, 51, "en-dra")
        // Prophetic (29-46, includes Baruch after Lamentations)
        , new BookDef("isaiah", "Isaiah", "OT", 29, 66, "en-kjv")
        , new BookDef("jeremiah", "Jeremiah", "OT", 30, 52, "en-kjv")
        , new BookDef("lamentations", "Lamentations", "OT", 31, 5, "en-kjv")
        , new BookDef("baruch", "Baruch", "DC", 32, 6, "en-dra")
        , new BookDef("ezekiel", "Ezekiel", "OT", 33, 48, "en-kjv")
        , new BookDef("daniel", "Daniel", "OT", 34, 12, "en-kjv")
        , new BookDef("hosea", "Hosea", "OT", 35, 14, "en-kjv")
        , new BookDef("joel", "Joel", "OT", 36, 3, "en-kjv")
        , new BookDef("amos", "Amos", "OT", 37, 9, "en-kjv")
        , new BookDef("obadiah", "Obadiah", "OT", 38, 1, "en-kjv")
        , new BookDef("jonah", "Jonah", "OT", 39, 4, "en-kjv")
        , new BookDef("micah", "Micah", "OT", 40, 7, "en-kjv")
        , new BookDef("nahum", "Nahum", "OT", 41, 3, "en-kjv")
        , new BookDef("habakkuk", "Habakkuk", "OT", 42, 3, "en-kjv")
        , new BookDef("zephaniah", "Zephaniah", "OT", 43, 3, "en-kjv")
        , new BookDef("haggai", "Haggai", "OT", 44, 2, "en-kjv")
        , new BookDef("zechariah", "Zechariah", "OT", 45, 14, "en-kjv")
        , new BookDef("malachi", "Malachi", "OT", 46, 4, "en-kjv")
        // New Testament (47-73)
        , new BookDef("matthew", "Matthew", "NT", 47, 28, "en-kjv")
        , new BookDef("mark", "Mark", "NT", 48, 16, "en-kjv")
        , new BookDef("luke", "Luke", "NT", 49, 24, "en-kjv")
        , new BookDef("john", "John", "NT", 50, 21, "en-kjv")
        , new BookDef("acts", "Acts", "NT", 51, 28, "en-kjv")
        , new BookDef("romans", "Romans", "NT", 52, 16, "en-kjv")
        , new BookDef("1-corinthians", "1 Corinthians", "NT", 53, 16, "en-kjv")
        , new BookDef("2-corinthians", "2 Corinthians", "NT", 54, 13, "en-kjv")
        , new BookDef("galatians", "Galatians", "NT", 55, 6, "en-kjv")
        , new BookDef("ephesians", "Ephesians", "NT", 56, 6, "en-kjv")
        , new BookDef("philippians", "Philippians", "NT", 57, 4, "en-kjv")
        , new BookDef("colossians", "Colossians", "NT", 58, 4, "en-kjv")
        , new BookDef("1-thessalonians", "1 Thessalonians", "NT", 59, 5, "en-kjv")
        , new BookDef("2-thessalonians", "2 Thessalonians", "NT", 60, 3, "en-kjv")
        , new BookDef("1-timothy", "1 Timothy", "NT", 61, 6, "en-kjv")
        , new BookDef("2-timothy", "2 Timothy", "NT", 62, 4, "en-kjv")
        , new BookDef("titus", "Titus", "NT", 63, 3, "en-kjv")
        , new BookDef("philemon", "Philemon", "NT", 64, 1, "en-kjv")
        , new BookDef("hebrews", "Hebrews", "NT", 65, 13, "en-kjv")
        , new BookDef("james", "James", "NT", 66, 5, "en-kjv")
        , new BookDef("1-peter", "1 Peter", "NT", 67, 5, "en-kjv")
        , new BookDef("2-peter", "2 Peter", "NT", 68, 3, "en-kjv")
        , new BookDef("1-john", "1 John", "NT", 69, 5, "en-kjv")
        , new BookDef("2-john", "2 John", "NT", 70, 1, "en-kjv")
        , new BookDef("3-john", "3 John", "NT", 71, 1, "en-kjv")
        , new BookDef("jude", "Jude", "NT", 72, 1, "en-kjv")
        , new BookDef("revelation", "Revelation", "NT", 73, 22, "en-kjv")
    );

    // ── Orthodox canon: 78 books ──────────────────────────────────────
    // Extends Catholic with 1 Esdras, 3 Maccabees, Prayer of Manasseh,
    // Psalm 151. Positions follow Eastern Orthodox Bible ordering.
    private static final List<BookDef> ORTHODOX_BOOKS = List.of(
        // Pentateuch (1-5)
         new BookDef("genesis", "Genesis", "OT", 1, 50, "en-kjv")
        , new BookDef("exodus", "Exodus", "OT", 2, 40, "en-kjv")
        , new BookDef("leviticus", "Leviticus", "OT", 3, 27, "en-kjv")
        , new BookDef("numbers", "Numbers", "OT", 4, 36, "en-kjv")
        , new BookDef("deuteronomy", "Deuteronomy", "OT", 5, 34, "en-kjv")
        // Historical (6-25, adds 1 Esdras and 3 Maccabees)
        , new BookDef("joshua", "Joshua", "OT", 6, 24, "en-kjv")
        , new BookDef("judges", "Judges", "OT", 7, 21, "en-kjv")
        , new BookDef("ruth", "Ruth", "OT", 8, 4, "en-kjv")
        , new BookDef("1-samuel", "1 Samuel", "OT", 9, 31, "en-kjv")
        , new BookDef("2-samuel", "2 Samuel", "OT", 10, 24, "en-kjv")
        , new BookDef("1-kings", "1 Kings", "OT", 11, 22, "en-kjv")
        , new BookDef("2-kings", "2 Kings", "OT", 12, 25, "en-kjv")
        , new BookDef("1-chronicles", "1 Chronicles", "OT", 13, 29, "en-kjv")
        , new BookDef("2-chronicles", "2 Chronicles", "OT", 14, 36, "en-kjv")
        , new BookDef("1-esdras", "1 Esdras", "DC", 15, 9, "en-dra")
        , new BookDef("ezra", "Ezra", "OT", 16, 10, "en-kjv")
        , new BookDef("nehemiah", "Nehemiah", "OT", 17, 13, "en-kjv")
        , new BookDef("tobit", "Tobit", "DC", 18, 14, "en-dra")
        , new BookDef("judith", "Judith", "DC", 19, 16, "en-dra")
        , new BookDef("esther", "Esther", "OT", 20, 10, "en-kjv")
        , new BookDef("1-maccabees", "1 Maccabees", "DC", 21, 16, "en-dra")
        , new BookDef("2-maccabees", "2 Maccabees", "DC", 22, 15, "en-dra")
        , new BookDef("3-maccabees", "3 Maccabees", "DC", 23, 7, "en-dra")
        // Poetic (24-32, adds Psalm 151 after Psalms)
        , new BookDef("job", "Job", "OT", 24, 42, "en-kjv")
        , new BookDef("psalms", "Psalms", "OT", 25, 150, "en-kjv")
        , new BookDef("psalm-151", "Psalm 151", "DC", 26, 1, "en-dra")
        , new BookDef("proverbs", "Proverbs", "OT", 27, 31, "en-kjv")
        , new BookDef("ecclesiastes", "Ecclesiastes", "OT", 28, 12, "en-kjv")
        , new BookDef("song-of-songs", "Song of Songs", "OT", 29, 8, "en-kjv")
        , new BookDef("wisdom", "Wisdom", "DC", 30, 19, "en-dra")
        , new BookDef("sirach", "Sirach", "DC", 31, 51, "en-dra")
        // Prophetic (32-51, adds Baruch, Prayer of Manasseh)
        , new BookDef("isaiah", "Isaiah", "OT", 32, 66, "en-kjv")
        , new BookDef("jeremiah", "Jeremiah", "OT", 33, 52, "en-kjv")
        , new BookDef("lamentations", "Lamentations", "OT", 34, 5, "en-kjv")
        , new BookDef("baruch", "Baruch", "DC", 35, 6, "en-dra")
        , new BookDef("ezekiel", "Ezekiel", "OT", 36, 48, "en-kjv")
        , new BookDef("daniel", "Daniel", "OT", 37, 12, "en-kjv")
        , new BookDef("hosea", "Hosea", "OT", 38, 14, "en-kjv")
        , new BookDef("joel", "Joel", "OT", 39, 3, "en-kjv")
        , new BookDef("amos", "Amos", "OT", 40, 9, "en-kjv")
        , new BookDef("obadiah", "Obadiah", "OT", 41, 1, "en-kjv")
        , new BookDef("jonah", "Jonah", "OT", 42, 4, "en-kjv")
        , new BookDef("micah", "Micah", "OT", 43, 7, "en-kjv")
        , new BookDef("nahum", "Nahum", "OT", 44, 3, "en-kjv")
        , new BookDef("habakkuk", "Habakkuk", "OT", 45, 3, "en-kjv")
        , new BookDef("zephaniah", "Zephaniah", "OT", 46, 3, "en-kjv")
        , new BookDef("haggai", "Haggai", "OT", 47, 2, "en-kjv")
        , new BookDef("zechariah", "Zechariah", "OT", 48, 14, "en-kjv")
        , new BookDef("malachi", "Malachi", "OT", 49, 4, "en-kjv")
        , new BookDef("prayer-manasseh", "Prayer of Manasseh", "DC", 50, 1, "en-dra")
        // New Testament (51-78)
        , new BookDef("matthew", "Matthew", "NT", 51, 28, "en-kjv")
        , new BookDef("mark", "Mark", "NT", 52, 16, "en-kjv")
        , new BookDef("luke", "Luke", "NT", 53, 24, "en-kjv")
        , new BookDef("john", "John", "NT", 54, 21, "en-kjv")
        , new BookDef("acts", "Acts", "NT", 55, 28, "en-kjv")
        , new BookDef("romans", "Romans", "NT", 56, 16, "en-kjv")
        , new BookDef("1-corinthians", "1 Corinthians", "NT", 57, 16, "en-kjv")
        , new BookDef("2-corinthians", "2 Corinthians", "NT", 58, 13, "en-kjv")
        , new BookDef("galatians", "Galatians", "NT", 59, 6, "en-kjv")
        , new BookDef("ephesians", "Ephesians", "NT", 60, 6, "en-kjv")
        , new BookDef("philippians", "Philippians", "NT", 61, 4, "en-kjv")
        , new BookDef("colossians", "Colossians", "NT", 62, 4, "en-kjv")
        , new BookDef("1-thessalonians", "1 Thessalonians", "NT", 63, 5, "en-kjv")
        , new BookDef("2-thessalonians", "2 Thessalonians", "NT", 64, 3, "en-kjv")
        , new BookDef("1-timothy", "1 Timothy", "NT", 65, 6, "en-kjv")
        , new BookDef("2-timothy", "2 Timothy", "NT", 66, 4, "en-kjv")
        , new BookDef("titus", "Titus", "NT", 67, 3, "en-kjv")
        , new BookDef("philemon", "Philemon", "NT", 68, 1, "en-kjv")
        , new BookDef("hebrews", "Hebrews", "NT", 69, 13, "en-kjv")
        , new BookDef("james", "James", "NT", 70, 5, "en-kjv")
        , new BookDef("1-peter", "1 Peter", "NT", 71, 5, "en-kjv")
        , new BookDef("2-peter", "2 Peter", "NT", 72, 3, "en-kjv")
        , new BookDef("1-john", "1 John", "NT", 73, 5, "en-kjv")
        , new BookDef("2-john", "2 John", "NT", 74, 1, "en-kjv")
        , new BookDef("3-john", "3 John", "NT", 75, 1, "en-kjv")
        , new BookDef("jude", "Jude", "NT", 76, 1, "en-kjv")
        , new BookDef("revelation", "Revelation", "NT", 77, 22, "en-kjv")
        , new BookDef("4-maccabees", "4 Maccabees", "DC", 78, 18, "en-dra")
    );

    private final RestTemplate    restTemplate;
    private final LocalBibleReader localReader;

    public BibleApiCrawler(
             final RestTemplate    restTemplate
            , final LocalBibleReader localReader) {
        this.restTemplate = restTemplate;
        this.localReader  = localReader;
    }

    /**
     * Returns the correct book list for the given canon.
     * Region values: "Catholic", "Orthodox", or null/anything else = Protestant.
     */
    public List<BookDef> getBooksForCanon(final String region) {
        if ("Orthodox".equalsIgnoreCase(region))  return ORTHODOX_BOOKS;
        if ("Catholic".equalsIgnoreCase(region))  return CATHOLIC_BOOKS;
        return PROTESTANT_BOOKS;
    }

    /**
     * Fetches all verses for all books in the appropriate canon.
     */
    public Map<String, List<Map<String, Object>>> fetchAllChapters(
            final IngestionRequest request) {

        final List<BookDef> books = getBooksForCanon(request.getRegion());
        final Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        for (final BookDef book : books) {
            log.info("Crawling book {}/{}: {}"
                , book.canonicalOrder(), books.size(), book.commonName());

            // Use request cdnTranslation if set, otherwise use book default
            final String cdnTranslation = (request.getRegion() != null
                && request.getSourceUrl() != null
                && request.getSourceUrl().contains("bibles/"))
                ? request.getSourceUrl().replaceAll(".*/bibles/([^/]+)/.*", "$1")
                : book.cdnTranslation();

            final List<Map<String, Object>> chapters = new ArrayList<>();
            for (int ch = 1; ch <= book.chapterCount(); ch++) {
                chapters.add(fetchChapter(book.id(), ch, cdnTranslation));
            }
            result.put(book.id(), chapters);
        }

        return result;
    }

    /**
     * Fetches all chapters for a single book.
     * Used by IngestionService for book-by-book processing.
     */
    public Map<String, List<Map<String, Object>>> fetchAllChapters(
             final IngestionRequest request
            , final BookDef          book) {

        final String cdnTranslation = book.cdnTranslation();

        if (!localReader.isAvailable(cdnTranslation)) {
            throw new IllegalStateException(
                "Local bible-api clone not found for: " + cdnTranslation
                + ". Clone https://github.com/wldeh/bible-api to /Volumes/VMs/data/bible-api-source");
        }

        log.debug("Reading local: {} / {}", cdnTranslation, book.commonName());
        final java.util.List<java.util.Map<String, Object>> chapters =
            localReader.fetchChapters(cdnTranslation, book.id().replaceAll("^(\\d+)-", "$1"));
        result.put(book.id(), chapters);
        return result;
    }

    /**
     * Fetches all verses for a single chapter in one HTTP call.
     * Response: {"data":[{"book":"Genesis","chapter":"1","verse":"1","text":"..."},...]}
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchChapter(
             final String bookId
            , final int    chapterNumber
            , final String cdnTranslation) {

        try {
            final String url = CHAPTER_URL
                .replace("{translation}", cdnTranslation)
                .replace("{book}",        bookId)
                .replace("{chapter}",     String.valueOf(chapterNumber));

            final Map<String, Object> response =
                restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("data")) {
                log.warn("Empty response for {}/{}", bookId, chapterNumber);
                return Map.of("chapterNumber", chapterNumber, "verses", List.of());
            }

            final List<Map<String, Object>> rawVerses =
                (List<Map<String, Object>>) response.get("data");

            // The CDN returns each chapter twice — deduplicate by verse number
            // keeping the first occurrence of each verse number
            final List<Map<String, Object>> verses  = new ArrayList<>();
            final java.util.Set<Integer>    seen    = new java.util.LinkedHashSet<>();
            for (final Map<String, Object> raw : rawVerses) {
                final int    verseNum = Integer.parseInt(String.valueOf(raw.get("verse")));
                final String text     = String.valueOf(raw.get("text"));
                if (seen.add(verseNum)) {
                    verses.add(Map.of("number", verseNum, "text", text));
                }
            }

            log.debug("  Chapter {}: {} verses", chapterNumber, verses.size());
            Thread.sleep(50);
            return Map.of("chapterNumber", chapterNumber, "verses", verses);

        } catch (final Exception e) {
            final String msg = e.getMessage();
            if (msg != null && msg.contains("50 MB")) {
                log.warn("Chapter {}/{} exceeds CDN limit - falling back to verse-by-verse"
                    , bookId, chapterNumber);
                return fetchChapterVerseByVerse(bookId, chapterNumber, cdnTranslation);
            }
            log.error("Failed to fetch {}/{}: {}", bookId, chapterNumber, msg);
            return Map.of("chapterNumber", chapterNumber, "verses", List.of());
        }
    }

    /**
     * Fallback: fetches verses one at a time for chapters that exceed the CDN 50MB limit.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchChapterVerseByVerse(
             final String bookId
            , final int    chapterNumber
            , final String cdnTranslation) {

        final String verseUrl =
            "https://cdn.jsdelivr.net/gh/wldeh/bible-api/bibles/{translation}"
            + "/books/{book}/chapters/{chapter}/verses/{verse}.json";

        // Numbered books use no hyphen at verse level: 2-kings -> 2kings
        final String verseBookId = bookId.replaceAll("^(\\d+)-", "$1");

        final List<Map<String, Object>> verses = new ArrayList<>();
        final java.util.Set<Integer>    seen   = new java.util.LinkedHashSet<>();
        int verseNumber = 1;

        while (true) {
            try {
                final String url = verseUrl
                    .replace("{translation}", cdnTranslation)
                    .replace("{book}",        verseBookId)
                    .replace("{chapter}",     String.valueOf(chapterNumber))
                    .replace("{verse}",       String.valueOf(verseNumber));

                final Map<String, Object> verse =
                    restTemplate.getForObject(url, Map.class);

                if (verse == null || !verse.containsKey("text")) break;

                if (seen.add(verseNumber)) {
                    verses.add(Map.of(
                         "number", verseNumber
                        , "text",   verse.get("text")));
                }
                verseNumber++;
                Thread.sleep(50);

            } catch (final Exception e) {
                break;
            }
        }

        log.debug("  Chapter {} (verse-by-verse): {} verses", chapterNumber, verses.size());
        return Map.of("chapterNumber", chapterNumber, "verses", verses);
    }

    /**
     * cdnTranslation: the CDN translation path to fetch this book from.
     *   Protestant books use "en-kjv" by default.
     *   DC books use "en-dra" since KJV does not contain them.
     *   When ingesting a full Catholic/Orthodox translation (e.g. DRA),
     *   the ingestion request overrides this via its own cdnTranslation.
     */
    public record BookDef(
         String id
        , String commonName
        , String testament
        , int    canonicalOrder
        , int    chapterCount
        , String cdnTranslation) {}
}

package org.religioustext.app.model;

/**
 * Immutable reference to a single verse, self-describing
 * with full address so it can stand alone outside tree context.
 * Used in streaming/chronological mode, search results,
 * commentary anchors and cross-reference panels.
 */
public final class VerseRef {

    private final String  sourceId;
    private final String  bookName;
    private final String  bookAltName;
    private final int     chapterNumber;
    private final String  chapterTitle;
    private final int     verseNumber;
    private final String  content;
    private final Integer globalCanonicalSeq;
    private final Integer globalChronologicalSeq;
    private final Integer globalNarrativeSeq;
    private final String  note;

    private VerseRef(final Builder builder) {
        this.sourceId               = builder.sourceId;
        this.bookName               = builder.bookName;
        this.bookAltName            = builder.bookAltName;
        this.chapterNumber          = builder.chapterNumber;
        this.chapterTitle           = builder.chapterTitle;
        this.verseNumber            = builder.verseNumber;
        this.content                = builder.content;
        this.globalCanonicalSeq     = builder.globalCanonicalSeq;
        this.globalChronologicalSeq = builder.globalChronologicalSeq;
        this.globalNarrativeSeq     = builder.globalNarrativeSeq;
        this.note                   = builder.note;
    }

    // ── Getters ───────────────────────────────────────────────────────

    public String  getSourceId()               { return sourceId; }
    public String  getBookName()               { return bookName; }
    public String  getBookAltName()            { return bookAltName; }
    public int     getChapterNumber()          { return chapterNumber; }
    public String  getChapterTitle()           { return chapterTitle; }
    public int     getVerseNumber()            { return verseNumber; }
    public String  getContent()                { return content; }
    public Integer getGlobalCanonicalSeq()     { return globalCanonicalSeq; }
    public Integer getGlobalChronologicalSeq() { return globalChronologicalSeq; }
    public Integer getGlobalNarrativeSeq()     { return globalNarrativeSeq; }
    public String  getNote()                   { return note; }

    /**
     * Returns the display content, uppercased if allCaps is set.
     */
    public String getDisplayContent(final boolean allCaps) {
        final String clean = cleanVerseText(content);
        return allCaps ? clean.toUpperCase() : clean;
    }

    /**
     * Strips artefacts from raw verse text:
     *   - Pilcrow signs and section markers (paragraph markers in source data)
     *   - Footnote text appended to verse (e.g. "1.4 the light from...")
     *   - Carriage returns, newlines, tabs
     */
    private static String cleanVerseText(final String raw) {
        if (raw == null) return "";
        return raw
            // Strip pilcrow and other paragraph/section markers
            .replace("\u00B6", "")   // ¶
            .replace("\u00A7", "")   // §
            // Strip appended footnotes — pattern: digits.digits followed by text
            .replaceAll("\\d+\\.\\d+\\s+[A-Z][^.]*(\\..*)?$", "")
            // Strip carriage returns, newlines, tabs
            .replaceAll("[\\r\\n\\t]+", " ")
            .trim();
    }

    /**
     * Human-readable address e.g. "Genesis 1:1" or "Al-Baqarah 2:1"
     */
    public String getAddress() {
        return bookName + " " + chapterNumber + ":" + verseNumber;
    }

    // ── Builder ───────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String  sourceId;
        private String  bookName;
        private String  bookAltName;
        private int     chapterNumber;
        private String  chapterTitle;
        private int     verseNumber;
        private String  content;
        private Integer globalCanonicalSeq;
        private Integer globalChronologicalSeq;
        private Integer globalNarrativeSeq;
        private String  note;

        public Builder sourceId(final String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder bookName(final String bookName) {
            this.bookName = bookName;
            return this;
        }

        public Builder bookAltName(final String bookAltName) {
            this.bookAltName = bookAltName;
            return this;
        }

        public Builder chapterNumber(final int chapterNumber) {
            this.chapterNumber = chapterNumber;
            return this;
        }

        public Builder chapterTitle(final String chapterTitle) {
            this.chapterTitle = chapterTitle;
            return this;
        }

        public Builder verseNumber(final int verseNumber) {
            this.verseNumber = verseNumber;
            return this;
        }

        public Builder content(final String content) {
            this.content = content;
            return this;
        }

        public Builder globalCanonicalSeq(final Integer globalCanonicalSeq) {
            this.globalCanonicalSeq = globalCanonicalSeq;
            return this;
        }

        public Builder globalChronologicalSeq(final Integer globalChronologicalSeq) {
            this.globalChronologicalSeq = globalChronologicalSeq;
            return this;
        }

        public Builder globalNarrativeSeq(final Integer globalNarrativeSeq) {
            this.globalNarrativeSeq = globalNarrativeSeq;
            return this;
        }

        public Builder note(final String note) {
            this.note = note;
            return this;
        }

        public VerseRef build() {
            return new VerseRef(this);
        }
    }
}

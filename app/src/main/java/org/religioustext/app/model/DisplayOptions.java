package org.religioustext.app.model;

/**
 * Encapsulates display state for one reader column.
 *
 * A single DisplayMode dropdown controls all structural display options.
 * Historical context for each mode is shown as a tooltip in the UI.
 */
public final class DisplayOptions {

    private DisplayMode mode      = DisplayMode.CHAPTERS_VERSES;
    private OrderMode   orderMode = OrderMode.CANONICAL;

    /**
     * ORIGINAL       : continuous uppercase, no chapter or verse breaks
     * CHAPTERS       : chapter headings only, prose flow within
     * CHAPTERS_VERSES: chapter headings + inline verse numbers (default)
     * TITLES         : chapter headings + titles + verses (disabled — no title data yet)
     */
    public enum DisplayMode {
         ORIGINAL(
             "Original"
            , "Original manuscripts were written in continuous uppercase "
            + "(scriptio continua) with no chapter or verse divisions"
            , false, false, true,  false)

        , CHAPTERS(
             "Chapters"
            , "Chapters added by Stephen Langton, Archbishop of Canterbury, c.1227 AD"
            , true,  false, false, false)

        , CHAPTERS_VERSES(
             "Chapters + Verses"
            , "Verses added by Robert Estienne (Stephanus), 1551 AD"
            , true,  true,  false, false)

        , TITLES(
             "Titles"
            , "Section titles are editorial additions varying by publisher. "
            + "Not yet available in current data sources"
            , true,  true,  false, true);

        private final String  label;
        private final String  tooltip;
        private final boolean showChapters;
        private final boolean showVerses;
        private final boolean allCaps;
        private final boolean disabled;

        DisplayMode(
                 final String  label
                , final String  tooltip
                , final boolean showChapters
                , final boolean showVerses
                , final boolean allCaps
                , final boolean disabled) {
            this.label        = label;
            this.tooltip      = tooltip;
            this.showChapters = showChapters;
            this.showVerses   = showVerses;
            this.allCaps      = allCaps;
            this.disabled     = disabled;
        }

        public String  getLabel()        { return label; }
        public String  getTooltip()      { return tooltip; }
        public boolean isShowChapters()  { return showChapters; }
        public boolean isShowVerses()    { return showVerses; }
        public boolean isAllCaps()       { return allCaps; }
        public boolean isDisabled()      { return disabled; }
    }

    public enum OrderMode {
         CANONICAL
        , CHRONOLOGICAL
        , NARRATIVE
    }

    // ── Getters ───────────────────────────────────────────────────────

    public DisplayMode getMode()              { return mode; }
    public OrderMode   getOrderMode()         { return orderMode; }
    public boolean     isShowChapters()       { return mode.isShowChapters(); }
    public boolean     isShowChapterTitles()  { return false; }  // deferred
    public boolean     isShowVerses()         { return mode.isShowVerses(); }
    public boolean     isAllCaps()            { return mode.isAllCaps(); }

    // ── Setters ───────────────────────────────────────────────────────

    public void setMode(final DisplayMode mode) {
        this.mode = mode;
    }

    public void setOrderMode(final OrderMode orderMode) {
        this.orderMode = orderMode;
    }

    // ── Factory ───────────────────────────────────────────────────────

    public static DisplayOptions defaults() {
        return new DisplayOptions();
    }
}

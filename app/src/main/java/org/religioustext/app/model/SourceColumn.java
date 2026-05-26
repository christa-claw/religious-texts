package org.religioustext.app.model;

/**
 * Represents one column in the multi-column reader view.
 * Each column displays one translation/source independently
 * but all columns sync to the same current reference point.
 * Display options are per-column so one column can show CAPS
 * while another shows normal prose.
 */
public final class SourceColumn {

    private final String         sourceId;
    private final String         translation;
    private final String         abbreviation;
    private final String         direction;
    private final DisplayOptions displayOptions;

    public SourceColumn(
         final String         sourceId
        , final String         translation
        , final String         abbreviation
        , final String         direction
        , final DisplayOptions displayOptions) {

        this.sourceId       = sourceId;
        this.translation    = translation;
        this.abbreviation   = abbreviation;
        this.direction      = direction;
        this.displayOptions = displayOptions;
    }

    public String         getSourceId()       { return sourceId; }
    public String         getTranslation()    { return translation; }
    public String         getAbbreviation()   { return abbreviation; }
    public String         getDirection()      { return direction; }
    public DisplayOptions getDisplayOptions() { return displayOptions; }

    public boolean isRtl() {
        return "rtl".equalsIgnoreCase(direction);
    }
}

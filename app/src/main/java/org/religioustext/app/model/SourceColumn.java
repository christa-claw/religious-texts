package org.religioustext.app.model;

/**
 * Represents one column in the multi-column reader view.
 * Each column displays one translation/source independently
 * but all columns sync to the same current reference point.
 * Display options are per-column so one column can show CAPS
 * while another shows normal prose.
 */
public final class SourceColumn {

    private String         sourceId;
    private String         translation;
    private String         abbreviation;
    private String         direction;
    private final DisplayOptions displayOptions;
    private boolean        synced = true;

    public SourceColumn(final DisplayOptions displayOptions) {
        this.displayOptions = displayOptions;
    }

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
    public boolean        isSynced()          { return synced; }

    public void setSource(final String sourceId, final String translation,
                          final String abbreviation, final String direction) {
        this.sourceId    = sourceId;
        this.translation = translation;
        this.abbreviation = abbreviation;
        this.direction   = direction;
    }

    public void toggleSync() { this.synced = !this.synced; }

    public boolean isRtl() {
        return "rtl".equalsIgnoreCase(direction);
    }
}

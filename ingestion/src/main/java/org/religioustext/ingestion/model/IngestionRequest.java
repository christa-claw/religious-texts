package org.religioustext.ingestion.model;

/**
 * Describes a single ingestion request — what to fetch,
 * what format it is, and what metadata to attach.
 */
public final class IngestionRequest {

    public enum SourceType  { BIBLE, QURAN, TORAH, COMMENTARY }
    public enum SourceFormat { OSIS, TEI, ZEFANIA, PLAIN, AUTO_DETECT }

    private final String       sourceUrl;
    private final SourceType   sourceType;
    private final SourceFormat sourceFormat;
    private final String       translation;
    private final String       abbreviation;
    private final String       bcp47Language;
    private final String       iso639_3;
    private final String       direction;
    private final Integer      year;
    private final String       license;
    private final String       region;

    private IngestionRequest(final Builder builder) {
        this.sourceUrl     = builder.sourceUrl;
        this.sourceType    = builder.sourceType;
        this.sourceFormat  = builder.sourceFormat;
        this.translation   = builder.translation;
        this.abbreviation  = builder.abbreviation;
        this.bcp47Language = builder.bcp47Language;
        this.iso639_3      = builder.iso639_3;
        this.direction     = builder.direction;
        this.year          = builder.year;
        this.license       = builder.license;
        this.region        = builder.region;
    }

    public String       getSourceUrl()     { return sourceUrl; }
    public SourceType   getSourceType()    { return sourceType; }
    public SourceFormat getSourceFormat()  { return sourceFormat; }
    public String       getTranslation()   { return translation; }
    public String       getAbbreviation()  { return abbreviation; }
    public String       getBcp47Language() { return bcp47Language; }
    public String       getIso639_3()      { return iso639_3; }
    public String       getDirection()     { return direction; }
    public Integer      getYear()          { return year; }
    public String       getLicense()       { return license; }
    public String       getRegion()        { return region; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        private String       sourceUrl;
        private SourceType   sourceType;
        private SourceFormat sourceFormat  = SourceFormat.AUTO_DETECT;
        private String       translation;
        private String       abbreviation;
        private String       bcp47Language = "en";
        private String       iso639_3      = "eng";
        private String       direction     = "ltr";
        private Integer      year;
        private String       license;
        private String       region;

        public Builder sourceUrl(final String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder sourceType(final SourceType sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder sourceFormat(final SourceFormat sourceFormat) {
            this.sourceFormat = sourceFormat;
            return this;
        }

        public Builder translation(final String translation) {
            this.translation = translation;
            return this;
        }

        public Builder abbreviation(final String abbreviation) {
            this.abbreviation = abbreviation;
            return this;
        }

        public Builder bcp47Language(final String bcp47Language) {
            this.bcp47Language = bcp47Language;
            return this;
        }

        public Builder iso639_3(final String iso639_3) {
            this.iso639_3 = iso639_3;
            return this;
        }

        public Builder direction(final String direction) {
            this.direction = direction;
            return this;
        }

        public Builder year(final Integer year) {
            this.year = year;
            return this;
        }

        public Builder license(final String license) {
            this.license = license;
            return this;
        }

        public Builder region(final String region) {
            this.region = region;
            return this;
        }

        public IngestionRequest build() {
            return new IngestionRequest(this);
        }
    }
}

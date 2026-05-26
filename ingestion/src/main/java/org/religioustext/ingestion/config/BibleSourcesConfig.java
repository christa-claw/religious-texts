package org.religioustext.ingestion.config;

import org.religioustext.ingestion.model.IngestionRequest;
import org.religioustext.ingestion.model.IngestionRequest.SourceType;
import org.religioustext.ingestion.model.IngestionRequest.SourceFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Loads Bible translation sources from bible-sources.yml.
 * Each entry maps to an IngestionRequest with an API.Bible ID.
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "")
public class BibleSourcesConfig {

    private static final Logger log = LoggerFactory.getLogger(BibleSourcesConfig.class);

    private List<TranslationDef> translations;

    public List<TranslationDef> getTranslations() {
        return translations;
    }

    public void setTranslations(final List<TranslationDef> translations) {
        this.translations = translations;
    }

    public List<IngestionRequest> toIngestionRequests() {
        if (translations == null) return List.of();
        return translations.stream()
            .map(this::toRequest)
            .toList();
    }

    public IngestionRequest findByAbbreviation(final String abbreviation) {
        if (translations == null) return null;
        return translations.stream()
            .filter(t -> t.getAbbreviation().equalsIgnoreCase(abbreviation))
            .findFirst()
            .map(this::toRequest)
            .orElse(null);
    }

    private IngestionRequest toRequest(final TranslationDef def) {
        final String sourceUrl = def.isLocal()
            ? "local:" + def.getLocalCode() + ":" + (def.getApiBibleId() != null ? def.getApiBibleId() : "")
            : "https://rest.api.bible/v1/bibles/" + def.getApiBibleId();
        return IngestionRequest.builder()
            .sourceUrl(sourceUrl)
            .sourceType(SourceType.BIBLE)
            .sourceFormat(SourceFormat.AUTO_DETECT)
            .translation(def.getTranslation())
            .abbreviation(def.getAbbreviation())
            .bcp47Language(def.getBcp47Language())
            .iso639_3(def.getIso639_3())
            .direction(def.getDirection() != null ? def.getDirection() : "ltr")
            .year(def.getYear())
            .license(def.getLicense() != null ? def.getLicense() : "Public Domain")
            .region(def.getRegion())
            .build();
    }

    public static class TranslationDef {
        private String  id;
        private String  translation;
        private String  abbreviation;
        private String  apiBibleId;
        private String  localCode;
        private String  bcp47Language;
        private String  iso639_3;
        private String  direction;
        private Integer year;
        private String  license;
        private String  region;

        public String  getId()            { return id; }
        public String  getTranslation()   { return translation; }
        public String  getAbbreviation()  { return abbreviation; }
        public String  getApiBibleId()    { return apiBibleId; }
        public String  getLocalCode()     { return localCode; }
        public boolean isLocal()          { return localCode != null && !localCode.isBlank(); }
        public String  getBcp47Language() { return bcp47Language; }
        public String  getIso639_3()      { return iso639_3; }
        public String  getDirection()     { return direction; }
        public Integer getYear()          { return year; }
        public String  getLicense()       { return license; }
        public String  getRegion()        { return region; }

        public void setId(final String id)                       { this.id = id; }
        public void setTranslation(final String v)               { this.translation = v; }
        public void setAbbreviation(final String v)              { this.abbreviation = v; }
        public void setApiBibleId(final String v)                { this.apiBibleId = v; }
        public void setLocalCode(final String v)                 { this.localCode = v; }
        public void setBcp47Language(final String v)             { this.bcp47Language = v; }
        public void setIso639_3(final String v)                  { this.iso639_3 = v; }
        public void setDirection(final String v)                 { this.direction = v; }
        public void setYear(final Integer v)                     { this.year = v; }
        public void setLicense(final String v)                   { this.license = v; }
        public void setRegion(final String v)                    { this.region = v; }
    }
}

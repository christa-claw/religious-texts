# Religious Texts Platform — Claude Notes

Running notes updated by Claude on request.
Last updated: 2026-05-25

---

## Project Overview

A Vaadin-based study platform for reading, comparing and studying religious texts
across translations, languages and historical orderings.

**Mac Mini M4** — dedicated AI/dev machine
**GitHub** — https://github.com/christa-claw/religious-texts

---

## Architecture

```
Ingestion Pipeline (port 8091)     Vaadin App (port 8090)
  Spring Boot                         Spring Boot + Vaadin 24
  ApiBibleCrawler                     ReaderView (multi-column)
  BibleApiParser                      TextQueryService
  BaseXStore                          BaseXConfig
        │                                   │
        ▼                                   ▼
  BaseX XML DB (port 8984)        MySQL (port 3306)
  Religious texts                 User comments
  One document per translation    Reader session state
                                  Ingestion job log
                                  Source metadata
```

Both databases run in Docker — `docker/docker-compose.yml`.

---

## Key Design Decisions

### XML Schema (schema/religious-text.xsd) v1.3
- One XML document per translation stored in BaseX
- Translation identity in root `<text>` attributes
- Bible/Torah: `text > book > chapter > verse`
- Quran: `text > chapter > verse` (no book wrapper)
- Three orderings on each verse:
  - `globalCanonicalSeq` — traditional printed order
  - `globalChronologicalSeq` — scholarly compositional order
  - `globalNarrativeSeq` — internal story timeline
- Verse carries full address (`bookName`, `chapterNumber`) for streaming mode
- ALL CAPS and display toggles are render-time only — never stored

### Canon Support
Three separate book lists in `BibleApiCrawler`:
- `PROTESTANT_BOOKS` — 66 books
- `CATHOLIC_BOOKS` — 73 books (DC books in correct canonical positions)
- `ORTHODOX_BOOKS` — 78 books

### Display Mode Dropdown (per column)
Replaces checkboxes — single dropdown with historical context as tooltip:
- **Original** — continuous uppercase, no breaks (scriptio continua)
- **Chapters** — chapter headings only, prose within (Langton c.1227)
- **Chapters + Verses** — chapter headings + verse numbers (Estienne 1551) ← default
- **Titles** — disabled, pending data source with section headings

### Data Sources
- **Bible**: API.Bible (scripture.api.bible) — clean data, chapter titles, footnotes separate
  - API key stored in `application-local.properties` (gitignored)
  - 5K requests/day free tier — ~1 translation per day
  - 3 licensed slots used for: NIV, NASB20, NBLA
- **Quran**: fawazahmed0/quran-api — 440+ translations, no key, no limits
  - NOT YET IMPLEMENTED
- **Commentaries**: source not yet identified

### Ingestion Pipeline
Producer/consumer threading:
- 5 crawler threads (network bound, parallel)
- 1 insert thread (serialised BaseX writes)
- `BlockingQueue<CrawledBook>` between them
- MDC logging: `[ABBREVIATION:BookName]` on every log line
- Book-by-book insertion into BaseX via XQuery update

### BaseX REST API
- PUT  `/rest/{database}/{document}` — create document (via curl, avoids Spring encoding)
- POST `/rest/{database}` with `<query xmlns="http://basex.org/rest">` wrapper — XQuery
- XQuery insert: `db:open('religioustext','bible-kjv')/rt:text`

### Properties Split
- `application.properties` — committed to git, safe, no secrets
- `application-local.properties` — gitignored, secrets only (API keys, passwords)
- `application-local.properties.template` — committed, shows what to fill in
- Spring Boot merges both automatically on startup

---

## Known Issues / TODOs

- [ ] Schema validation disabled in XmlNormaliser — namespace mismatch on root element.
      Does NOT affect BaseX query performance. Fix before next major release.
- [ ] `globalChronologicalSeq` and `globalNarrativeSeq` not yet populated.
- [ ] Quran ingestion not started — use fawazahmed0/quran-api.
- [ ] Commentary ingestion not started — need to identify open source dataset.
- [ ] User comments not wired to UI — MySQL tables exist, needs Spring Security + login.
- [ ] OpenClaw integration deferred — may use local LLM instead of Claude API.
- [ ] API.Bible parser not yet written — ApiBibleCrawler exists but parser needs
      updating for API.Bible JSON format (different from wldeh format).

---

## Ingestion Status

| Translation | Language | Source | Status |
|---|---|---|---|
| KJV | English | API.Bible de4e12af7f28f599-01 | ⏳ Ready to ingest |
| ASV | English | API.Bible 06125adad2d5898a-01 | ⏳ Ready |
| WEB | English | API.Bible 9879dbb7cfe39e4d-01 | ⏳ Ready |
| DRA | English Catholic | API.Bible 179568874c45066f-01 | ⏳ Ready |
| NIV | English Licensed | API.Bible 78a9f6124f344018-01 | ⏳ Ready (slot 1/3) |
| NASB20 | English Licensed | API.Bible a761ca71e0b3ddcf-01 | ⏳ Ready (slot 2/3) |
| RVR09 | Spanish | API.Bible 592420522e16049f-01 | ⏳ Ready |
| NBLA | Spanish Licensed | API.Bible ce11b813f9a27e20-01 | ⏳ Ready (slot 3/3) |
| LUT1912 | German | API.Bible 926aa5efbc5e04e2-01 | ⏳ Ready |
| AEUUT | Finnish | API.Bible c739534f6a23acb2-01 | ⏳ Ready |
| NAV | Arabic | API.Bible b17e246951402e50-01 | ⏳ Ready |
| WLC | Biblical Hebrew | API.Bible 0b262f1ed7f084a6-01 | ⏳ Ready |
| GRCTR | Ancient Greek | API.Bible 3aefb10641485092-01 | ⏳ Ready |

BaseX wiped clean 2026-05-25 — all previous CDN-ingested documents deleted
(contained KJV text due to CDN silent fallback on all translations).

---

## Next Session Priority

1. Write API.Bible parser (BibleApiParser rewrite for API.Bible JSON format)
2. Test single chapter fetch and parse for KJV
3. Run full KJV ingestion overnight
4. Write Quran ingestion (fawazahmed0/quran-api)
5. Fix schema validation namespace issue
6. Add Spring Security + login + user comments UI

---

## Pending Actions for Next Session

- Apply `properties-split.zip` if not done
- Apply `apibible-ingestion.zip` if not done
- Rewrite `BibleApiParser` for API.Bible JSON format
- Rewrite `IngestionService` to use `ApiBibleCrawler` instead of old crawler
- Update `BibleSourcesConfig` to read `apiBibleId` field instead of `cdnTranslation`

---

## Docker

```bash
cd docker
docker-compose up -d      # start
docker-compose down       # stop (keeps data)
docker-compose down -v    # full reset including data
```

BaseX web UI: http://localhost:8984
MySQL: localhost:3306 / rtuser / (see application-local.properties)

---

## API.Bible

API key: in `application-local.properties` (gitignored)
Rate limit: 5K requests/day free tier
Dashboard: https://scripture.api.bible

### Useful API calls
```bash
# List all available Bibles
curl --request GET \
  --url https://rest.api.bible/v1/bibles \
  --header 'api-key: YOUR_KEY'

# Fetch a chapter
curl --request GET \
  --url "https://rest.api.bible/v1/bibles/de4e12af7f28f599-01/chapters/GEN.1?content-type=json&include-titles=true&include-verse-numbers=true" \
  --header 'api-key: YOUR_KEY'
```

---

## Useful Curl Commands

```bash
# Check what's in BaseX
curl -u admin:admin http://localhost:8984/rest/religioustext

# Trigger ingestion (once pipeline is rewritten for API.Bible)
curl -X POST http://localhost:8091/ingest/KJV
curl -X POST http://localhost:8091/ingest/all

# List configured translations
curl http://localhost:8091/ingest/status
```

---

## Architecture Decisions (Confirmed)

### Data Source — API.Bible only
- Single implementation, richest dataset, chapter titles included
- wldeh/bible-api local clone no longer needed

### Quran — fawazahmed0/quran-api
- 440+ translations, 90+ languages, no API key, no rate limits
- Single JSON file per translation = clean ingestion

### Commentaries
- Find open source commentary dataset to test the model
- Anchor model: commentary entry references (book, chapter, verse) independent of translation

### User Comments + Login
- Spring Security + local username/password to start
- Each comment anchored to (sourceId, book, chapter, verse)
- UI: margin annotation panel alongside reader columns

### OpenClaw Integration
- Deferred — may use local LLM via Ollama instead of Claude API

## Display Mode Dropdown (replaces checkboxes)

| Option | Chapters | Verses | CAPS | Tooltip |
|---|---|---|---|---|
| Original | ❌ | ❌ | ✅ | scriptio continua — original manuscript style |
| Chapters | ✅ | ❌ | ❌ | Added by Stephen Langton c.1227 AD |
| Chapters + Verses | ✅ | ✅ | ❌ | Verses added by Robert Estienne 1551 AD |
| Titles | ✅ | ✅ | ❌ | Disabled — no title data in current sources |


## Session Notes 2026-05-25 (Evening)

### What was completed
- API.Bible crawler written (ApiBibleCrawler.java)
- API.Bible parser written (BibleApiParser.java) — handles complex nested JSON format
- IngestionService rewritten for API.Bible with producer/consumer threading
- BibleSourcesConfig updated to use apiBibleId field
- bible-sources.yml updated with correct API.Bible IDs for 13 translations
- Properties split into public/secret: application.properties + application-local.properties
- Stale misplaced files cleaned up from store/xmldb/ directory
- IngestionApp.java and IngestionConfig.java recreated after accidental deletion
- ingestion/pom.xml mainClass property added
- BaseX wiped clean — ready for fresh API.Bible data

### Known issue at end of session
- Ingestion app starts (Tomcat on 8091) but /ingest/* endpoints return 404
- IngestionController.java has correct package and annotations
- Likely cause: Spring component scan not picking up controller
  — check if IngestionApp @SpringBootApplication scanBasePackages is needed
  — or stale compiled class conflict in target/
- Fix for next session:
  1. mvn clean compile
  2. Check: curl http://localhost:8091/ingest/status
  3. If still 404: add scanBasePackages to IngestionApp:
     @SpringBootApplication(scanBasePackages = "org.religioustext.ingestion")

### Next session priorities
1. Fix /ingest/* 404 issue
2. Trigger KJV ingestion and verify clean data in BaseX
3. Run remaining translations overnight
4. Write Quran ingestion (fawazahmed0/quran-api)
5. Fix schema validation namespace issue
6. Add Spring Security + login + user comments UI

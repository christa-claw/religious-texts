# Religious Texts Platform — Claude Notes

Running notes updated by Claude on request.
Last updated: 2026-05-26

---

## Project Overview

A Vaadin-based study platform for reading, comparing and studying religious texts
across translations, languages and historical orderings.

**Mac Mini M4** — dedicated AI/dev machine
**GitHub** — https://github.com/christa-claw/religious-texts (remote named "hub")
**Local project path** — /Volumes/VMs/data/Claude/religious-texts

---

## Architecture

```
Ingestion Pipeline (port 8091)     Vaadin App (port 8090)
  Spring Boot                         Spring Boot + Vaadin 24
  ApiBibleCrawler                     ReaderView (multi-column)
  LocalBibleReader                    TextQueryService
  LocalIngestionService               BaseXConfig
  IngestionService
        │                                   │
        ▼                                   ▼
  BaseX XML DB (port 8984)        MySQL (port 3306)
  Religious texts                 User comments
  One document per translation    Reader session state
```

Both databases run in Docker — `docker/docker-compose.yml`.

---

## Module Structure

```
religious-texts/
├── app/                          Vaadin reader application (port 8090)
│   └── src/main/java/org/religioustext/app/
│       ├── ReligiousTextsApp.java
│       ├── config/BaseXConfig.java
│       ├── model/DisplayOptions.java
│       ├── model/SourceColumn.java
│       ├── model/VerseRef.java
│       ├── service/TextQueryService.java
│       └── ui/views/ReaderView.java
├── ingestion/                    Ingestion pipeline (port 8091)
│   └── src/main/java/org/religioustext/ingestion/
│       ├── IngestionApp.java
│       ├── IngestionController.java    REST: /ingest/status, /ingest/{ABB}, /ingest/all
│       ├── IngestionService.java       API.Bible full ingestion
│       ├── LocalIngestionService.java  Local clone + API.Bible titles
│       ├── config/
│       │   ├── BibleSourcesConfig.java  Loads bible-sources.yml
│       │   └── IngestionConfig.java     RestTemplate bean
│       ├── crawler/
│       │   ├── ApiBibleCrawler.java     Fetches from API.Bible REST API
│       │   └── LocalBibleReader.java    Reads from local git clone
│       ├── normaliser/XmlNormaliser.java
│       ├── parser/BibleApiParser.java   Parses API.Bible JSON → XML
│       └── store/xmldb/BaseXStore.java  Stores to BaseX via curl
├── schema/religious-text.xsd     Canonical XML schema v1.3
├── docker/docker-compose.yml
└── CLAUDE_NOTES.md               This file
```

---

## Key Design Decisions

### XML Schema (schema/religious-text.xsd) v1.3
- One XML document per translation stored in BaseX
- Translation identity in root `<text>` attributes
- Bible: `text > book > chapter > verse`
- Quran: `text > chapter > verse` (no book wrapper)
- Three orderings on each verse:
  - `globalCanonicalSeq` — traditional printed order
  - `globalChronologicalSeq` — scholarly compositional order (not yet populated)
  - `globalNarrativeSeq` — internal story timeline (not yet populated)
- Verse carries `bookName`, `chapterNumber` for streaming
- Chapter carries optional `title` attribute (section heading from API.Bible)

### Display Mode Dropdown (per column)
Replaces old checkboxes — single dropdown with historical context as tooltip:

| Option | Chapters | Verses | CAPS | Tooltip |
|---|---|---|---|---|
| Original | ❌ | ❌ | ✅ | scriptio continua — original manuscript style |
| Chapters | ✅ | ❌ | ❌ | Added by Stephen Langton c.1227 AD |
| Chapters + Verses | ✅ | ✅ | ❌ | Verses added by Robert Estienne 1551 AD ← default |
| Titles | ✅ | ✅ | ❌ | Disabled — no title data in wldeh source |

### Data Sources

**Bible — two source types:**

1. **API.Bible** (scripture.api.bible) — licensed/rich translations
   - Clean data, chapter titles, footnotes separate
   - API key in `application-local.properties` (gitignored)
   - Rate limit: 5K requests/day free tier
   - Used for: NIV (done), NASB20, NBLA
   - `IngestionService.java` handles this path

2. **Local git clone** (wldeh/bible-api) — open source translations
   - Cloned to: `/Volumes/VMs/data/bible-api-source/`
   - Structure: `bibles/{code}/books/{book}/chapters/{N}/verses/{N}.json`
   - Book folders use lowercase no-hyphen names (e.g. `1samuel`, `songofsolomon`)
   - KJV clone includes DC/Apocrypha books (80 folders total)
   - Text from local files + titles from API.Bible for same translation
   - If no titles available in API.Bible → leave empty (correctness over completeness)
   - `LocalIngestionService.java` handles this path
   - `LocalBibleReader.java` discovers books/chapters dynamically from folder structure

**Ingestion routing** — `IngestionController` checks `sourceUrl`:
- `sourceUrl` starts with `"local:"` → `LocalIngestionService`
- `sourceUrl` starts with `"https://rest.api.bible"` → `IngestionService`

**bible-sources.yml** — configured translations:
- `apiBibleId` field → API.Bible translation ID
- `localCode` field → local clone folder name (e.g. `en-kjv`)
- Both fields present on local translations (localCode for text, apiBibleId for titles)

**Quran** — fawazahmed0/quran-api — NOT YET IMPLEMENTED
- 440+ translations, no API key, no rate limits
- Single JSON file per translation

### Properties Split
- `application.properties` — committed to git, no secrets
- `application-local.properties` — gitignored, secrets only
- `application-local.properties.template` — committed, shows what to fill in
- Both files in `ingestion/src/main/resources/` and `app/src/main/resources/`
- Spring Boot merges both via `spring.config.import`

### BaseX REST API
- PUT `/rest/{database}/{document}` — create document (via curl, avoids Spring encoding)
- POST `/rest/{database}` with `<query xmlns="http://basex.org/rest">` wrapper — XQuery
- XQuery insert: `db:open('religioustext','bible-niv-2011')/rt:text`
- Document stored as `bible-niv-2011.xml` in BaseX

### Threading (IngestionService + LocalIngestionService)
Producer/consumer pattern:
- 5 crawler threads (network bound, parallel)
- 1 insert thread (serialised BaseX writes, no conflicts)
- `BlockingQueue<CrawledBook>` between them
- MDC logging: `[ABBREVIATION:BookName]` on every log line

---

## API.Bible

**Key:** in `application-local.properties` (gitignored)
**Rate limit:** 5K requests/day free tier
**Dashboard:** https://scripture.api.bible
**Licensed slots (3 free):** NIV, NASB20, NBLA

### Translation IDs

| Abbreviation | Language | API.Bible ID | Source |
|---|---|---|---|
| NIV | English | 78a9f6124f344018-01 | API.Bible (licensed, slot 1) |
| NASB20 | English | a761ca71e0b3ddcf-01 | API.Bible (licensed, slot 2) |
| NBLA | Spanish | ce11b813f9a27e20-01 | API.Bible (licensed, slot 3) |
| KJV | English | de4e12af7f28f599-01 | Local clone + API.Bible titles |
| ASV | English | 06125adad2d5898a-01 | Local clone + API.Bible titles |
| WEB | English | 9879dbb7cfe39e4d-01 | Local clone + API.Bible titles |
| DRA | English Catholic | 179568874c45066f-01 | Local clone + API.Bible titles |
| RVR09 | Spanish | 592420522e16049f-01 | Local clone + API.Bible titles |
| LUT1912 | German | 926aa5efbc5e04e2-01 | API.Bible |
| AEUUT | Finnish | c739534f6a23acb2-01 | API.Bible |
| NAV | Arabic | b17e246951402e50-01 | API.Bible |
| WLC | Biblical Hebrew | 0b262f1ed7f084a6-01 | API.Bible |
| GRCTR | Ancient Greek | 3aefb10641485092-01 | API.Bible |

---

## Ingestion Status

| Translation | Language | Status |
|---|---|---|
| NIV 2011 | English | ✅ Complete — 30,752 verses, chapter titles working |
| KJV 1611 | English | ⏳ Ready — local clone ingestion pending |
| ASV 1901 | English | ⏳ Ready |
| WEB | English | ⏳ Ready |
| DRA 1899 | English Catholic | ⏳ Ready |
| RVR09 1909 | Spanish | ⏳ Ready |
| NASB20 | English | ⏳ Ready — API.Bible licensed slot 2 |
| NBLA | Spanish | ⏳ Ready — API.Bible licensed slot 3 |
| LUT1912 | German | ⏳ Ready |
| AEUUT | Finnish | ⏳ Ready |
| Quran | Multiple | ❌ Not started |

BaseX currently contains: `bible-niv-2011.xml` (183KB, 30,752 verses)

---

## Known Issues / TODOs

- [ ] **application.properties missing local import** — `spring.config.import` only has
      `bible-sources.yml`, not `application-local.properties`. Fix:
      Change line in `ingestion/src/main/resources/application.properties`:
      ```
      spring.config.import=optional:classpath:bible-sources.yml,optional:classpath:application-local.properties
      ```
      This causes 401 Unauthorized when triggering ingestion.

- [ ] **NIV book names abbreviated** — API.Bible returns short names (e.g. `Lev.` instead
      of `Leviticus`). Need to map API.Bible book IDs to full display names in parser.

- [ ] Schema validation disabled in XmlNormaliser — namespace mismatch on root element.
      Does NOT affect BaseX query performance. Fix when convenient.

- [ ] `globalChronologicalSeq` and `globalNarrativeSeq` not yet populated.

- [ ] Quran ingestion not started — use fawazahmed0/quran-api.

- [ ] Commentary ingestion not started — need open source dataset.

- [ ] User comments not wired to UI — MySQL tables exist, needs Spring Security + login.

- [ ] TextQueryService uses document path without `.xml` suffix — may need updating
      to match BaseX storage format `bible-niv-2011.xml`.

- [ ] App module display mode dropdown applied but not tested since NIV book name issue.

---

## Next Session Priorities

1. Fix `spring.config.import` in application.properties (one line fix)
2. Restart ingestion and trigger KJV local clone ingestion
3. Fix NIV abbreviated book names in BibleApiParser
4. Trigger NASB20 and NBLA from API.Bible
5. Trigger remaining local clone translations
6. Start Quran ingestion
7. Fix schema validation namespace issue
8. Add Spring Security + login + user comments UI

---

## Docker

```bash
cd /Volumes/VMs/data/Claude/religious-texts/docker
docker-compose up -d      # start
docker-compose down       # stop (keeps data)
docker-compose down -v    # full reset including data
```

BaseX web UI: http://localhost:8984
MySQL: localhost:3306 / rtuser / (see application-local.properties)

---

## Useful Commands

```bash
# Check what's in BaseX
curl -u admin:admin http://localhost:8984/rest/religioustext

# Check NIV document
curl -s -u admin:admin "http://localhost:8984/rest/religioustext/bible-niv-2011.xml" | head -c 500

# List configured translations
curl http://localhost:8091/ingest/status

# Trigger ingestion
curl -X POST http://localhost:8091/ingest/KJV    # local clone
curl -X POST http://localhost:8091/ingest/NASB   # API.Bible
curl -X POST http://localhost:8091/ingest/all    # all in background

# Kill ingestion process
lsof -ti:8091 | xargs kill -9

# Start ingestion
cd /Volumes/VMs/data/Claude/religious-texts/ingestion && mvn spring-boot:run

# Start app
cd /Volumes/VMs/data/Claude/religious-texts/app && mvn spring-boot:run

# Git push
git add -A && git commit -m "message" && git push hub main
```

---

## GitHub Token

Stored in `.git/config` under remote named `hub`. Never committed.

---

## Local Bible Clone

Path: `/Volumes/VMs/data/bible-api-source/`
Structure: `bibles/{code}/books/{bookfolder}/chapters/{N}/verses/{N}.json`
Book folder names: lowercase, no hyphens (e.g. `1samuel`, `songofsolomon`, `1corinthians`)
Includes DC/Apocrypha: `tobit`, `wisdom`, `susanna`, `songofthethree`, `1maccabees` etc.
Available translations include: `en-kjv`, `en-asv`, `en-web`, `en-dra`, `es-rv09` and many more.


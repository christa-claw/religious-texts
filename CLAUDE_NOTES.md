# Religious Texts Platform — Claude Notes

Running notes updated by Claude on request.
Last updated: 2026-05-27 (session 3)

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

### Column Layout (ReaderView)
- Horizontal list of columns, each fixed at 33% viewport width
- Adding columns doesn't resize existing ones — they scroll off-screen to the right
- No hard column limit; 3–5 is typical use
- Column types: Bible translation, Quran translation, Bible commentary, Quran commentary, personal notes
- **Vertical sync**: all columns scroll together by default to the same passage
- **Break sync**: per-column toggle to allow independent scrolling (e.g. comparing non-parallel passages)
- **Column header**: book / chapter / verse navigation selectors
- **Source selection**: separate per-column settings panel (translation, commentary version etc.)
- **Drag to reorder** columns
- **Add column** button — opens picker for column type and source
- **Remove column** button per column

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

### UI Layout & Column Model

- Horizontal list of equal-width columns, each fixed at 33% viewport width
- Adding columns doesn't resize existing ones — they scroll off-screen to the right
- No hard limit on columns — 5 is the practical sweet spot but unlimited
- Each column is independently configurable — any column can be:
  - A Bible translation
  - A Quran translation
  - A Bible commentary
  - A Quran commentary
  - A Hadith collection
  - Personal notes/comments

**Vertical sync:**
- Columns scroll together by default (same passage)
- Sync is breakable per column — user can unlink a column to browse independently
- Unlinked columns show a visual indicator (e.g. broken chain icon)

**Column header:**
- Dropdown to select what's in the column (translation, commentary, hadith collection etc.)
- Reordering: TBD (drag to reorder or just add/remove)

**Cross-references:**
- Commentary columns show verse references as clickable links
- Clicking a reference navigates the synced Bible/Quran column to that passage
- If the referenced text is not open in any column, offer to open it in a new column

---

### Data Sources — Planned

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
| KJV 1611 | English | ✅ Complete — 36,820 verses (inc. Apocrypha), canonical order fixed |
| ASV 1901 | English | ✅ Complete — local clone |
| WEB | English | ✅ Complete — local clone |
| DRA 1899 | English Catholic | ✅ Complete — local clone |
| RVR09 1909 | Spanish | ✅ Complete — 31,102 verses, local clone |
| NASB20 | English | ⏳ Ready — API.Bible licensed slot 2 |
| NBLA | Spanish | ⏳ Ready — API.Bible licensed slot 3 |
| LUT1912 | German | ⏳ Ready |
| AEUUT | Finnish | ⏳ Ready |
| NAV | Arabic | ⏳ Ready |
| WLC | Biblical Hebrew | ⏳ Ready |
| GRCTR | Ancient Greek | ⏳ Ready |
| Quran | Multiple | ❌ Not started |

BaseX currently contains:
- `bible-niv-2011.xml` (183KB, 30,752 verses) ✅
- `bible-kjv-1611` (187KB, 36,820 verses inc. Apocrypha) ✅
- `bible-asv-1901` (158KB) ✅
- `bible-dra-1899` (181KB) ✅
- `bible-web` (192KB) ✅
- `bible-rvr09-1909` (158KB, 31,102 verses) ✅

---

## Session 2 Notes (2026-05-26)

- Checked BaseX: NIV and KJV both present
- KJV `canonicalOrder` was wrong — `LocalIngestionService` used thread submission counter (alphabetical) instead of `BOOK_META.canonicalOrder()`
- **Fixed** `LocalIngestionService.java`: now uses `meta.canonicalOrder()`; removed `orderCounter` and broken `order <= 39` testament fallback; unknown books default to `"DC"`
- KJV has no `totalVerses` attribute on root — set via `store.setTotalVerses()` post-ingestion; NIV path may differ — worth checking consistency
- KJV document name has no `.xml` suffix (`bible-kjv-1611` vs `bible-niv-2011.xml`) — worth standardising
- NIV abbreviated book names (e.g. `Lev.`) still an open TODO
- **Fixed** `BaseXStore.java`: `proc.waitFor()` replaced with `proc.waitFor(30, TimeUnit.SECONDS)` + `destroyForcibly()` on timeout in both `put()` and `postXQuery()` — eliminates spurious InterruptedException noise
- **Fixed** `LocalIngestionService.java` `BOOK_META` folder name mismatches: `sirach`→`ecclesiasticus`, `manasses`→`manasseh`, `bel`→`belandthedragon`, added `esther(greek)`
- KJV re-ingested cleanly — 80 books, correct canonical order, 36,820 verses
- Committed and pushed all changes
- Added `sourceUrl` attribution to all 13 translations in `bible-sources.yml`
- Wired `attributionUrl` through `BibleSourcesConfig` → `IngestionRequest` → `BaseXStore.createRootDocument()` → XML `source=` attribute
- `TextQueryService.listSources()` now returns license (index 4) and source URL (index 5)
- `SourceColumn` stores `license` and `attributionUrl`
- `ReaderView` column header shows ℹ️ anchor linking to source when available
- Patched existing BaseX documents with source URLs via XQuery (no re-ingest needed)
- Planned: About + Help page at `/about` with sources & licensing section

---

### Infrastructure

| Component | Technology | Port |
|---|---|---|
| Text database | BaseX 10 (native XML) | 8984 |
| User/comment database | MySQL 8.3 | 3306 |
| App server | Embedded Tomcat (app module) | 8090 |
| Ingestion service | Embedded Tomcat (ingestion module) | 8091 |

Both databases run in Docker (`religioustext-basex`, `religioustext-mysql`).

- [ ] **application.properties missing local import** — `spring.config.import` only has
      `bible-sources.yml`, not `application-local.properties`. Fix:
      Change line in `ingestion/src/main/resources/application.properties`:
      ```
      spring.config.import=optional:classpath:bible-sources.yml,optional:classpath:application-local.properties
      ```
      This causes 401 Unauthorized when triggering ingestion.

- [ ] **Bulk import all local clone translations** — the local clone has 200+ translations across dozens of languages. Future task: iterate all folders in `/Volumes/VMs/data/bible-api-source/bibles/`, auto-generate `bible-sources.yml` entries, and ingest everything. Will need language detection from folder name prefix and a smarter `BOOK_META` fallback for non-Latin scripts.

- [ ] **NIV book names abbreviated** — API.Bible returns short names (e.g. `Lev.` instead
      of `Leviticus`). Need to map API.Bible book IDs to full display names in parser.

- [ ] Schema validation disabled in XmlNormaliser — namespace mismatch on root element.
      Does NOT affect BaseX query performance. Fix when convenient.

- [ ] `globalChronologicalSeq` and `globalNarrativeSeq` not yet populated.

- [ ] Quran ingestion not started — use fawazahmed0/quran-api.

- [ ] Commentary ingestion not started — need open source dataset.

- [ ] **User comments not wired to UI** — MySQL schema written, JPA entities written, needs Spring Security + login + `uuid-creator` dependency added to pom.xml
- [ ] **Add `uuid-creator` dependency** — `com.github.f4b6a3:uuid-creator` needed for `TypedId.java`

- [ ] TextQueryService uses document path without `.xml` suffix — may need updating
      to match BaseX storage format `bible-niv-2011.xml`.

- [ ] App module display mode dropdown applied but not tested since NIV book name issue.

---

## Session 3 Notes (2026-05-27)

### UI
- `ReaderView.java` rewritten with continuous scroll — IntersectionObserver, book-level DOM window (prev+current+next book), chapter selector tracks scroll position, synced columns navigate together
- `AboutView.java` created at `/about` — hero, 6-step how-to, available texts grid, sources & attribution, comments CTA, about section, footer
- Both files written directly to project filesystem
- `ReaderView` toolbar now has About & Help link

### MySQL Schema & Entities
- Schema: `app/src/main/resources/db/migration/V1__initial_schema.sql`
- Tables: `users`, `comments`, `comment_references`, `personal_notes`, `blocked_domains`
- `TypedId.java` utility: `app/src/main/java/org/religioustext/app/util/TypedId.java`
- JPA entities: `User`, `Comment`, `CommentReference`, `PersonalNote`, `BlockedDomain`
  in `app/src/main/java/org/religioustext/app/model/user/`

### Comment System Design
- Private by default; author can make public
- Making public: external refs present → `moderation_status=pending`; no external refs → `approved` immediately
- Approved public comments retractable by author at any time
- Rejected comments revert to private with `rejection_reason` shown to author
- `CommentReference` supports internal (verse in DB) and external (URL) references
- One comment can reference multiple verses across different traditions
- External links open in new tab (`target=_blank rel=noopener noreferrer`)
- Private: no URL checking; public with external URLs: checked against `blocked_domains`

### Typed ID Format
- Format: `{3-char prefix}-{uuid-v7}` e.g. `cmt-018e7b2d-8a1c-7b3f-8d2e-4a5b6c7d8e9f`
- UUID v7 is time-ordered — IDs sort by creation time
- Type readable from first 3 chars; requires `com.github.f4b6a3:uuid-creator` in pom.xml
- Prefixes: `usr-` users, `cmt-` comments, `ref-` references, `nte-` notes, `blk-` blocked domains

---

## Next Session Priorities

1. Add `uuid-creator` to pom.xml and wire Spring Security + Flyway for MySQL
2. Fix NIV abbreviated book names — re-ingest via API.Bible path
3. Start Quran ingestion via fawazahmed0/quran-api
4. Commentary cross-reference links via verse anchors
5. Trigger NASB20 + NBLA from API.Bible
6. Standardise document naming (`.xml` suffix inconsistency)
7. Bulk translation import (200+ languages in local clone)

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


package org.religioustext.ingestion;

import org.religioustext.ingestion.crawler.ApiBibleCrawler;
import org.religioustext.ingestion.model.IngestionRequest;
import org.religioustext.ingestion.parser.BibleApiParser;
import org.religioustext.ingestion.parser.BibleApiParser.ParsedChapter;
import org.religioustext.ingestion.store.xmldb.BaseXStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates the API.Bible ingestion pipeline.
 *
 * Producer/consumer threading:
 *   5 crawler threads fetch book chapters in parallel (network bound)
 *   1 insert thread serialises writes to BaseX (no write conflicts)
 *
 *   CrawlerThread-1 ─┐
 *   CrawlerThread-2 ─┤
 *   CrawlerThread-3 ─┼──► BlockingQueue<CrawledBook> ──► InsertThread ──► BaseX
 *   CrawlerThread-4 ─┤
 *   CrawlerThread-5 ─┘
 *
 * Rate limit: API.Bible allows 5K requests/day.
 * At ~50 chapters/book x 66 books = ~3300 requests per translation.
 * One translation fits comfortably in one day.
 */
@Service
public class IngestionService {

    private static final Logger log             = LoggerFactory.getLogger(IngestionService.class);
    private static final int    MAX_CRAWL_THREADS = 5;
    private static final CrawledBook POISON_PILL = new CrawledBook(null, null, 0, null, null, 0);

    private final ApiBibleCrawler crawler;
    private final BibleApiParser  parser;
    private final BaseXStore      store;

    public IngestionService(
             final ApiBibleCrawler crawler
            , final BibleApiParser  parser
            , final BaseXStore      store) {
        this.crawler = crawler;
        this.parser  = parser;
        this.store   = store;
    }

    @SuppressWarnings("unchecked")
    public String ingest(final IngestionRequest request) throws Exception {
        final String translationId = request.getAbbreviation();
        final String documentId    = buildDocumentId(request);
        final String bibleId       = extractBibleId(request);

        log.info("Starting ingestion: {} -> {} (API.Bible: {})"
            , translationId, documentId, bibleId);
        final long startMs = System.currentTimeMillis();

        // Step 1: Fetch book list
        final List<Map<String, Object>> books = crawler.fetchBooks(bibleId);
        log.info("Found {} books", books.size());

        // Step 2: Create empty root document in BaseX
        if (store.exists(documentId)) {
            log.info("Document exists — deleting for fresh ingestion");
            store.delete(documentId);
        }
        store.createRootDocument(
             documentId
            , request.getSourceType().name().toLowerCase()
            , request.getTranslation()
            , request.getAbbreviation()
            , request.getBcp47Language()
            , request.getIso639_3()
            , request.getDirection()
            , request.getLicense()
            , request.getYear()
            , request.getRegion());

        // Step 3: Producer/consumer pipeline
        final BlockingQueue<CrawledBook> queue =
            new LinkedBlockingQueue<>(MAX_CRAWL_THREADS * 2);

        final AtomicInteger totalVerses  = new AtomicInteger(0);
        final AtomicInteger crawlErrors  = new AtomicInteger(0);
        final AtomicInteger insertErrors = new AtomicInteger(0);

        // ── Insert thread ─────────────────────────────────────────────
        final Thread insertThread = new Thread(() -> {
            MDC.put("translation", translationId);
            MDC.put("book", "INSERT");
            int inserted = 0;
            while (true) {
                try {
                    final CrawledBook crawled = queue.take();
                    if (crawled == POISON_PILL) break;

                    final String bookXml = parser.serialiseBook(
                         null
                        , crawled.bookName()
                        , crawled.bookAltName()
                        , crawled.canonicalOrder()
                        , crawled.testament()
                        , crawled.chapters());

                    store.insertBook(documentId, bookXml);

                    final int verses = crawled.chapters().stream()
                        .mapToInt(c -> c.verses().size()).sum();
                    totalVerses.addAndGet(verses);
                    inserted++;
                    log.info("Inserted [{}/{}] {} ({} verses)"
                        , inserted, books.size(), crawled.bookName(), verses);

                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (final Exception e) {
                    insertErrors.incrementAndGet();
                    log.error("Insert error: {}", e.getMessage(), e);
                }
            }
        }, "basex-insert");
        insertThread.start();

        // ── Crawler thread pool ───────────────────────────────────────
        final ExecutorService crawlPool =
            Executors.newFixedThreadPool(MAX_CRAWL_THREADS);

        // Determine canonical order and testament from book position
        final int[] orderCounter = {0};

        books.forEach(book -> crawlPool.submit(() -> {
            final String bookId   = (String) book.get("id");
            final String bookName = (String) book.get("name");
            final int    order;
            synchronized (orderCounter) { order = ++orderCounter[0]; }
            final String testament = order <= 39 ? "OT" : "NT";

            try {
                MDC.put("translation", translationId);
                MDC.put("book", bookName);
                log.info("Crawling book {}: {}", order, bookName);

                final List<Map<String, Object>> rawChapters =
                    crawler.fetchChaptersForBook(bibleId, book);
                final List<ParsedChapter> chapters = rawChapters.stream()
                    .map(parser::parseChapter)
                    .toList();

                queue.put(new CrawledBook(
                     bookName, null, order, testament, chapters, 0));

            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                crawlErrors.incrementAndGet();
                log.error("Crawl error for {}: {}", bookName, e.getMessage());
            } finally {
                MDC.clear();
            }
        }));

        crawlPool.shutdown();
        crawlPool.awaitTermination(2, TimeUnit.HOURS);
        queue.put(POISON_PILL);
        insertThread.join();

        // Step 4: Update total verse count
        store.setTotalVerses(documentId, totalVerses.get());

        final long elapsed = System.currentTimeMillis() - startMs;
        log.info("Ingestion complete: {} — {} verses, {} crawl errors, {} insert errors, {}ms"
            , documentId, totalVerses.get(), crawlErrors.get(), insertErrors.get(), elapsed);

        return documentId;
    }

    private String buildDocumentId(final IngestionRequest request) {
        return request.getSourceType().name().toLowerCase()
            + "-" + request.getAbbreviation().toLowerCase()
            + (request.getYear() != null ? "-" + request.getYear() : "");
    }

    private String extractBibleId(final IngestionRequest request) {
        // sourceUrl is set to https://rest.api.bible/v1/bibles/{bibleId}
        final String url = request.getSourceUrl();
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private record CrawledBook(
         String            bookName
        , String            bookAltName
        , int               canonicalOrder
        , String            testament
        , List<ParsedChapter> chapters
        , int               unused) {}
}

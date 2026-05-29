package org.religioustext.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.religioustext.app.model.user.Comment;
import org.religioustext.app.model.user.CommentReference;
import org.religioustext.app.model.user.User;
import org.religioustext.app.util.TypedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Seeds pre-populated arguments from transcripts/arguments.json into the database.
 * Runs once at startup; skips if arguments are already seeded.
 */
@Service
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String SYSTEM_USER_ID =
        "usr-00000000-0000-7000-8000-000000000001";

    // Book code mappings from argument JSON to our internal codes
    private static final Map<String, String> SOURCE_MAP = Map.of(
        "bible", null  // source_id null = translation-independent
    );

    @PersistenceContext
    private EntityManager em;

    @Value("${religioustext.transcripts-dir:transcripts}")
    private String transcriptsDir;

    @PostConstruct
    @Transactional
    public void seed() {
        // Check if already seeded
        final Long count = (Long) em.createQuery(
            "SELECT COUNT(c) FROM Comment c WHERE c.user.id = :uid")
            .setParameter("uid", SYSTEM_USER_ID)
            .getSingleResult();

        if (count > 0) {
            log.info("Arguments already seeded ({} comments), skipping.", count);
            return;
        }

        final File jsonFile = new File(transcriptsDir, "arguments.json");
        if (!jsonFile.exists()) {
            log.warn("No arguments.json found at {}. Skipping seed.", jsonFile.getAbsolutePath());
            return;
        }

        final User systemUser = em.find(User.class, SYSTEM_USER_ID);
        if (systemUser == null) {
            log.error("System user not found — run V2 migration first.");
            return;
        }

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final List<Map<String, Object>> entries = mapper.readValue(
                jsonFile, new TypeReference<>() {});

            int seeded = 0;
            for (final Map<String, Object> entry : entries) {
                if (!Boolean.TRUE.equals(entry.get("useful"))) continue;

                final String summary = (String) entry.get("argument_summary");
                final String channel  = (String) entry.get("channel");
                final String tradition = (String) entry.get("tradition");
                final String argType  = (String) entry.get("argument_type");
                if (summary == null || summary.isBlank()) continue;

                // Build comment content with attribution header
                final String content = String.format(
                    "[%s — %s — %s]\n\n%s",
                    channel, tradition, argType, summary);

                final Comment comment = new Comment();
                comment.setUser(systemUser);
                comment.setContent(content);
                em.persist(comment);

                // After persist, comment has an id — now make it public (no external refs)
                comment.makePublic();

                // Add verse references
                @SuppressWarnings("unchecked")
                final List<Map<String, Object>> verseRefs =
                    (List<Map<String, Object>>) entry.get("verse_refs");

                if (verseRefs != null) {
                    int position = 0;
                    for (final Map<String, Object> vr : verseRefs) {
                        final String type = (String) vr.get("type");
                        final CommentReference ref;

                        if ("bible".equals(type)) {
                            final String code    = (String) vr.get("code");
                            final int    chapter = toInt(vr.get("chapter"));
                            final int    verse   = toInt(vr.get("verse"));
                            if (code == null || chapter == 0 || verse == 0) continue;
                            ref = CommentReference.internal(
                                comment, position++, null, code, chapter, verse);
                        } else if ("quran".equals(type)) {
                            final int surah = toInt(vr.get("surah"));
                            final int ayah  = toInt(vr.get("ayah"));
                            if (surah == 0 || ayah == 0) continue;
                            // Store Quran refs with source_id = "quran" as a placeholder
                            // until Quran ingestion is complete
                            ref = CommentReference.internal(
                                comment, position++,
                                "quran", String.format("%03d", surah), surah, ayah);
                        } else {
                            continue;
                        }

                        comment.getReferences().add(ref);
                        em.persist(ref);
                    }
                }

                seeded++;
            }

            log.info("Seeded {} argument comments from arguments.json.", seeded);

        } catch (final Exception e) {
            log.error("Failed to seed arguments: {}", e.getMessage(), e);
        }
    }

    private int toInt(final Object val) {
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        return 0;
    }
}

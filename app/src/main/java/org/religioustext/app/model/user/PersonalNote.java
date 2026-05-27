package org.religioustext.app.model.user;

import jakarta.persistence.*;
import org.religioustext.app.util.TypedId;

import java.time.LocalDateTime;

@Entity
@Table(name = "personal_notes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_note_per_verse",
        columnNames = {"user_id", "source_id", "book_code", "chapter", "verse"}))
public class PersonalNote {

    @Id
    @Column(length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(name = "book_code", nullable = false, length = 10)
    private String bookCode;

    @Column(nullable = false)
    private int chapter;

    @Column(nullable = false)
    private int verse;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = TypedId.generate(TypedId.Type.NOTE);
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String        getId()        { return id; }
    public User          getUser()      { return user; }
    public String        getSourceId()  { return sourceId; }
    public String        getBookCode()  { return bookCode; }
    public int           getChapter()   { return chapter; }
    public int           getVerse()     { return verse; }
    public String        getContent()   { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUser(final User v)       { this.user = v; }
    public void setSourceId(final String v) { this.sourceId = v; }
    public void setBookCode(final String v) { this.bookCode = v; }
    public void setChapter(final int v)     { this.chapter = v; }
    public void setVerse(final int v)       { this.verse = v; }
    public void setContent(final String v)  { this.content = v; }
}

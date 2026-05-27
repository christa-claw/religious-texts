package org.religioustext.app.model.user;

import jakarta.persistence.*;
import org.religioustext.app.util.TypedId;

@Entity
@Table(name = "comment_references")
public class CommentReference {

    public enum RefType { internal, external }

    @Id
    @Column(length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Column(nullable = false)
    private int position = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false)
    private RefType refType;

    // Internal reference fields
    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "book_code", length = 10)
    private String bookCode;

    private Integer chapter;
    private Integer verse;

    // External reference fields
    @Column(length = 2048)
    private String url;

    @Column(length = 255)
    private String label;

    @Column(length = 1000)
    private String description;

    @PrePersist
    void onCreate() {
        if (id == null) id = TypedId.generate(TypedId.Type.COMMENT_REF);
    }

    /** Factory — internal verse reference. */
    public static CommentReference internal(final Comment comment, final int position,
                                             final String sourceId, final String bookCode,
                                             final int chapter, final int verse) {
        final CommentReference ref = new CommentReference();
        ref.comment  = comment;
        ref.position = position;
        ref.refType  = RefType.internal;
        ref.sourceId = sourceId;
        ref.bookCode = bookCode;
        ref.chapter  = chapter;
        ref.verse    = verse;
        return ref;
    }

    /** Factory — external URL reference. */
    public static CommentReference external(final Comment comment, final int position,
                                             final String url, final String label,
                                             final String description) {
        final CommentReference ref = new CommentReference();
        ref.comment     = comment;
        ref.position    = position;
        ref.refType     = RefType.external;
        ref.url         = url;
        ref.label       = label;
        ref.description = description;
        return ref;
    }

    public String  getId()          { return id; }
    public Comment getComment()     { return comment; }
    public int     getPosition()    { return position; }
    public RefType getRefType()     { return refType; }
    public String  getSourceId()    { return sourceId; }
    public String  getBookCode()    { return bookCode; }
    public Integer getChapter()     { return chapter; }
    public Integer getVerse()       { return verse; }
    public String  getUrl()         { return url; }
    public String  getLabel()       { return label; }
    public String  getDescription() { return description; }

    public void setPosition(final int v)       { this.position = v; }
    public void setDescription(final String v) { this.description = v; }
}

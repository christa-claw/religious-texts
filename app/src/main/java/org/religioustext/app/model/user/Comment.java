package org.religioustext.app.model.user;

import jakarta.persistence.*;
import org.religioustext.app.util.TypedId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class Comment {

    public enum ModerationStatus { approved, pending, rejected }

    @Id
    @Column(length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus = ModerationStatus.approved;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CommentReference> references = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (id == null) id = TypedId.generate(TypedId.Type.COMMENT);
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    /**
     * Makes this comment public.
     * If it has external references → pending (needs admin review).
     * If no external references → approved immediately.
     */
    public void makePublic() {
        this.isPublic = true;
        final boolean hasExternalRefs = references.stream()
            .anyMatch(r -> r.getRefType() == CommentReference.RefType.external);
        this.moderationStatus = hasExternalRefs
            ? ModerationStatus.pending
            : ModerationStatus.approved;
        this.rejectionReason = null;
    }

    /** Retracts a public comment — removes from public view immediately. */
    public void makePrivate() {
        this.isPublic = false;
        this.moderationStatus = ModerationStatus.approved;
        this.rejectionReason = null;
    }

    /** Admin approves a pending comment. */
    public void approve() {
        this.moderationStatus = ModerationStatus.approved;
        this.rejectionReason = null;
    }

    /** Admin rejects a pending comment with a reason. */
    public void reject(final String reason) {
        this.moderationStatus = ModerationStatus.rejected;
        this.rejectionReason = reason;
        this.isPublic = false;
    }

    /** Whether this comment is visible to the general public. */
    public boolean isVisiblePublicly() {
        return isPublic && moderationStatus == ModerationStatus.approved;
    }

    public String               getId()               { return id; }
    public User                 getUser()             { return user; }
    public String               getContent()          { return content; }
    public boolean              isPublic()            { return isPublic; }
    public ModerationStatus     getModerationStatus() { return moderationStatus; }
    public String               getRejectionReason()  { return rejectionReason; }
    public LocalDateTime        getCreatedAt()        { return createdAt; }
    public LocalDateTime        getUpdatedAt()        { return updatedAt; }
    public List<CommentReference> getReferences()     { return references; }

    public void setUser(final User v)      { this.user = v; }
    public void setContent(final String v) { this.content = v; }
}

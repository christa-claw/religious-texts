package org.religioustext.app.model.user;

import jakarta.persistence.*;
import org.religioustext.app.util.TypedId;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 40)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @PrePersist
    void onCreate() {
        if (id == null) id = TypedId.generate(TypedId.Type.USER);
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String        getId()           { return id; }
    public String        getEmail()        { return email; }
    public String        getPasswordHash() { return passwordHash; }
    public String        getDisplayName()  { return displayName; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getUpdatedAt()    { return updatedAt; }
    public boolean       isVerified()      { return verified; }
    public boolean       isAdmin()         { return admin; }
    public boolean       isActive()        { return active; }

    public void setEmail(final String v)        { this.email = v; }
    public void setPasswordHash(final String v) { this.passwordHash = v; }
    public void setDisplayName(final String v)  { this.displayName = v; }
    public void setVerified(final boolean v)    { this.verified = v; }
    public void setAdmin(final boolean v)       { this.admin = v; }
    public void setActive(final boolean v)      { this.active = v; }
}

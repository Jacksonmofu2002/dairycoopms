package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps to: dairycoopms.users.
 *
 * NO Lombok – all methods explicit so login always works.
 *
 * member field: only set when role = MEMBER.
 *   - Staff accounts (ADMIN, COLLECTION_OFFICER, etc.) have member = null
 *   - Member accounts have member pointing to their Member record
 */
@Entity
@Table(name = "users", schema = "dairycoopms")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role roleEntity;

    /**
     * Links this login account to a cooperative Member record.
     * Only populated when role = MEMBER.
     * Staff accounts leave this null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "basic_salary", precision = 14, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "signature_data", columnDefinition = "LONGTEXT")
    private String signatureData;

    public String getSignatureData() { return signatureData; }
    public void setSignatureData(String signatureData) { this.signatureData = signatureData; }

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    public User(UUID id, String email, String passwordHash, String fullName,
                Role roleEntity, Member member, boolean active, boolean mustChangePassword,
                BigDecimal basicSalary,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id                 = id;
        this.email              = email;
        this.passwordHash       = passwordHash;
        this.fullName           = fullName;
        this.roleEntity         = roleEntity;
        this.member             = member;
        this.active             = active;
        this.mustChangePassword = mustChangePassword;
        this.basicSalary        = basicSalary;
        this.createdAt          = createdAt;
        this.updatedAt          = updatedAt;
    }

    // ── JPA lifecycle ─────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId()                      { return id; }
    public String getEmail()                 { return email; }
    public String getPasswordHash()          { return passwordHash; }
    public String getFullName()              { return fullName; }
    public Role getRoleEntity()              { return roleEntity; }
    public Member getMember()                { return member; }
    public boolean isActive()                { return active; }
    public boolean isMustChangePassword()    { return mustChangePassword; }
    public BigDecimal getBasicSalary()       { return basicSalary; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public LocalDateTime getUpdatedAt()      { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(UUID id)                        { this.id = id; }
    public void setEmail(String email)                { this.email = email; }
    public void setPasswordHash(String ph)            { this.passwordHash = ph; }
    public void setFullName(String fn)                { this.fullName = fn; }
    public void setRoleEntity(Role r)                 { this.roleEntity = r; }
    public void setMember(Member m)                   { this.member = m; }
    public void setActive(boolean active)             { this.active = active; }
    public void setMustChangePassword(boolean v)      { this.mustChangePassword = v; }
    public void setBasicSalary(BigDecimal salary)     { this.basicSalary = salary; }
    public void setCreatedAt(LocalDateTime t)         { this.createdAt = t; }
    public void setUpdatedAt(LocalDateTime t)         { this.updatedAt = t; }

    // ── Business methods ──────────────────────────────────────────────────────

    /** Role NAME e.g. "ADMIN", "MEMBER". Used on every login. */
    public String getRole() {
        return roleEntity != null ? roleEntity.getName() : null;
    }

    /** True if this is a cooperative member account (not staff). */
    public boolean isMemberAccount() {
        return "MEMBER".equals(getRole());
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String email, passwordHash, fullName;
        private Role roleEntity;
        private Member member;
        private boolean active = true;
        private boolean mustChangePassword = false;
        private BigDecimal basicSalary;
        private LocalDateTime createdAt, updatedAt;

        public Builder id(UUID v)                    { this.id = v; return this; }
        public Builder email(String v)               { this.email = v; return this; }
        public Builder passwordHash(String v)        { this.passwordHash = v; return this; }
        public Builder fullName(String v)            { this.fullName = v; return this; }
        public Builder roleEntity(Role v)            { this.roleEntity = v; return this; }
        public Builder member(Member v)              { this.member = v; return this; }
        public Builder active(boolean v)             { this.active = v; return this; }
        public Builder mustChangePassword(boolean v) { this.mustChangePassword = v; return this; }
        public Builder basicSalary(BigDecimal v)     { this.basicSalary = v; return this; }
        public Builder createdAt(LocalDateTime v)    { this.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v)    { this.updatedAt = v; return this; }

        public User build() {
            return new User(id, email, passwordHash, fullName,
                    roleEntity, member, active, mustChangePassword,
                    basicSalary, createdAt, updatedAt);
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id != null && id.equals(other.id);
    }
    @Override public int hashCode() { return id != null ? id.hashCode() : 0; }
    @Override public String toString() {
        return "User{email='" + email + "', role=" + getRole() + ", active=" + active + "}";
    }
}
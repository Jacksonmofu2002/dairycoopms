package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to: dairycoopms.roles
 *
 * NO Lombok – getName() is written explicitly.
 * User.getRole() calls roleEntity.getName() on every login.
 * It must always exist regardless of annotation processing.
 */
@Entity
@Table(name = "roles", schema = "dairycoopms")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Role() {}

    public Role(Integer id, String name, String description, LocalDateTime createdAt) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.createdAt   = createdAt;
    }

    // ── JPA lifecycle ─────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Integer getId()              { return id; }
    public String getName()             { return name; }
    public String getDescription()      { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(Integer id)                  { this.id = id; }
    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private String name;
        private String description;
        private LocalDateTime createdAt;

        public Builder id(Integer id)                  { this.id = id; return this; }
        public Builder name(String name)               { this.name = name; return this; }
        public Builder description(String d)           { this.description = d; return this; }
        public Builder createdAt(LocalDateTime t)      { this.createdAt = t; return this; }

        public Role build() {
            return new Role(id, name, description, createdAt);
        }
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role other = (Role) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Role{id=" + id + ", name='" + name + "'}";
    }
}
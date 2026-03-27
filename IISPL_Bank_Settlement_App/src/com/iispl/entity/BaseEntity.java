package com.iispl.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.iispl.intefaces.Auditable;

/**
 * Abstract base class for all JPA/JDBC entities.
 * Every domain class extends this — it provides audit fields,
 * optimistic locking (version), and Serializable support.
 *
 * All other members depend on this class.
 */
public abstract class BaseEntity implements Auditable, Serializable {

    private static final long serialVersionUID = 1L;

    protected Long id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected String createdBy;
    protected int version; // optimistic lock — increment on every UPDATE

    // ── Constructors ──────────────────────────────────────────────────────────

    protected BaseEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    protected BaseEntity(String createdBy) {
        this();
        this.createdBy = createdBy;
    }

    // ── Auditable implementation ──────────────────────────────────────────────

    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String getCreatedBy() { return createdBy; }

    @Override
    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", version=" + version + "}";
    }
}
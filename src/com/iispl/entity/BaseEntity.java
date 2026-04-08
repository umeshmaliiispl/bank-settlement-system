package com.iispl.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import com.iispl.intefaces.Auditable;

/**
 * BaseEntity — Abstract superclass for ALL domain classes in the settlement
 * system.
 *
 * Provides: - id : unique surrogate key (AtomicLong generator — DB will replace
 * in prod) - createdAt : timestamp when record was first created - updatedAt :
 * timestamp of last modification (auto-updated via markUpdated()) - createdBy :
 * user/service that created the record (audit trail) - version : optimistic
 * lock counter (increments on every update)
 *
 * DESIGN PATTERN: Template Method — all concrete entity toString() calls super
 * fields, then appends their own.
 *
 * IS-A: implements Auditable, Serializable
 */
public abstract class BaseEntity implements Auditable, Serializable {

	private static final long serialVersionUID = 1L;

	// Thread-safe ID generator 
	private static final AtomicLong ID_SEQ = new AtomicLong(1000L);

	protected Long id;
	protected LocalDateTime createdAt;
	protected LocalDateTime updatedAt;
	protected String createdBy;
	protected int version; 
	
	protected BaseEntity() {
		this.id = ID_SEQ.getAndIncrement();
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
		this.version = 0;
	}

	protected BaseEntity(String createdBy) {
		this();
		this.createdBy = createdBy;
	}

	// Auditable implementation 
	@Override
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	@Override
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public String getCreatedBy() {
		return createdBy;
	}

	@Override
	public void markUpdated() {
		this.updatedAt = LocalDateTime.now();
		this.version++;
	}


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setCreatedAt(LocalDateTime v) {
		this.createdAt = v;
	}

	public void setUpdatedAt(LocalDateTime v) {
		this.updatedAt = v;
	}

	public void setCreatedBy(String v) {
		this.createdBy = v;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int v) {
		this.version = v;
	}

	// Object overrides 
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BaseEntity that = (BaseEntity) o;
		return id != null && id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id=" + id + ", v=" + version + ", createdBy=" + createdBy + "}";
	}
}
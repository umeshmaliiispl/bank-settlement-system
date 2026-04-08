package com.iispl.utility;

import java.time.LocalDateTime;

import com.iispl.enums.AuditAction;

/**
 * AuditLog — Immutable audit trail for every entity change.
 *
 * IMPORTANT RULES: - INSERT ONLY — this record is NEVER updated or deleted -
 * oldValue and newValue store JSON snapshots of entity state - Every CREATE,
 * UPDATE, APPROVE, REVERSE on any entity must produce one AuditLog entry
 *
 * WHO CREATES IT: - All team members call AuditLog when they change any entity
 * - Umesh owns this class — others just use it
 *
 * DB TABLE: audit_log
 */
public class AuditLog {

	private Long logId;
	private String entityType; // "IncomingTransaction", "Account", "SettlementBatch" etc.
	private Long entityId; // PK of the changed entity
	private AuditAction action; // CREATE / UPDATE / APPROVE / REVERSE / DELETE etc.
	private String oldValue; // JSON snapshot BEFORE change (null for CREATE)
	private String newValue; // JSON snapshot AFTER change (null for DELETE)
	private String changedBy; // username / system name who made the change
	private LocalDateTime changedAt; // exact timestamp
	private String ipAddress; // IP of the caller (optional)

	public AuditLog() {
		this.changedAt = LocalDateTime.now();
	}

	/**
	 * Convenience constructor — most common use case.
	 */
	public AuditLog(String entityType, Long entityId, AuditAction action, String oldValue, String newValue,
			String changedBy) {
		this();
		this.entityType = entityType;
		this.entityId = entityId;
		this.action = action;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.changedBy = changedBy;
	}

	// ── Static factory helpers ────────────────────────────────────────────────

	/** Quick factory for a CREATE action */
	public static AuditLog forCreate(String entityType, Long entityId, String newValue, String changedBy) {
		return new AuditLog(entityType, entityId, AuditAction.CREATE, null, newValue, changedBy);
	}

	/** Quick factory for an UPDATE action */
	public static AuditLog forUpdate(String entityType, Long entityId, String oldValue, String newValue,
			String changedBy) {
		return new AuditLog(entityType, entityId, AuditAction.UPDATE, oldValue, newValue, changedBy);
	}

	/** Quick factory for an APPROVE action */
	public static AuditLog forApprove(String entityType, Long entityId, String changedBy) {
		return new AuditLog(entityType, entityId, AuditAction.APPROVE, null, null, changedBy);
	}

	/** Quick factory for a REVERSE action */
	public static AuditLog forReverse(String entityType, Long entityId, String oldValue, String changedBy) {
		return new AuditLog(entityType, entityId, AuditAction.REVERSE, oldValue, null, changedBy);
	}

	// ── Getters & Setters ─────────────────────────────────────────────────────

	public Long getLogId() {
		return logId;
	}

	public void setLogId(Long logId) {
		this.logId = logId;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public AuditAction getAction() {
		return action;
	}

	public void setAction(AuditAction action) {
		this.action = action;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public String getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}

	public void setChangedAt(LocalDateTime changedAt) {
		this.changedAt = changedAt;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	// ── toString ──────────────────────────────────────────────────────────────

	@Override
	public String toString() {
		return "AuditLog{" + "logId=" + logId + ", entity=" + entityType + "#" + entityId + ", action=" + action
				+ ", changedBy=" + changedBy + ", changedAt=" + changedAt + "}";
	}
}

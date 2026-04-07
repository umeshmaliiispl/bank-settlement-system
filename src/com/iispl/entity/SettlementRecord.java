package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.SettlementStatus;

public class SettlementRecord extends BaseEntity {

	private String batchId;
	private long incomingTxnId;
	private BigDecimal settledAmount = BigDecimal.ZERO;
	private LocalDateTime settledDate;
	private SettlementStatus settledStatus;
	private String failureReason;

	public SettlementRecord() {
		this.settledDate = LocalDateTime.now();
		this.settledStatus = SettlementStatus.PENDING;
	}

	public SettlementRecord(String batchId, long incomingTxnId, BigDecimal settledAmount, SettlementStatus status) {

		this.batchId = batchId;
		this.incomingTxnId = incomingTxnId;
		this.settledAmount = settledAmount;
		this.settledDate = LocalDateTime.now();
		this.settledStatus = status;
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public long getIncomingTxnId() {
		return incomingTxnId;
	}

	public void setIncomingTxnId(long incomingTxnId) {
		this.incomingTxnId = incomingTxnId;
	}

	public BigDecimal getSettledAmount() {
		return settledAmount;
	}

	public void setSettledAmount(BigDecimal settledAmount) {
		this.settledAmount = settledAmount;
	}

	public LocalDateTime getSettledDate() {
		return settledDate;
	}

	public void setSettledDate(LocalDateTime settledDate) {
		this.settledDate = settledDate;
	}

	public SettlementStatus getSettledStatus() {
		return settledStatus;
	}

	public void setSettledStatus(SettlementStatus settledStatus) {
		this.settledStatus = settledStatus;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	public void markFailed(String reason) {
		this.settledStatus = SettlementStatus.FAILED;
		this.failureReason = reason;
	}
}
package com.iispl.entity;

import java.time.LocalDateTime;
import com.iispl.enums.SettlementStatus;

public class SettlementRecord extends BaseEntity {

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

	public double getSettledAmount() {
		return settledAmount;
	}

	public void setSettledAmount(double settledAmount) {
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

	public SettlementRecord(String batchId, long incomingTxnId, double settledAmount, LocalDateTime settledDate,
			SettlementStatus settledStatus, String failureReason) {
		super();
		this.batchId = batchId;
		this.incomingTxnId = incomingTxnId;
		this.settledAmount = settledAmount;
		this.settledDate = settledDate;
		this.settledStatus = settledStatus;
		this.failureReason = failureReason;
	}

	private String batchId;
	private long incomingTxnId;
	private double settledAmount;
	private LocalDateTime settledDate;
	private SettlementStatus settledStatus;
	private String failureReason;

}
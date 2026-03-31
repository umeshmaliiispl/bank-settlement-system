package com.iispl.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.iispl.enums.BatchStatus;

public class SettlementBatch extends BaseEntity {

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public LocalDate getBatchDate() {
		return batchDate;
	}

	public void setBatchDate(LocalDate batchDate) {
		this.batchDate = batchDate;
	}

	public BatchStatus getBatchStatus() {
		return batchStatus;
	}

	public void setBatchStatus(BatchStatus batchStatus) {
		this.batchStatus = batchStatus;
	}

	public int getTotalTransactions() {
		return totalTransactions;
	}

	public void setTotalTransactions(int totalTransactions) {
		this.totalTransactions = totalTransactions;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public List<SettlementRecord> getRecords() {
		return records;
	}

	public void setRecords(List<SettlementRecord> records) {
		this.records = records;
	}

	public String getRunBy() {
		return runBy;
	}

	public void setRunBy(String runBy) {
		this.runBy = runBy;
	}

	public LocalDateTime getRunAt() {
		return runAt;
	}

	public void setRunAt(LocalDateTime runAt) {
		this.runAt = runAt;
	}

	public SettlementBatch(String batchId, LocalDate batchDate, BatchStatus batchStatus, int totalTransactions,
			double totalAmount, List<SettlementRecord> records, String runBy, LocalDateTime runAt) {
		super();
		this.batchId = batchId;
		this.batchDate = batchDate;
		this.batchStatus = batchStatus;
		this.totalTransactions = totalTransactions;
		this.totalAmount = totalAmount;
		this.records = records;
		this.runBy = runBy;
		this.runAt = runAt;
	}

	private String batchId;
	private LocalDate batchDate;
	private BatchStatus batchStatus;
	private int totalTransactions;
	private double totalAmount;

	private List<SettlementRecord> records; // HAS-A

	private String runBy;
	private LocalDateTime runAt;

}
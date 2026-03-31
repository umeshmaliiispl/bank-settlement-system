package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.iispl.enums.BatchStatus;

public class SettlementBatch extends BaseEntity {

	private String batchId;
	private LocalDate batchDate;
	private BatchStatus batchStatus;

	private int totalTransactions;
	private BigDecimal totalAmount = BigDecimal.ZERO;

	private List<SettlementRecord> records = new ArrayList<>();

	private String runBy;
	private LocalDateTime runAt;

	// ✅ DEFAULT CONSTRUCTOR
	public SettlementBatch() {
		this.batchDate = LocalDate.now();
		this.batchStatus = BatchStatus.SCHEDULED;
		this.runAt = LocalDateTime.now();
	}

	// ✅ CUSTOM CONSTRUCTOR
	public SettlementBatch(String batchId, String runBy) {
		this();
		this.batchId = batchId;
		this.runBy = runBy;
	}

	// ✅ CORE BUSINESS METHOD (VERY IMPORTANT)
	public void addRecord(SettlementRecord record) {
		this.records.add(record);
		this.totalTransactions++;
		this.totalAmount = this.totalAmount.add(record.getSettledAmount());
	}

	// ─────────────────────────────
	// GETTERS & SETTERS
	// ─────────────────────────────

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

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
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
}

//	package com.iispl.entity;
//	
//	import java.time.LocalDate;
//	import java.time.LocalDateTime;
//	import java.util.List;
//	import com.iispl.enums.BatchStatus;
//	
//	public class SettlementBatch extends BaseEntity {
//	
//		public String getBatchId() {
//			return batchId;
//		}
//	
//		public void setBatchId(String batchId) {
//			this.batchId = batchId;
//		}
//	
//		public LocalDate getBatchDate() {
//			return batchDate;
//		}
//	
//		public void setBatchDate(LocalDate batchDate) {
//			this.batchDate = batchDate;
//		}
//	
//		public BatchStatus getBatchStatus() {
//			return batchStatus;
//		}
//	
//		public void setBatchStatus(BatchStatus batchStatus) {
//			this.batchStatus = batchStatus;
//		}
//	
//		public int getTotalTransactions() {
//			return totalTransactions;
//		}
//	
//		public void setTotalTransactions(int totalTransactions) {
//			this.totalTransactions = totalTransactions;
//		}
//	
//		public double getTotalAmount() {
//			return totalAmount;
//		}
//	
//		public void setTotalAmount(double totalAmount) {
//			this.totalAmount = totalAmount;
//		}
//	
//		public List<SettlementRecord> getRecords() {
//			return records;
//		}
//	
//		public void setRecords(List<SettlementRecord> records) {
//			this.records = records;
//		}
//	
//		public String getRunBy() {
//			return runBy;
//		}
//	
//		public void setRunBy(String runBy) {
//			this.runBy = runBy;
//		}
//	
//		public LocalDateTime getRunAt() {
//			return runAt;
//		}
//	
//		public void setRunAt(LocalDateTime runAt) {
//			this.runAt = runAt;
//		}
//	
//		public SettlementBatch(String batchId, LocalDate batchDate, BatchStatus batchStatus, int totalTransactions,
//				double totalAmount, List<SettlementRecord> records, String runBy, LocalDateTime runAt) {
//			super();
//			this.batchId = batchId;
//			this.batchDate = batchDate;
//			this.batchStatus = batchStatus;
//			this.totalTransactions = totalTransactions;
//			this.totalAmount = totalAmount;
//			this.records = records;
//			this.runBy = runBy;
//			this.runAt = runAt;
//		}
//	
//		private String batchId;
//		private LocalDate batchDate;
//		private BatchStatus batchStatus;
//		private int totalTransactions;
//		private double totalAmount;
//	
//		private List<SettlementRecord> records; // HAS-A
//	
//		private String runBy;
//		private LocalDateTime runAt;
//	
//	}
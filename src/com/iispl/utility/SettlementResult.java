package com.iispl.utility;

import com.iispl.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SettlementResult — Return value of SettlementProcessor (Callable).
 *
 * When Prakash's SettlementProcessor finishes processing a batch, it returns a
 * SettlementResult wrapped in a Future/CompletableFuture.
 *
 * The ZK ViewModel (or any caller) reads this object to know: - How many
 * transactions settled successfully - How many failed - Total settled amount -
 * List of failure reasons for failed transactions
 *
 * FLOW: SettlementProcessor (Callable<SettlementResult>) │ └── returns
 * SettlementResult ↓ CompletableFuture.get() ↓ UI / IntegrationTest reads the
 * result
 *
 * OWNED BY: Umesh (Team Lead) USED BY: Prakash (SettlementProcessor returns
 * this) Umesh (IntegrationTest reads this to verify pipeline)
 */
public class SettlementResult {

	private String batchId;
	private SettlementStatus status;
	private int settledCount;
	private int failedCount;
	private int totalCount;
	private BigDecimal totalSettledAmount;
	private List<String> failureReasons;
	private LocalDateTime processedAt;
	private long processingTimeMs; // how long the batch took in milliseconds

	public SettlementResult() {
		this.processedAt = LocalDateTime.now();
		this.failureReasons = new ArrayList<>();
		this.totalSettledAmount = BigDecimal.ZERO;
	}

	public SettlementResult(String batchId) {
		this();
		this.batchId = batchId;
		this.status = SettlementStatus.IN_PROGRESS;
	}

	// ── Business logic ────────────────────────────────────────────────────────

	/**
	 * Add a failure reason (called once per failed transaction).
	 * 
	 * @param reason short description, e.g. "TXN-123: Insufficient funds"
	 */
	public void addFailureReason(String reason) {
		this.failureReasons.add(reason);
		this.failedCount++;
	}

	/**
	 * Mark one transaction as successfully settled.
	 * 
	 * @param amount the settled amount to add to running total
	 */
	public void recordSuccess(BigDecimal amount) {
		this.settledCount++;
		this.totalSettledAmount = this.totalSettledAmount.add(amount);
	}

	/**
	 * Finalize the result — computes final status and totalCount. Call this ONCE
	 * after all transactions in the batch are processed.
	 */
	public void finalise() {
		this.totalCount = settledCount + failedCount;
		this.processedAt = LocalDateTime.now();

		if (failedCount == 0) {
			this.status = SettlementStatus.SETTLED;
		} else if (settledCount == 0) {
			this.status = SettlementStatus.FAILED;
		} else {
			this.status = SettlementStatus.PARTIALLY_SETTLED;
		}
	}

	/**
	 * Returns true if the entire batch settled without any failures.
	 */
	public boolean isFullySettled() {
		return SettlementStatus.SETTLED.equals(this.status) && failedCount == 0;
	}

	/**
	 * Returns success rate as a percentage (0.0 to 100.0). Example: 95 transactions
	 * settled out of 100 → 95.0
	 */
	public double getSuccessRate() {
		if (totalCount == 0)
			return 0.0;
		return (settledCount * 100.0) / totalCount;
	}

	/**
	 * Returns an unmodifiable view of failure reasons.
	 */
	public List<String> getFailureReasons() {
		return Collections.unmodifiableList(failureReasons);
	}

	// ── Getters & Setters ─────────────────────────────────────────────────────

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	public SettlementStatus getStatus() {
		return status;
	}

	public void setStatus(SettlementStatus status) {
		this.status = status;
	}

	public int getSettledCount() {
		return settledCount;
	}

	public void setSettledCount(int settledCount) {
		this.settledCount = settledCount;
	}

	public int getFailedCount() {
		return failedCount;
	}

	public void setFailedCount(int failedCount) {
		this.failedCount = failedCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public BigDecimal getTotalSettledAmount() {
		return totalSettledAmount;
	}

	public void setTotalSettledAmount(BigDecimal totalSettledAmount) {
		this.totalSettledAmount = totalSettledAmount;
	}

	public LocalDateTime getProcessedAt() {
		return processedAt;
	}

	public void setProcessedAt(LocalDateTime processedAt) {
		this.processedAt = processedAt;
	}

	public long getProcessingTimeMs() {
		return processingTimeMs;
	}

	public void setProcessingTimeMs(long processingTimeMs) {
		this.processingTimeMs = processingTimeMs;
	}

	// ── toString ──────────────────────────────────────────────────────────────

	@Override
	public String toString() {
		return "SettlementResult{" + "batchId=" + batchId + ", status=" + status + ", settled=" + settledCount
				+ ", failed=" + failedCount + ", total=" + totalCount + ", amount=" + totalSettledAmount
				+ ", successRate=" + String.format("%.1f", getSuccessRate()) + "%" + ", processedAt=" + processedAt
				+ "}";
	}
}

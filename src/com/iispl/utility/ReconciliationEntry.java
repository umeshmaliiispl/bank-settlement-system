package com.iispl.utility;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.iispl.enums.ReconciliationStatus;

/**
 * ReconciliationEntry — Compares expected vs actual settlement for an account.
 *
 * After every settlement batch runs, the reconciliation engine:
 *   1. Reads what SHOULD have settled  (expectedAmount)
 *   2. Reads what ACTUALLY settled     (actualAmount)
 *   3. Computes variance = actual - expected
 *   4. Flags status: MATCHED / MISMATCH / INVESTIGATING / RESOLVED
 *
 * variance = 0           → MATCHED      (all good)
 * variance != 0          → MISMATCH     (needs investigation)
 * under investigation    → INVESTIGATING
 * root cause found+fixed → RESOLVED
 *
 * OWNED BY: Umesh (Team Lead)
 * USED BY:  Nayeem (settlement engine creates entries after each batch)
 *
 * DB TABLE: reconciliation_entry
 */
public class ReconciliationEntry {

    private Long entryId;
    private LocalDate reconciliationDate;
    private Long accountId;               // FK → account.id
    private String batchId;               // which batch this reconciliation covers
    private BigDecimal expectedAmount;    // what the system expected to settle
    private BigDecimal actualAmount;      // what was actually settled in DB
    private BigDecimal variance;          // actualAmount - expectedAmount (auto-computed)
    private ReconciliationStatus reconStatus;
    private String remarks;               // free-text notes for investigation
    private String reconciledBy;          // user/system who ran the reconciliation
    private LocalDateTime reconciledAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ReconciliationEntry() {
        this.reconciliationDate = LocalDate.now();
        this.reconciledAt       = LocalDateTime.now();
        this.reconStatus        = ReconciliationStatus.PENDING;
    }

    public ReconciliationEntry(Long accountId, String batchId,
                                BigDecimal expectedAmount, BigDecimal actualAmount,
                                String reconciledBy) {
        this();
        this.accountId      = accountId;
        this.batchId        = batchId;
        this.expectedAmount = expectedAmount;
        this.actualAmount   = actualAmount;
        this.reconciledBy   = reconciledBy;
        computeVariance();
    }

    // ── Business logic ────────────────────────────────────────────────────────

    /**
     * Computes variance = actualAmount - expectedAmount.
     * Also auto-sets status:
     *   variance == 0  → MATCHED
     *   variance != 0  → MISMATCH
     */
    public void computeVariance() {
        if (expectedAmount != null && actualAmount != null) {
            this.variance = actualAmount.subtract(expectedAmount);
            if (this.variance.compareTo(BigDecimal.ZERO) == 0) {
                this.reconStatus = ReconciliationStatus.MATCHED;
            } else {
                this.reconStatus = ReconciliationStatus.MISMATCH;
            }
        }
    }

    /**
     * Returns true if this entry is fully reconciled (no variance).
     */
    public boolean isMatched() {
        return ReconciliationStatus.MATCHED.equals(this.reconStatus);
    }

    /**
     * Returns true if this entry needs investigation.
     */
    public boolean requiresInvestigation() {
        return ReconciliationStatus.MISMATCH.equals(this.reconStatus)
            || ReconciliationStatus.INVESTIGATING.equals(this.reconStatus);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public LocalDate getReconciliationDate() { return reconciliationDate; }
    public void setReconciliationDate(LocalDate reconciliationDate) { this.reconciliationDate = reconciliationDate; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
        computeVariance();
    }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
        computeVariance();
    }

    public BigDecimal getVariance() { return variance; }

    public ReconciliationStatus getReconStatus() { return reconStatus; }
    public void setReconStatus(ReconciliationStatus reconStatus) { this.reconStatus = reconStatus; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getReconciledBy() { return reconciledBy; }
    public void setReconciledBy(String reconciledBy) { this.reconciledBy = reconciledBy; }

    public LocalDateTime getReconciledAt() { return reconciledAt; }
    public void setReconciledAt(LocalDateTime reconciledAt) { this.reconciledAt = reconciledAt; }

    // ── toString ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ReconciliationEntry{"
             + "entryId=" + entryId
             + ", accountId=" + accountId
             + ", batchId=" + batchId
             + ", expected=" + expectedAmount
             + ", actual=" + actualAmount
             + ", variance=" + variance
             + ", status=" + reconStatus
             + "}";
    }
}

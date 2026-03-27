package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

/**
 * Canonical IncomingTransaction — normalised form of EVERY transaction
 * regardless of source system. Produced by TransactionAdapter implementations.
 *
 * Placed onto BlockingQueue<IncomingTransaction> for downstream settlement.
 *
 * OWNED BY: Team Lead UM (stub) — Member 4 fills adapter logic on Day 1.
 */
public class IncomingTransaction extends BaseEntity {

    private SourceSystem sourceSystem;          // HAS-A
    private String sourceRef;
    private String rawPayload;
    private String normalizedPayload;
    private TransactionType txnType;
    private BigDecimal amount;
    private String currency;
    private LocalDate valueDate;
    private ProcessingStatus processingStatus;
    private LocalDateTime ingestTimestamp;

    public IncomingTransaction() {
        super();
        this.processingStatus = ProcessingStatus.RECEIVED;
        this.ingestTimestamp = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public SourceSystem getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(SourceSystem sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public String getNormalizedPayload() { return normalizedPayload; }
    public void setNormalizedPayload(String normalizedPayload) { this.normalizedPayload = normalizedPayload; }

    public TransactionType getTxnType() { return txnType; }
    public void setTxnType(TransactionType txnType) { this.txnType = txnType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }

    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }

    public LocalDateTime getIngestTimestamp() { return ingestTimestamp; }
    public void setIngestTimestamp(LocalDateTime ingestTimestamp) { this.ingestTimestamp = ingestTimestamp; }

    @Override
    public String toString() {
        return "IncomingTransaction{id=" + id + ", sourceRef=" + sourceRef
                + ", amount=" + amount + ", currency=" + currency
                + ", status=" + processingStatus + "}";
    }
}
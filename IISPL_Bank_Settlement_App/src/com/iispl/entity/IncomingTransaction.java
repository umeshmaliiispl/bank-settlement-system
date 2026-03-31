package com.iispl.entity;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IncomingTransaction — The canonical POJO of the entire ingestion pipeline.
 *
 * EVERY transaction from EVERY source system (CBS, RTGS, SWIFT, NEFT, UPI,
 * Fintech) is normalised into this single POJO by its TransactionAdapter before
 * being placed onto the BlockingQueue for downstream settlement.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * FIELD GROUPS
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * [A] SOURCE IDENTIFICATION sourceSystem — HAS-A SourceSystem (which system
 * sent this) channelCode — "CBS" / "RTGS" / "SWIFT" / "NEFT" / "UPI" /
 * "FINTECH" sourceRef — source-system-assigned unique reference
 * (UTR/TRN/CBS-REF) rawPayload — original wire string exactly as received
 * normalizedPayload — JSON snapshot after parsing (for audit/reprocessing)
 * checksum — SHA-256 of rawPayload (duplicate detection)
 *
 * [B] TRANSACTION CORE txnType — CREDIT / DEBIT / REVERSAL / SWAP / FEE /
 * INTRABANK amount — settlement amount (net for Fintech; gross for all others)
 * grossAmount — pre-fee amount (Fintech only; equals amount for others)
 * feeAmount — partner fee deducted (Fintech only; 0 for others) currency — ISO
 * 4217: INR / USD / GBP / EUR / AED … valueDate — settlement / value date
 * ingestTimestamp — exact moment this system received the payload
 *
 * [C] SENDER DETAILS senderIfsc — RBI IFSC code of sending bank ("SBIN0001234")
 * senderBankName — resolved from IFSC ("State Bank of India") senderAccount —
 * account number of sender (CBS) senderName — customer / corporate name of
 * sender (SWIFT :50K:) senderVpa — UPI Virtual Payment Address ("rahul@okaxis")
 * senderBic — SWIFT BIC of sending bank ("SBININBB")
 *
 * [D] RECEIVER DETAILS receiverIfsc — RBI IFSC of receiving bank
 * ("HDFC0005678") receiverBankName — resolved from IFSC ("HDFC Bank")
 * receiverAccount — beneficiary account number receiverName — beneficiary name
 * (SWIFT :59:) receiverVpa — UPI VPA of beneficiary ("priya@ybl") receiverBic —
 * SWIFT BIC of receiving bank ("HDFCINBB")
 *
 * [E] PAYMENT METADATA remarks — payment narration / remittance info (SWIFT
 * :70:) narration — internal bank narration chargeType — SWIFT :71A: — OUR /
 * BEN / SHA (who bears charges) purposeCode — RBI purpose code (P0001 = trade,
 * etc.) schemeCode — payment scheme identifier
 *
 * [F] FINTECH-SPECIFIC partnerName — "Razorpay" / "Paytm" / "PhonePe" /
 * "Cashfree" merchantId — merchant identifier from Fintech platform
 *
 * [G] PROCESSING PIPELINE processingStatus —
 * RECEIVED→VALIDATED→QUEUED→PROCESSING→PROCESSED priority — 1=RTGS(HIGH),
 * 3=SWIFT, 5=CBS/NEFT, 7=UPI/FINTECH(LOW) retryCount — how many times
 * reprocessed after failure errorMessage — last error description if FAILED or
 * DEAD_LETTER batchId — FK to SettlementBatch once assigned
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * DESIGN NOTE: HAS-A SourceSystem (composition — not just an ID, but the full
 * object, so adapters can call sourceSystem.recordSuccess() / recordFailure()).
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class IncomingTransaction extends BaseEntity {

	// ─────────────────────────────────────────────────────────────────────────
	// [A] SOURCE IDENTIFICATION
	// ─────────────────────────────────────────────────────────────────────────
	private SourceSystem sourceSystem; // HAS-A — which system sent this payload
	private String channelCode; // "CBS" / "RTGS" / "SWIFT" / "NEFT" / "UPI" / "FINTECH"
	private String sourceRef; // Unique ref from source (UTR / TRN / CBS-REF-NO)
	private String rawPayload; // Exact wire string — never modify
	private String normalizedPayload; // JSON after parsing — for audit + re-process
	private String checksum; // SHA-256 of rawPayload — duplicate guard

	// ─────────────────────────────────────────────────────────────────────────
	// [B] TRANSACTION CORE
	// ─────────────────────────────────────────────────────────────────────────
	private TransactionType txnType; // CREDIT / DEBIT / REVERSAL / SWAP / FEE / INTRABANK
	private BigDecimal amount; // settlement amount (net for Fintech)
	private BigDecimal grossAmount; // pre-fee amount (Fintech); equals amount for others
	private BigDecimal feeAmount; // partner fee (Fintech only; ZERO for others)
	private String currency; // ISO 4217 — INR / USD / GBP / EUR / AED / …
	private LocalDate valueDate; // settlement / value date
	private LocalDateTime ingestTimestamp;// timestamp of ingestion into this system

	// ─────────────────────────────────────────────────────────────────────────
	// [C] SENDER DETAILS
	// ─────────────────────────────────────────────────────────────────────────
	private String senderIfsc; // RBI IFSC: "SBIN0001234"
	private String senderBankName; // Resolved: "State Bank of India"
	private String senderAccount; // Account number of sender (CBS / NEFT / RTGS)
	private String senderName; // Customer / corp name (SWIFT :50K:, NEFT :50:)
	private String senderVpa; // UPI Virtual Payment Address: "rahul@okaxis"
	private String senderBic; // SWIFT BIC of sending bank: "SBININBB"

	// ─────────────────────────────────────────────────────────────────────────
	// [D] RECEIVER DETAILS
	// ─────────────────────────────────────────────────────────────────────────
	private String receiverIfsc; // RBI IFSC: "HDFC0005678"
	private String receiverBankName; // Resolved: "HDFC Bank"
	private String receiverAccount; // Beneficiary account number
	private String receiverName; // Beneficiary customer / corporate name
	private String receiverVpa; // UPI VPA of beneficiary: "priya@ybl"
	private String receiverBic; // SWIFT BIC of receiving bank: "HDFCINBB"

	// ─────────────────────────────────────────────────────────────────────────
	// [E] PAYMENT METADATA
	// ─────────────────────────────────────────────────────────────────────────
	private String remarks; // Payment narration / remittance info (SWIFT :70:)
	private String narration; // Internal bank narration / CBS memo
	private String chargeType; // SWIFT :71A: — "OUR" / "BEN" / "SHA"
	private String purposeCode; // RBI purpose code: "P0001" (trade), "P1302" (salary)
	private String schemeCode; // Payment scheme / product code from source

	// ─────────────────────────────────────────────────────────────────────────
	// [F] FINTECH-SPECIFIC
	// ─────────────────────────────────────────────────────────────────────────
	private String partnerName; // "Razorpay" / "Paytm" / "PhonePe" / "Cashfree"
	private String merchantId; // Merchant identifier on Fintech platform

	// ─────────────────────────────────────────────────────────────────────────
	// [G] PROCESSING PIPELINE
	// ─────────────────────────────────────────────────────────────────────────
	private ProcessingStatus processingStatus; // current lifecycle stage
	private int priority; // 1=RTGS(max), 3=SWIFT, 5=CBS/NEFT, 7=UPI/FIN
	private int retryCount; // incremented on each reprocessing attempt
	private String errorMessage; // last error — filled on FAILED / DEAD_LETTER
	private String batchId; // FK to SettlementBatch (filled by engine)

	// ─────────────────────────────────────────────────────────────────────────
	// CONSTRUCTORS
	// ─────────────────────────────────────────────────────────────────────────

	public IncomingTransaction() {
		super();
		this.processingStatus = ProcessingStatus.RECEIVED;
		this.ingestTimestamp = LocalDateTime.now();
		this.retryCount = 0;
		this.priority = 5;
		this.feeAmount = BigDecimal.ZERO;
		this.grossAmount = BigDecimal.ZERO;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// BUSINESS METHODS
	// ─────────────────────────────────────────────────────────────────────────

	/** True when this transaction is ready to enter the settlement queue. */
	public boolean isQueueable() {
		return ProcessingStatus.VALIDATED.equals(processingStatus) || ProcessingStatus.QUEUED.equals(processingStatus);
	}

	/** True when processing has permanently failed (max retries exhausted). */
	public boolean isDeadLettered() {
		return ProcessingStatus.DEAD_LETTER.equals(processingStatus);
	}

	/** Increment retry count and record the error message, then mark FAILED. */
	public void incrementRetry(String error) {
		this.retryCount++;
		this.errorMessage = error;
		this.processingStatus = ProcessingStatus.FAILED;
		markUpdated();
	}

	/**
	 * One-line audit string — used in all System.out / log statements across
	 * adapters. Format: [CHANNEL] ref | TYPE CCY amount | senderIFSC → receiverIFSC
	 * | STATUS (pri=N)
	 */
	public String toAuditString() {
		return String.format("[%s] %s | %s %s %s | %s → %s | %s (pri=%d)", channelCode == null ? "?" : channelCode,
				sourceRef == null ? "?" : sourceRef, txnType == null ? "?" : txnType,
				currency == null ? "???" : currency, amount == null ? "0" : amount.toPlainString(),
				senderIfsc == null ? (senderBic != null ? senderBic : "?") : senderIfsc,
				receiverIfsc == null ? (receiverBic != null ? receiverBic : "?") : receiverIfsc,
				processingStatus == null ? "?" : processingStatus, priority);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GETTERS & SETTERS — [A] SOURCE
	// ─────────────────────────────────────────────────────────────────────────

	public SourceSystem getSourceSystem() {
		return sourceSystem;
	}

	public void setSourceSystem(SourceSystem v) {
		this.sourceSystem = v;
	}

	public String getChannelCode() {
		return channelCode;
	}

	public void setChannelCode(String v) {
		this.channelCode = v;
	}

	public String getSourceRef() {
		return sourceRef;
	}

	public void setSourceRef(String v) {
		this.sourceRef = v;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public void setRawPayload(String v) {
		this.rawPayload = v;
	}

	public String getNormalizedPayload() {
		return normalizedPayload;
	}

	public void setNormalizedPayload(String v) {
		this.normalizedPayload = v;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String v) {
		this.checksum = v;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GETTERS & SETTERS — [B] CORE
	// ─────────────────────────────────────────────────────────────────────────

	public TransactionType getTxnType() {
		return txnType;
	}

	public void setTxnType(TransactionType v) {
		this.txnType = v;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal v) {
		this.amount = v;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public void setGrossAmount(BigDecimal v) {
		this.grossAmount = v;
	}

	public BigDecimal getFeeAmount() {
		return feeAmount;
	}

	public void setFeeAmount(BigDecimal v) {
		this.feeAmount = v;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String v) {
		this.currency = v;
	}

	public LocalDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(LocalDate v) {
		this.valueDate = v;
	}

	public LocalDateTime getIngestTimestamp() {
		return ingestTimestamp;
	}

	public void setIngestTimestamp(LocalDateTime v) {
		this.ingestTimestamp = v;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GETTERS & SETTERS — [C] SENDER
	// ─────────────────────────────────────────────────────────────────────────

	public String getSenderIfsc() {
		return senderIfsc;
	}

	public void setSenderIfsc(String v) {
		this.senderIfsc = v;
	}

	public String getSenderBankName() {
		return senderBankName;
	}

	public void setSenderBankName(String v) {
		this.senderBankName = v;
	}

	public String getSenderAccount() {
		return senderAccount;
	}

	public void setSenderAccount(String v) {
		this.senderAccount = v;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String v) {
		this.senderName = v;
	}

	public String getSenderVpa() {
		return senderVpa;
	}

	public void setSenderVpa(String v) {
		this.senderVpa = v;
	}

	public String getSenderBic() {
		return senderBic;
	}

	public void setSenderBic(String v) {
		this.senderBic = v;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GETTERS & SETTERS — [D] RECEIVER
	// ─────────────────────────────────────────────────────────────────────────

	public String getReceiverIfsc() {
		return receiverIfsc;
	}

	public void setReceiverIfsc(String v) {
		this.receiverIfsc = v;
	}

	public String getReceiverBankName() {
		return receiverBankName;
	}

	public void setReceiverBankName(String v) {
		this.receiverBankName = v;
	}

	public String getReceiverAccount() {
		return receiverAccount;
	}

	public void setReceiverAccount(String v) {
		this.receiverAccount = v;
	}

	public String getReceiverName() {
		return receiverName;
	}

	public void setReceiverName(String v) {
		this.receiverName = v;
	}

	public String getReceiverVpa() {
		return receiverVpa;
	}

	public void setReceiverVpa(String v) {
		this.receiverVpa = v;
	}

	public String getReceiverBic() {
		return receiverBic;
	}

	public void setReceiverBic(String v) {
		this.receiverBic = v;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GETTERS & SETTERS — [E] PAYMENT METADATA
	// ─────────────────────────────────────────────────────────────────────────

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String v) {
		this.remarks = v;
	}

	public String getNarration() {
		return narration;
	}

	public void setNarration(String v) {
		this.narration = v;
	}

	public String getChargeType() {
		return chargeType;
	}

	public void setChargeType(String v) {
		this.chargeType = v;
	}

	public String getPurposeCode() {
		return purposeCode;
	}

	public void setPurposeCode(String v) {
		this.purposeCode = v;
	}

	public String getSchemeCode() {
		return schemeCode;
	}

	public void setSchemeCode(String v) {
		this.schemeCode = v;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GETTERS & SETTERS — [F] FINTECH
	// ─────────────────────────────────────────────────────────────────────────

	public String getPartnerName() {
		return partnerName;
	}

	public void setPartnerName(String v) {
		this.partnerName = v;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String v) {
		this.merchantId = v;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GETTERS & SETTERS — [G] PIPELINE
	// ─────────────────────────────────────────────────────────────────────────

	public ProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(ProcessingStatus v) {
		this.processingStatus = v;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int v) {
		this.priority = v;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int v) {
		this.retryCount = v;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String v) {
		this.errorMessage = v;
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String v) {
		this.batchId = v;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// toString
	// ─────────────────────────────────────────────────────────────────────────

	@Override
	public String toString() {
		return "IncomingTransaction{" + "id=" + id + ", ref='" + sourceRef + "'" + ", channel='" + channelCode + "'"
				+ ", type=" + txnType + ", amount=" + currency + " " + amount + ", sender=" + senderIfsc + ", receiver="
				+ receiverIfsc + ", status=" + processingStatus + ", priority=" + priority + "}";
	}
}


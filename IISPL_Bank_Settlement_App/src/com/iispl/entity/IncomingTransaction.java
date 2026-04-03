package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

public class IncomingTransaction extends BaseEntity {

	private Long incomingTxnId;

	// SOURCESYstem
	private SourceSystem sourceSystem;
	private String sourceRef;
	private String rawPayload;
	private String normalizedPayload;
	private String checksum;

	private String senderAccount;
	private String receiverAccount;
	private String senderCustomerId;
	private String receiverCustomerId;

	// CORE
	private TransactionType txnType;
	private BigDecimal amount;
	private BigDecimal grossAmount;
	private BigDecimal feeAmount;
	private String currency;
	private LocalDateTime valueDate;

	// 🔥 NEW (SOURCE STATUS)
	private TransactionStatus txnStatus;

	// PIPELINE
	private ProcessingStatus processingStatus;
	private LocalDateTime ingestTimestamp;

	// DETAILS
	private String senderIfsc;
	private String receiverIfsc;
	private String senderBankName;
	private String receiverBankName;
	private String channelCode;
	private String senderBic;
	private String receiverBic;

	// PIPELINE CONTROL
	private int priority;
	private int retryCount;
	private String errorMessage;

	// FINTECH SPECIFIC
	private String partnerName;
	private String merchantId;

	public IncomingTransaction() {
		this.processingStatus = ProcessingStatus.RECEIVED;
		this.ingestTimestamp = LocalDateTime.now();
		this.priority = 5;
		this.retryCount = 0;
		this.grossAmount = BigDecimal.ZERO;
		this.feeAmount = BigDecimal.ZERO;
	}

	// ONLY SUCCESS GOES TO SETTLEMENT
	public boolean isQueueable() {
		return TransactionStatus.SUCCESS.equals(txnStatus) && (ProcessingStatus.VALIDATED.equals(processingStatus)
				|| ProcessingStatus.QUEUED.equals(processingStatus));
	}

//	public String toAuditString() {
//		return String.format("[%s] %s | %s %s %s | %s → %s | SRC=%s | PROC=%s", channelCode, sourceRef, txnType,
//				currency, amount, senderIfsc, receiverIfsc, txnStatus, processingStatus);
//	}

	public String toAuditString() {
		return String.format("[%s] REF=%-18s | AMT=%10s %-3s | STATUS=%s/%s", safe(channelCode), safe(sourceRef),
				formatAmount(amount), safe(currency), safe(txnStatus), safe(processingStatus));
	}

	private String formatAmount(java.math.BigDecimal amt) {
		if (amt == null)
			return "0.00";
		return String.format("%,.2f", amt);
	}

	private String safe(Object val) {
		return val == null ? "N/A" : val.toString();
	}

	public TransactionStatus getTxnStatus() {
		return txnStatus;
	}

	public void setTxnStatus(TransactionStatus txnStatus) {
		this.txnStatus = txnStatus;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setGrossAmount(BigDecimal grossAmount) {
		this.grossAmount = grossAmount;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public void setFeeAmount(BigDecimal feeAmount) {
		this.feeAmount = feeAmount;
	}

	public BigDecimal getFeeAmount() {
		return feeAmount;
	}

	public void setSenderIfsc(String sendreIfsc) {
		this.senderIfsc = senderIfsc;
	}

	public String getSenderIfsc() {
		return senderIfsc;
	}

	public void setReceiverIfsc(String receiverIfsc) {
		this.receiverIfsc = receiverIfsc;
	}

	public String getReceiverIfsc() {
		return receiverIfsc;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}

	public Long getIncomingTxnId() {
		return incomingTxnId;
	}

	public void setIncomingTxnId(Long incomingTxnId) {
		this.incomingTxnId = incomingTxnId;
	}

	public SourceSystem getSourceSystem() {
		return sourceSystem;
	}

	public void setSourceSystem(SourceSystem sourceSystem) {
		this.sourceSystem = sourceSystem;
	}

	public String getSourceRef() {
		return sourceRef;
	}

	public void setSourceRef(String sourceRef) {
		this.sourceRef = sourceRef;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public void setRawPayload(String rawPayload) {
		this.rawPayload = rawPayload;
	}

	public String getNormalizedPayload() {
		return normalizedPayload;
	}

	public void setNormalizedPayload(String normalizedPayload) {
		this.normalizedPayload = normalizedPayload;
	}

	public TransactionType getTxnType() {
		return txnType;
	}

	public void setTxnType(TransactionType txnType) {
		this.txnType = txnType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public LocalDateTime getValueDate() {
		return valueDate;
	}

	public String getSenderBic() {
		return senderBic;
	}

	public void setSenderBic(String senderBic) {
		this.senderBic = senderBic;
	}

	public String getReceiverBic() {
		return receiverBic;
	}

	public void setReceiverBic(String receiverBic) {
		this.receiverBic = receiverBic;
	}

	public void setValueDate(LocalDateTime valueDate) {
		this.valueDate = valueDate;
	}

	public ProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(ProcessingStatus processingStatus) {
		this.processingStatus = processingStatus;
	}

	public LocalDateTime getIngestTimestamp() {
		return ingestTimestamp;
	}

	public void setIngestTimestamp(LocalDateTime ingestTimestamp) {
		this.ingestTimestamp = ingestTimestamp;
	}

	public String getSenderBankName() {
		return senderBankName;
	}

	public void setSenderBankName(String senderBankName) {
		this.senderBankName = senderBankName;
	}

	public String getReceiverBankName() {
		return receiverBankName;
	}

	public void setReceiverBankName(String receiverBankName) {
		this.receiverBankName = receiverBankName;
	}

	public String getChannelCode() {
		return channelCode;
	}

	public void setChannelCode(String channelCode) {
		this.channelCode = channelCode;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public String getPartnerName() {
		return partnerName;
	}

	public void setPartnerName(String partnerName) {
		this.partnerName = partnerName;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getSenderAccount() {
		return senderAccount;
	}

	public void setSenderAccount(String senderAccount) {
		this.senderAccount = senderAccount;
	}

	public String getReceiverAccount() {
		return receiverAccount;
	}

	public void setReceiverAccount(String receiverAccount) {
		this.receiverAccount = receiverAccount;
	}

	public String getSenderCustomerId() {
		return senderCustomerId;
	}

	public void setSenderCustomerId(String senderCustomerId) {
		this.senderCustomerId = senderCustomerId;
	}

	public String getReceiverCustomerId() {
		return receiverCustomerId;
	}

	public void setReceiverCustomerId(String receiverCustomerId) {
		this.receiverCustomerId = receiverCustomerId;
	}

}
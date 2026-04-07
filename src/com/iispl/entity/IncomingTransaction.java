package com.iispl.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

public final class IncomingTransaction extends BaseEntity {

	private Long incomingTxnId;

	// SOURCE
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
	
	
	private Long sourceSystemId;
	public Long getSourceSystemId() {
		return sourceSystemId;
	}

	public void setSourceSystemId(Long sourceSystemId) {
		this.sourceSystemId = sourceSystemId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String createdBy;
	private int version;

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

	public void setChecksum(String v) {
		this.checksum = v;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setGrossAmount(BigDecimal v) {
		this.grossAmount = v;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public void setFeeAmount(BigDecimal v) {
		this.feeAmount = v;
	}

	public BigDecimal getFeeAmount() {
		return feeAmount;
	}

	public void setSenderIfsc(String v) {
		this.senderIfsc = v;
	}

	public String getSenderIfsc() {
		return senderIfsc;
	}

	public void setReceiverIfsc(String v) {
		this.receiverIfsc = v;
	}

	public String getReceiverIfsc() {
		return receiverIfsc;
	}

	public void setPriority(int v) {
		this.priority = v;
	}


	public Long getIncomingTxnId() {
		return incomingTxnId;
	}

	public String getSourceRef() {
		return sourceRef;
	}

	public String getChannelCode() {
		return channelCode;
	}

	public SourceSystem getSourceSystem() {
		return sourceSystem;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public TransactionType getTxnType() {
		return txnType;
	}

	public TransactionStatus getTxnStatus() {
		return txnStatus;
	}

	public ProcessingStatus getProcessingStatus() {
		return processingStatus;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public BigDecimal getFeeAmount() {
		return feeAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getSenderCustomerId() {
		return senderCustomerId;
	}

	public String getSenderAccount() {
		return senderAccount;
	}

	public String getSenderIfsc() {
		return senderIfsc;
	}

	public String getSenderBic() {
		return senderBic;
	}

	public String getSenderBankName() {
		return senderBankName;
	}

	public String getReceiverCustomerId() {
		return receiverCustomerId;
	}

	public String getReceiverAccount() {
		return receiverAccount;
	}

	public String getReceiverIfsc() {
		return receiverIfsc;
	}

	public String getReceiverBic() {
		return receiverBic;
	}

	public String getReceiverBankName() {
		return receiverBankName;
	}

	public LocalDateTime getValueDate() {
		return valueDate;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public String getNormalizedPayload() {
		return normalizedPayload;
	}

	public String getChecksum() {
		return checksum;
	}

	public int getPriority() {
		return priority;
	}

	public String getPartnerName() {
		return partnerName;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Builder toBuilder() {
		return new Builder().incomingTxnId(this.incomingTxnId).sourceRef(this.sourceRef).channelCode(this.channelCode)
				.sourceSystem(this.sourceSystem).createdBy(this.createdBy).txnType(this.txnType)
				.txnStatus(this.txnStatus).processingStatus(this.processingStatus).amount(this.amount)
				.grossAmount(this.grossAmount).feeAmount(this.feeAmount).currency(this.currency)
				.senderCustomerId(this.senderCustomerId).senderAccount(this.senderAccount).senderIfsc(this.senderIfsc)
				.senderBic(this.senderBic).senderBankName(this.senderBankName)
				.receiverCustomerId(this.receiverCustomerId).receiverAccount(this.receiverAccount)
				.receiverIfsc(this.receiverIfsc).receiverBic(this.receiverBic).receiverBankName(this.receiverBankName)
				.valueDate(this.valueDate).rawPayload(this.rawPayload).normalizedPayload(this.normalizedPayload)
				.checksum(this.checksum).priority(this.priority).partnerName(this.partnerName)
				.merchantId(this.merchantId).errorMessage(this.errorMessage);
	}

	public String toAuditString() {
		return String.format("REF=%-22s | CH=%-6s | AMT=%12s %-3s | STATUS=%-8s/%-10s", safe(sourceRef),
				safe(channelCode), formatAmount(amount), safe(currency), safe(txnStatus), safe(processingStatus));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof IncomingTransaction))
			return false;
		IncomingTransaction that = (IncomingTransaction) o;
		return Objects.equals(sourceRef, that.sourceRef);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sourceRef);
	}

	@Override
	public String toString() {
		return "IncomingTransaction{ref='" + sourceRef + "', channel='" + channelCode + "', amount=" + amount
				+ ", status=" + processingStatus + '}';
	}

	private static String safe(Object value) {
		return value == null ? "N/A" : value.toString();
	}

	private static String formatAmount(BigDecimal amount) {
		return amount == null ? "0.00" : String.format("%,.2f", amount);
	}

	// ── Builder Class ────────────────────────────────────────────────────────

	public static final class Builder {

		private Long incomingTxnId;
		private String sourceRef;
		private String channelCode;
		private SourceSystem sourceSystem;
		private String createdBy;
		private TransactionType txnType;
		private TransactionStatus txnStatus;
		private ProcessingStatus processingStatus;
		private BigDecimal amount;
		private BigDecimal grossAmount;
		private BigDecimal feeAmount;
		private String currency;
		private String senderCustomerId;
		private String senderAccount;
		private String senderIfsc;
		private String senderBic;
		private String senderBankName;
		private String receiverCustomerId;
		private String receiverAccount;
		private String receiverIfsc;
		private String receiverBic;
		private String receiverBankName;
		private LocalDateTime valueDate;
		private String rawPayload;
		private String normalizedPayload;
		private String checksum;
		private int priority;
		private String partnerName;
		private String merchantId;
		private String errorMessage;

		public Builder incomingTxnId(Long incomingTxnId) {
			this.incomingTxnId = incomingTxnId;
			return this;
		}

		public Builder sourceRef(String sourceRef) {
			this.sourceRef = sourceRef;
			return this;
		}

		public Builder channelCode(String channelCode) {
			this.channelCode = channelCode;
			return this;
		}

		public Builder sourceSystem(SourceSystem sourceSystem) {
			this.sourceSystem = sourceSystem;
			return this;
		}

		public Builder createdBy(String createdBy) {
			this.createdBy = createdBy;
			return this;
		}

		public Builder txnType(TransactionType txnType) {
			this.txnType = txnType;
			return this;
		}

		public Builder txnStatus(TransactionStatus txnStatus) {
			this.txnStatus = txnStatus;
			return this;
		}

		public Builder processingStatus(ProcessingStatus processingStatus) {
			this.processingStatus = processingStatus;
			return this;
		}

		public Builder amount(BigDecimal amount) {
			this.amount = amount;
			return this;
		}

		public Builder grossAmount(BigDecimal grossAmount) {
			this.grossAmount = grossAmount;
			return this;
		}

		public Builder feeAmount(BigDecimal feeAmount) {
			this.feeAmount = feeAmount;
			return this;
		}

		public Builder currency(String currency) {
			this.currency = currency;
			return this;
		}

		public Builder senderCustomerId(String senderCustomerId) {
			this.senderCustomerId = senderCustomerId;
			return this;
		}

		public Builder senderAccount(String senderAccount) {
			this.senderAccount = senderAccount;
			return this;
		}

		public Builder senderIfsc(String senderIfsc) {
			this.senderIfsc = senderIfsc;
			return this;
		}

		public Builder senderBic(String senderBic) {
			this.senderBic = senderBic;
			return this;
		}

		public Builder senderBankName(String senderBankName) {
			this.senderBankName = senderBankName;
			return this;
		}

		public Builder receiverCustomerId(String receiverCustomerId) {
			this.receiverCustomerId = receiverCustomerId;
			return this;
		}

		public Builder receiverAccount(String receiverAccount) {
			this.receiverAccount = receiverAccount;
			return this;
		}

		public Builder receiverIfsc(String receiverIfsc) {
			this.receiverIfsc = receiverIfsc;
			return this;
		}

		public Builder receiverBic(String receiverBic) {
			this.receiverBic = receiverBic;
			return this;
		}

		public Builder receiverBankName(String receiverBankName) {
			this.receiverBankName = receiverBankName;
			return this;
		}

		public Builder valueDate(LocalDateTime valueDate) {
			this.valueDate = valueDate;
			return this;
		}

		public Builder rawPayload(String rawPayload) {
			this.rawPayload = rawPayload;
			return this;
		}

		public Builder normalizedPayload(String normalizedPayload) {
			this.normalizedPayload = normalizedPayload;
			return this;
		}

		public Builder checksum(String checksum) {
			this.checksum = checksum;
			return this;
		}

		public Builder priority(int priority) {
			this.priority = priority;
			return this;
		}

		public Builder partnerName(String partnerName) {
			this.partnerName = partnerName;
			return this;
		}

		public Builder merchantId(String merchantId) {
			this.merchantId = merchantId;
			return this;
		}

		public Builder errorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
			return this;
		}

		public IncomingTransaction build() {
			return new IncomingTransaction(this);
		}
	}
}
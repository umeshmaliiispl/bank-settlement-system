package com.iispl.adaptor;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;
import com.iispl.exception.IngestionException;
import com.iispl.intefaces.TransactionAdapter;
import com.iispl.validation.PayloadValidator;

/**
 * NeftUpiAdapter — Enterprise adapter handling BOTH NEFT and UPI channels.
 *
 * ONE adapter handles TWO source types because: (a) Both are RBI-regulated
 * domestic retail payment rails (b) Both use the same 7-column CSV format from
 * NPCI (c) Source type (NEFT vs UPI) is auto-detected from the REF_NO prefix
 *
 * ─────────────────────────────────────────────────────────────────────────
 * NEFT — National Electronic Funds Transfer SOURCE : NPCI NEFT Batch Gateway
 * PROTOCOL : FLAT_FILE — batch CSV file deposited via SFTP every 30 min
 * PRIORITY : 5 (NORMAL — batch processing, not real-time) LIMIT : No RBI upper
 * cap; bank policy typically ₹10,00,000 max SETTLEMENT: Batch (every 30 min, 6
 * AM–8 PM on working days) REF PREFIX: "NEFT-" e.g. NEFT-20250718-00123
 *
 * UPI — Unified Payments Interface SOURCE : NPCI UPI Real-Time Gateway PROTOCOL
 * : REST_API — real-time push (24×7, 365 days) PRIORITY : 7 (LOW —
 * micro-payments; high volume but small value) LIMIT : ₹1,00,000 max per
 * transaction (RBI circular) SETTLEMENT: Real-time REF PREFIX: "UPI-" e.g.
 * UPI-20250718-XYZ99
 * ─────────────────────────────────────────────────────────────────────────
 *
 * CSV FORMAT — 7 columns (same for NEFT and UPI): [0] REF_NO → sourceRef
 * (auto-detects NEFT or UPI from prefix) [1] TXN_TYPE → txnType (CREDIT /
 * DEBIT) [2] SENDER_IFSC → senderIfsc (RBI IFSC of sending bank) [3]
 * RECEIVER_IFSC → receiverIfsc (RBI IFSC of receiving bank) [4] AMOUNT → amount
 * [5] CURRENCY → currency [6] VALUE_DATE → valueDate (yyyy-MM-dd)
 *
 * SAMPLE DATA — neft_transactions.csv:
 * NEFT-20250718-00123,CREDIT,SBIN0001234,HDFC0005678,50000.00,INR,2025-07-18
 * NEFT-20250718-00124,CREDIT,HDFC0005678,ICIC0009999,15000.00,INR,2025-07-18
 *
 * SAMPLE DATA — upi_transactions.csv:
 * UPI-20250718-XYZ99,CREDIT,AXIS0001122,ICIC0009999,2500.00,INR,2025-07-18
 * UPI-20250718-MNO90,CREDIT,HDFC0005678,AXIS0001122,99000.00,INR,2025-07-18
 *
 * VALIDATION APPLIED: NEFT: amount <= ₹10 lakh, valid IFSC UPI : amount <= ₹1
 * lakh (RBI), amount >= ₹1, valid IFSC Both: sender IFSC ≠ receiver IFSC,
 * valueDate not stale
 */
public class NeftUpiAdapter implements TransactionAdapter {

	private static final SourceSystem NEFT_SOURCE = SourceSystem.NEFT();
	private static final SourceSystem UPI_SOURCE = SourceSystem.UPI();

	private static final BigDecimal UPI_MAX_AMOUNT = new BigDecimal("100000.00");

	// ─────────────────────────────────────────────────────────────────────────
	// ADAPT
	// ─────────────────────────────────────────────────────────────────────────

	@Override
	public IncomingTransaction adapt(String rawPayload) {

		if (rawPayload == null || rawPayload.trim().isEmpty())
			throw new IngestionException(IngestionException.ERR_NULL_PAYLOAD, SourceType.NEFT, rawPayload,
					"rawPayload is null or empty");

		// ── Parse 7-column CSV ────────────────────────────────────────────────
		String[] f = rawPayload.split(",", -1);

		if (f.length < 7)
			throw new IngestionException(IngestionException.ERR_INVALID_FORMAT, SourceType.NEFT, rawPayload,
					"Expected 7 CSV columns, got: " + f.length + " | Payload: " + rawPayload);

		String refNo = trim(f[0]);

		// ── Auto-detect NEFT vs UPI from reference prefix ─────────────────────
		boolean isUpi = refNo.toUpperCase().startsWith("UPI-");
		SourceType channel = isUpi ? SourceType.UPI : SourceType.NEFT;
		SourceSystem source = isUpi ? UPI_SOURCE : NEFT_SOURCE;
		String adapterTag = isUpi ? "UPI-ADAPTER" : "NEFT-ADAPTER";
		int priority = isUpi ? 7 : 5;

		IncomingTransaction txn = new IncomingTransaction();
		txn.setSourceSystem(source);
		txn.setChannelCode(isUpi ? "UPI" : "NEFT");
		txn.setRawPayload(rawPayload);
		txn.setChecksum(sha256(rawPayload));
		txn.setCreatedBy(adapterTag);
		txn.setPriority(priority);

		// [A] Source ref
		txn.setSourceRef(refNo);

		// [B] Transaction type
		try {
			txn.setTxnType(TransactionType.valueOf(trim(f[1]).toUpperCase()));
		} catch (IllegalArgumentException e) {
			throw new IngestionException(IngestionException.ERR_INVALID_FORMAT, channel, rawPayload,
					"Invalid TXN_TYPE: '" + f[1].trim() + "'. Expected: CREDIT or DEBIT");
		}

		// [C] Sender
		txn.setSenderIfsc(trim(f[2]).toUpperCase());
		txn.setSenderBankName(BankNameResolver.fromIfsc(txn.getSenderIfsc()));

		// [D] Receiver
		txn.setReceiverIfsc(trim(f[3]).toUpperCase());
		txn.setReceiverBankName(BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

		// [B] Amount
		try {
			txn.setAmount(new BigDecimal(trim(f[4])));
			txn.setGrossAmount(txn.getAmount());
			txn.setFeeAmount(BigDecimal.ZERO);
		} catch (NumberFormatException e) {
			throw new IngestionException(IngestionException.ERR_INVALID_FORMAT, channel, rawPayload,
					"Invalid AMOUNT: '" + f[4].trim() + "'");
		}

		// [B] Currency
		txn.setCurrency(trim(f[5]).toUpperCase());

		// [B] Value date
		try {
			txn.setValueDate(LocalDate.parse(trim(f[6])));
		} catch (Exception e) {
			throw new IngestionException(IngestionException.ERR_INVALID_FORMAT, channel, rawPayload,
					"Invalid VALUE_DATE: '" + f[6].trim() + "'. Expected: yyyy-MM-dd");
		}

		// ── UPI hard limit — throw immediately (RBI mandate) ──────────────────
		if (isUpi && txn.getAmount().compareTo(UPI_MAX_AMOUNT) > 0) {
			throw new IngestionException(IngestionException.ERR_LIMIT_EXCEEDED, SourceType.UPI, rawPayload,
					"UPI transaction " + refNo + " exceeds RBI limit ₹1,00,000. " + "Amount: ₹" + txn.getAmount()
							+ " (RBI circular DPSS.CO.PD.No.1201/02.14.003/2019-20)");
		}

		txn.setNormalizedPayload(buildNormalized(txn, isUpi));

		// ── Full 3-level validation ───────────────────────────────────────────
		PayloadValidator.ValidationResult vr = PayloadValidator.validate(txn, channel);
		if (!vr.isPassed()) {
			txn.setProcessingStatus(ProcessingStatus.FAILED);
			txn.setErrorMessage(vr.getErrorSummary());
			System.err.println("  [" + adapterTag + "][FAIL] " + vr);
		} else {
			txn.setProcessingStatus(ProcessingStatus.QUEUED);
			source.recordSuccess();
		}

		System.out.println("  [" + adapterTag + "] " + txn.toAuditString());
		return txn;
	}

	/**
	 * Returns NEFT as the primary SourceType for registry registration.
	 * AdapterRegistry separately registers this same instance for UPI as well.
	 */
	@Override
	public SourceType getSourceType() {
		return SourceType.NEFT;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// HELPERS
	// ─────────────────────────────────────────────────────────────────────────

	private String buildNormalized(IncomingTransaction txn, boolean isUpi) {
		return "{" + "\"source\":\"" + (isUpi ? "UPI" : "NEFT") + "\"," + "\"ref\":\"" + txn.getSourceRef() + "\","
				+ "\"type\":\"" + txn.getTxnType() + "\"," + "\"senderIfsc\":\"" + txn.getSenderIfsc() + "\","
				+ "\"senderBank\":\"" + txn.getSenderBankName() + "\"," + "\"receiverIfsc\":\"" + txn.getReceiverIfsc()
				+ "\"," + "\"receiverBank\":\"" + txn.getReceiverBankName() + "\"," + "\"amount\":" + txn.getAmount()
				+ "," + "\"currency\":\"" + txn.getCurrency() + "\"," + "\"valueDate\":\"" + txn.getValueDate() + "\","
				+ "\"priority\":" + txn.getPriority() + "," + "\"checksum\":\"" + txn.getChecksum() + "\"" + "}";
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	private static String sha256(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(input.getBytes("UTF-8"));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash)
				sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (Exception e) {
			return "CHECKSUM-ERROR";
		}
	}
}
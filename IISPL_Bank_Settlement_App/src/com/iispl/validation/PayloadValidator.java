package com.iispl.validation;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PayloadValidator — Three-level validation pipeline for IncomingTransaction.
 *
 * Called by every adapter AFTER parsing, BEFORE setting QUEUED status.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * LEVEL 1 — STRUCTURAL Mandatory field presence, data types, field lengths.
 * Applies to ALL channels equally.
 *
 * LEVEL 2 — BUSINESS RULES (channel-specific, RBI-mandated) RTGS : amount >=
 * ₹2,00,000 UPI : amount <= ₹1,00,000 (RBI limit) NEFT : amount <= ₹10,00,000
 * (typical ceiling) SWIFT : BIC length >= 8 chars CBS : IFSC format FINTECH:
 * gross - fee = net; partnerName mandatory
 *
 * LEVEL 3 — CROSS-FIELD CONSISTENCY Sender IFSC != Receiver IFSC valueDate not
 * stale (> 30 days old)
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Usage: ValidationResult vr = PayloadValidator.validate(txn, SourceType.RTGS);
 * if (!vr.isPassed()) { txn.setErrorMessage(vr.getErrorSummary()); }
 */
public final class PayloadValidator {

	// ── RBI / Business Rule Constants ─────────────────────────────────────────
	private static final BigDecimal RTGS_MINIMUM = new BigDecimal("200000.00");
	private static final BigDecimal UPI_MAXIMUM = new BigDecimal("100000.00");
	private static final BigDecimal NEFT_MAXIMUM = new BigDecimal("1000000.00");
	private static final BigDecimal ZERO = BigDecimal.ZERO;

	// RBI-approved settlement currencies
	private static final Set<String> VALID_CURRENCIES = new HashSet<>(Arrays.asList("INR", "USD", "GBP", "EUR", "JPY",
			"AUD", "CAD", "SGD", "AED", "CHF", "HKD", "SEK", "NOK", "DKK"));

	// IFSC pattern: 4 uppercase alpha + literal '0' + 6 alphanumeric
	// Example: SBIN0001234, HDFC0005678, ICIC0009999
	private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

	// SWIFT BIC: 8 or 11 characters
	private static final Pattern BIC_PATTERN = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");

	// Utility class — no instantiation
	private PayloadValidator() {
	}

	// ─────────────────────────────────────────────────────────────────────────
	// PUBLIC API
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Run the full three-level validation pipeline.
	 *
	 * @param txn        the parsed IncomingTransaction to validate
	 * @param sourceType which channel the transaction came from
	 * @return ValidationResult (passed + list of error strings)
	 */
	public static ValidationResult validate(IncomingTransaction txn, SourceType sourceType) {
		List<String> errors = new ArrayList<>();

		// Level 1 — Structural (always runs)
		validateStructural(txn, errors);

		// Level 2 — Business rules (only if Level 1 clean)
		if (errors.isEmpty()) {
			validateBusinessRules(txn, sourceType, errors);
		}

		// Level 3 — Cross-field consistency (only if Levels 1 & 2 clean)
		if (errors.isEmpty()) {
			validateCrossField(txn, errors);
		}

		return new ValidationResult(errors.isEmpty(), errors, sourceType);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// LEVEL 1 — STRUCTURAL VALIDATION
	// ─────────────────────────────────────────────────────────────────────────

	private static void validateStructural(IncomingTransaction txn, List<String> errors) {

		// sourceRef is the primary key from source — absolutely mandatory
		if (isBlank(txn.getSourceRef()))
			errors.add("[ING-002] sourceRef is mandatory and cannot be blank");

		// txnType must be set by adapter
		if (txn.getTxnType() == null)
			errors.add("[ING-002] txnType is mandatory");

		// amount: must exist and be positive
		if (txn.getAmount() == null)
			errors.add("[ING-002] amount is mandatory");
		else if (txn.getAmount().compareTo(ZERO) <= 0)
			errors.add("[ING-003] amount must be > 0, got: " + txn.getAmount());

		// currency: 3-char ISO 4217
		if (isBlank(txn.getCurrency()))
			errors.add("[ING-002] currency is mandatory");
		else if (txn.getCurrency().length() != 3)
			errors.add("[ING-003] currency must be 3-char ISO 4217, got: '" + txn.getCurrency() + "'");
		else if (!VALID_CURRENCIES.contains(txn.getCurrency().toUpperCase()))
			errors.add("[ING-004] currency not in RBI-approved list: " + txn.getCurrency() + " | Allowed: "
					+ VALID_CURRENCIES);

		// valueDate: mandatory
		if (txn.getValueDate() == null)
			errors.add("[ING-002] valueDate is mandatory");

		// sourceSystem: must be wired by adapter
		if (txn.getSourceSystem() == null)
			errors.add("[ING-002] sourceSystem must be set by adapter");

		// channelCode: must be set by adapter
		if (isBlank(txn.getChannelCode()))
			errors.add("[ING-002] channelCode must be set by adapter");
	}

	// ─────────────────────────────────────────────────────────────────────────
	// LEVEL 2 — CHANNEL-SPECIFIC BUSINESS RULES
	// ─────────────────────────────────────────────────────────────────────────

	private static void validateBusinessRules(IncomingTransaction txn, SourceType sourceType, List<String> errors) {
		BigDecimal amount = txn.getAmount();

		switch (sourceType) {

		case RTGS:
			// RBI mandates: RTGS minimum ₹2,00,000
			if (amount.compareTo(RTGS_MINIMUM) < 0)
				errors.add("[ING-004] RTGS minimum is ₹2,00,000. " + "Got: ₹" + amount
						+ ". Use NEFT for smaller amounts.");
			// RTGS must have valid IFSC codes
			validateIfsc(txn.getSenderIfsc(), "senderIfsc", errors);
			validateIfsc(txn.getReceiverIfsc(), "receiverIfsc", errors);
			// UTR (sourceRef) must be non-blank (already checked) and 16 chars
			if (!isBlank(txn.getSourceRef()) && txn.getSourceRef().length() != 16)
				errors.add("[ING-003] RTGS UTR must be exactly 16 characters. " + "Got: " + txn.getSourceRef().length()
						+ " chars → '" + txn.getSourceRef() + "'");
			break;

		case UPI:
			// RBI UPI limit: max ₹1,00,000
			if (amount.compareTo(UPI_MAXIMUM) > 0)
				errors.add("[ING-007] UPI per-transaction limit is ₹1,00,000. " + "Got: ₹" + amount
						+ " (RBI circular DPSS.CO.PD.No.1201/02.14.003/2019-20)");
			// UPI min: Re.1
			if (amount.compareTo(BigDecimal.ONE) < 0)
				errors.add("[ING-004] UPI minimum is ₹1.00. Got: ₹" + amount);
			validateIfsc(txn.getSenderIfsc(), "senderIfsc", errors);
			validateIfsc(txn.getReceiverIfsc(), "receiverIfsc", errors);
			break;

		case NEFT:
			// Typical NEFT ceiling ₹10 lakh (no RBI upper cap, but bank policy)
			if (amount.compareTo(NEFT_MAXIMUM) > 0)
				errors.add("[ING-007] NEFT bank-policy limit is ₹10,00,000. " + "Got: ₹" + amount
						+ ". Use RTGS for higher amounts.");
			validateIfsc(txn.getSenderIfsc(), "senderIfsc", errors);
			validateIfsc(txn.getReceiverIfsc(), "receiverIfsc", errors);
			break;

		case SWIFT:
			// SWIFT amount must be positive (already in L1, extra message here)
			if (amount.compareTo(ZERO) <= 0)
				errors.add("[ING-004] SWIFT MT103 amount must be > 0");
			// BIC validation
			validateBic(txn.getSenderBic(), "senderBic", errors);
			validateBic(txn.getReceiverBic(), "receiverBic", errors);
			// TRN (sourceRef) max 16 chars per SWIFT spec
			if (!isBlank(txn.getSourceRef()) && txn.getSourceRef().length() > 16)
				errors.add("[ING-003] SWIFT TRN must be <= 16 characters. " + "Got: " + txn.getSourceRef().length());
			break;

		case FINTECH:
			// net_amount = gross_amount - fee_amount
			if (txn.getGrossAmount() != null && txn.getFeeAmount() != null) {
				BigDecimal expectedNet = txn.getGrossAmount().subtract(txn.getFeeAmount());
				if (expectedNet.compareTo(txn.getAmount()) != 0)
					errors.add("[ING-003] Fintech net_amount mismatch. " + "gross(" + txn.getGrossAmount() + ") - fee("
							+ txn.getFeeAmount() + ") = " + expectedNet + " ≠ amount(" + txn.getAmount() + ")");
			}
			// partnerName is mandatory for audit trail
			if (isBlank(txn.getPartnerName()))
				errors.add("[ING-002] partnerName is mandatory for FINTECH channel");
			// fee cannot be negative
			if (txn.getFeeAmount() != null && txn.getFeeAmount().compareTo(ZERO) < 0)
				errors.add("[ING-003] feeAmount cannot be negative. Got: " + txn.getFeeAmount());
			break;

		case CBS:
			validateIfsc(txn.getSenderIfsc(), "senderIfsc", errors);
			validateIfsc(txn.getReceiverIfsc(), "receiverIfsc", errors);
			break;

		default:
			// INTERNAL — no channel-specific rules
			break;
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// LEVEL 3 — CROSS-FIELD CONSISTENCY
	// ─────────────────────────────────────────────────────────────────────────

	private static void validateCrossField(IncomingTransaction txn, List<String> errors) {

		// Sender and receiver IFSC must differ (prevent same-account loops)
		if (!isBlank(txn.getSenderIfsc()) && !isBlank(txn.getReceiverIfsc())
				&& txn.getSenderIfsc().equalsIgnoreCase(txn.getReceiverIfsc()))
			errors.add("[ING-004] senderIfsc and receiverIfsc cannot be identical: " + txn.getSenderIfsc()
					+ " (intrabank transfers should use INTRABANK type)");

		// valueDate must not be more than 30 days in the past (stale transaction guard)
		if (txn.getValueDate() != null) {
			LocalDate cutoff = LocalDate.now().minusDays(30);
			if (txn.getValueDate().isBefore(cutoff))
				errors.add("[ING-004] valueDate " + txn.getValueDate() + " is more than 30 days old. "
						+ "Possible stale/replayed transaction. Cutoff: " + cutoff);
		}

		// valueDate must not be unreasonably far in the future (> 7 business days)
		if (txn.getValueDate() != null) {
			LocalDate future = LocalDate.now().plusDays(10);
			if (txn.getValueDate().isAfter(future))
				errors.add("[ING-004] valueDate " + txn.getValueDate() + " is more than 10 days in the future. "
						+ "Possible data error.");
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// HELPER VALIDATORS
	// ─────────────────────────────────────────────────────────────────────────

	private static void validateIfsc(String ifsc, String fieldName, List<String> errors) {
		if (!isBlank(ifsc) && !IFSC_PATTERN.matcher(ifsc.toUpperCase()).matches())
			errors.add("[ING-003] Invalid " + fieldName + " format: '" + ifsc
					+ "'. Expected: 4 alpha + '0' + 6 alphanumeric (e.g. SBIN0001234)");
	}

	private static void validateBic(String bic, String fieldName, List<String> errors) {
		if (!isBlank(bic) && !BIC_PATTERN.matcher(bic.toUpperCase()).matches())
			errors.add("[ING-003] Invalid " + fieldName + " SWIFT BIC: '" + bic
					+ "'. Expected 8 or 11 char BIC (e.g. SBININBB or SBININBBXXX)");
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// INNER CLASS: ValidationResult
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Immutable result returned by PayloadValidator.validate(). The adapter
	 * inspects passed/errors and sets ProcessingStatus accordingly.
	 */
	public static final class ValidationResult {

		private final boolean passed;
		private final List<String> errors;
		private final SourceType sourceType;

		ValidationResult(boolean passed, List<String> errors, SourceType sourceType) {
			this.passed = passed;
			this.errors = errors;
			this.sourceType = sourceType;
		}

		/** True when ALL three levels passed with zero errors. */
		public boolean isPassed() {
			return passed;
		}

		/** Returns the list of error strings (empty if passed). */
		public List<String> getErrors() {
			return errors;
		}

		/** Single-string summary, pipe-separated. */
		public String getErrorSummary() {
			return String.join(" | ", errors);
		}

		/** How many errors were found. */
		public int getErrorCount() {
			return errors.size();
		}

		@Override
		public String toString() {
			return passed ? "[" + sourceType + "] VALID"
					: "[" + sourceType + "] INVALID (" + errors.size() + " error(s)): " + getErrorSummary();
		}
	}
}
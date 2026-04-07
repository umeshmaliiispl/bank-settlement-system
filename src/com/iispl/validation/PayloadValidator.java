package com.iispl.validation;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * PayloadValidator — Stateless, three-tier payload validator for incoming
 * bank settlement transactions in the IISPL pipeline.
 *
 * <p>Validation is executed in three sequential levels. Each level runs only
 * if the previous level produced zero errors, preventing misleading cascading
 * failures:
 * <ol>
 *   <li><b>Structural</b>   — mandatory field presence and format checks</li>
 *   <li><b>Business Rules</b> — source-type-specific amount limits and routing code checks</li>
 *   <li><b>Cross-Field</b>  — inter-field consistency checks (e.g., same IFSC, stale timestamp)</li>
 * </ol>
 *
 * <p><b>Design characteristics:</b>
 * <ul>
 *   <li>Declared {@code final} and {@code private} constructor — utility class, not instantiable</li>
 *   <li>Reads from {@link IncomingTransaction} via getters only — input is never mutated</li>
 *   <li>Returns a {@link ValidationResult} value object — all errors are collected before returning</li>
 *   <li>Fully stateless — safe for concurrent use without synchronisation</li>
 * </ul>
 *
 * <p><b>Error Code Reference:</b>
 * <pre>
 * ┌──────────┬──────────────────────────────────────────────────────────────┐
 * │ Code     │ Meaning                                                      │
 * ├──────────┼──────────────────────────────────────────────────────────────┤
 * │ ING-002  │ Mandatory field is null or blank                             │
 * │ ING-003  │ Field present but fails format or length constraint          │
 * │ ING-004  │ Field value violates a business or cross-field rule          │
 * │ ING-007  │ Amount exceeds the maximum allowed for this source type      │
 * └──────────┴──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author IISPL Bank Platform Team
 * @since 1.0
 */
public final class PayloadValidator {

    // =========================================================================
    // Amount Thresholds (RBI / NPCI mandates)
    // =========================================================================

    /** Minimum transaction amount for RTGS, as mandated by RBI (₹2,00,000). */
    private static final BigDecimal RTGS_MINIMUM_AMOUNT = new BigDecimal("200000.00");

    /** Maximum transaction amount for UPI, as mandated by RBI (₹1,00,000). */
    private static final BigDecimal UPI_MAXIMUM_AMOUNT = new BigDecimal("100000.00");

    /** Maximum transaction amount for NEFT, as per typical NPCI limit (₹10,00,000). */
    private static final BigDecimal NEFT_MAXIMUM_AMOUNT = new BigDecimal("1000000.00");

    /** Constant zero used for amount positivity checks, avoiding repeated object creation. */
    private static final BigDecimal ZERO = BigDecimal.ZERO;


    // =========================================================================
    // Allowed Currency Codes (ISO 4217)
    // =========================================================================

    /**
     * Set of ISO 4217 currency codes accepted by the settlement pipeline.
     * Lookup is O(1); comparison is done after upper-casing the input.
     */
    private static final Set<String> ACCEPTED_CURRENCY_CODES = new HashSet<>(Arrays.asList(
            "INR", "USD", "GBP", "EUR", "JPY",
            "AUD", "CAD", "SGD", "AED", "CHF",
            "HKD", "SEK", "NOK", "DKK"
    ));


    // =========================================================================
    // Routing Code Patterns
    // =========================================================================

    /**
     * Compiled regex for Indian Financial System Code (IFSC) validation.
     * Format: 4 uppercase letters + literal '0' + 6 alphanumeric characters.
     * Example: {@code HDFC0001234}
     */
    private static final Pattern IFSC_CODE_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    /**
     * Compiled regex for SWIFT Bank Identifier Code (BIC) validation.
     * Format: 4-letter bank code + 2-letter country + 2-char location + optional 3-char branch.
     * Example: {@code HDFCINBB} or {@code HDFCINBBXXX}
     */
    private static final Pattern SWIFT_BIC_PATTERN = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");

    /** Maximum age of a transaction timestamp before it is considered stale (30 days). */
    private static final int STALE_TRANSACTION_THRESHOLD_DAYS = 30;

    /** Required character length for an RTGS Unique Transaction Reference (UTR). */
    private static final int RTGS_UTR_REQUIRED_LENGTH = 16;

    /** Required character count for an ISO 4217 currency code. */
    private static final int CURRENCY_CODE_REQUIRED_LENGTH = 3;


    // =========================================================================
    // Private Constructor — Utility Class
    // =========================================================================

    /** Prevents instantiation. All methods are static; this class is a utility. */
    private PayloadValidator() { }


    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Validates the given incoming transaction in three sequential levels:
     * structural, business-rule, and cross-field.
     *
     * <p>Each level executes only if the previous level collected zero errors,
     * keeping error output focused on the root cause rather than cascading symptoms.
     *
     * @param txn        the incoming transaction payload to validate; must not be {@code null}
     * @param sourceType the originating source channel (CBS, RTGS, SWIFT, NEFT, UPI, FINTECH)
     * @return a {@link ValidationResult} containing the pass/fail outcome and all collected errors
     */
    public static ValidationResult validate(IncomingTransaction txn, SourceType sourceType) {

        List<String> validationErrors = new ArrayList<>();

        // Level 1 — Structural: mandatory fields and basic format
        validateStructural(txn, validationErrors);

        // Level 2 — Business Rules: source-type-specific amount limits and routing codes
        // Only runs when structural validation is clean, avoiding misleading cascades
        if (validationErrors.isEmpty()) {
            validateBusinessRules(txn, sourceType, validationErrors);
        }

        // Level 3 — Cross-Field: inter-field consistency rules
        // Only runs when both prior levels are clean
        if (validationErrors.isEmpty()) {
            validateCrossField(txn, validationErrors);
        }

        return new ValidationResult(validationErrors.isEmpty(), validationErrors, sourceType);
    }


    // =========================================================================
    // Level 1 — Structural Validation
    // =========================================================================

    /**
     * Checks that all mandatory fields are present and satisfy basic format constraints.
     * Errors are appended to {@code validationErrors}; this method does not short-circuit.
     *
     * <p>Fields validated: {@code sourceRef}, {@code txnType}, {@code amount},
     * {@code currency}, {@code valueDate}, {@code sourceSystem}, {@code channelCode},
     * {@code txnStatus}.
     *
     * @param txn              the transaction to inspect
     * @param validationErrors mutable list to which error codes are appended
     */
    private static void validateStructural(IncomingTransaction txn, List<String> validationErrors) {

        if (isBlank(txn.getSourceRef())) {
            validationErrors.add("[ING-002] sourceRef is mandatory");
        }

        if (txn.getTxnType() == null) {
            validationErrors.add("[ING-002] txnType is mandatory");
        }

        if (txn.getAmount() == null) {
            validationErrors.add("[ING-002] amount is mandatory");
        } else if (txn.getAmount().compareTo(ZERO) <= 0) {
            validationErrors.add("[ING-003] amount must be > 0");
        }

        if (isBlank(txn.getCurrency())) {
            validationErrors.add("[ING-002] currency is mandatory");
        } else if (txn.getCurrency().length() != CURRENCY_CODE_REQUIRED_LENGTH) {
            validationErrors.add("[ING-003] currency must be 3 chars");
        } else if (!ACCEPTED_CURRENCY_CODES.contains(txn.getCurrency().toUpperCase())) {
            validationErrors.add("[ING-004] invalid currency: " + txn.getCurrency());
        }

        if (txn.getValueDate() == null) {
            validationErrors.add("[ING-002] txnTimestamp is mandatory");
        }

        if (txn.getSourceSystem() == null) {
            validationErrors.add("[ING-002] sourceSystem required");
        }

        if (isBlank(txn.getChannelCode())) {
            validationErrors.add("[ING-002] channelCode required");
        }

        if (txn.getTxnStatus() == null) {
            validationErrors.add("[ING-002] txnStatus is mandatory");
        }
    }


    // =========================================================================
    // Level 2 — Business Rule Validation (Source-Type Specific)
    // =========================================================================

    /**
     * Applies source-type-specific business rules: amount thresholds, routing code
     * requirements, UTR format, and partner field requirements.
     *
     * <p>Rules by source type:
     * <ul>
     *   <li><b>RTGS</b>    — amount ≥ ₹2,00,000; valid sender/receiver IFSC; UTR must be 16 chars</li>
     *   <li><b>UPI</b>     — amount ≤ ₹1,00,000; valid sender/receiver IFSC</li>
     *   <li><b>NEFT</b>    — amount ≤ ₹10,00,000; valid sender/receiver IFSC</li>
     *   <li><b>SWIFT</b>   — valid sender/receiver BIC (IFSC not applicable for cross-border)</li>
     *   <li><b>FINTECH</b> — net amount must equal gross minus fee; partnerName is mandatory</li>
     *   <li><b>CBS</b>     — valid sender/receiver IFSC</li>
     * </ul>
     *
     * @param txn              the transaction to inspect
     * @param sourceType       the originating source channel
     * @param validationErrors mutable list to which error codes are appended
     */
    private static void validateBusinessRules(
            IncomingTransaction txn,
            SourceType sourceType,
            List<String> validationErrors) {

        BigDecimal transactionAmount = txn.getAmount();

        switch (sourceType) {

            case RTGS:
                // RBI mandates RTGS only for high-value transfers of ₹2,00,000 and above
                if (transactionAmount.compareTo(RTGS_MINIMUM_AMOUNT) < 0) {
                    validationErrors.add("[ING-004] RTGS minimum ₹2,00,000");
                }
                validateIfscCode(txn.getSenderIfsc(), "senderIfsc", validationErrors);
                validateIfscCode(txn.getReceiverIfsc(), "receiverIfsc", validationErrors);
                // RTGS UTR is exactly 16 characters as per RBI specification
                if (!isBlank(txn.getSourceRef()) && txn.getSourceRef().length() != RTGS_UTR_REQUIRED_LENGTH) {
                    validationErrors.add("[ING-003] RTGS UTR must be 16 chars");
                }
                break;

            case UPI:
                // RBI caps UPI per-transaction at ₹1,00,000
                if (transactionAmount.compareTo(UPI_MAXIMUM_AMOUNT) > 0) {
                    validationErrors.add("[ING-007] UPI max ₹1,00,000");
                }
                validateIfscCode(txn.getSenderIfsc(), "senderIfsc", validationErrors);
                validateIfscCode(txn.getReceiverIfsc(), "receiverIfsc", validationErrors);
                break;

            case NEFT:
                // NPCI caps NEFT per-transaction at ₹10,00,000
                if (transactionAmount.compareTo(NEFT_MAXIMUM_AMOUNT) > 0) {
                    validationErrors.add("[ING-007] NEFT max ₹10,00,000");
                }
                validateIfscCode(txn.getSenderIfsc(), "senderIfsc", validationErrors);
                validateIfscCode(txn.getReceiverIfsc(), "receiverIfsc", validationErrors);
                break;

            case SWIFT:
                // Cross-border payments use BIC/SWIFT codes instead of IFSC
                validateSwiftBicCode(txn.getSenderBic(), "senderBic", validationErrors);
                validateSwiftBicCode(txn.getReceiverBic(), "receiverBic", validationErrors);
                break;

            case FINTECH:
                // Net amount must reconcile exactly: grossAmount - feeAmount = amount
                if (txn.getGrossAmount() != null && txn.getFeeAmount() != null) {
                    BigDecimal expectedNetAmount = txn.getGrossAmount().subtract(txn.getFeeAmount());
                    if (expectedNetAmount.compareTo(txn.getAmount()) != 0) {
                        validationErrors.add("[ING-003] net mismatch");
                    }
                }
                if (isBlank(txn.getPartnerName())) {
                    validationErrors.add("[ING-002] partnerName required");
                }
                break;

            case CBS:
                validateIfscCode(txn.getSenderIfsc(), "senderIfsc", validationErrors);
                validateIfscCode(txn.getReceiverIfsc(), "receiverIfsc", validationErrors);
                break;

            default:
                break;
        }
    }


    // =========================================================================
    // Level 3 — Cross-Field Validation
    // =========================================================================

    /**
     * Validates consistency rules that span multiple fields and cannot be expressed
     * as single-field checks.
     *
     * <p>Checks performed:
     * <ul>
     *   <li>Sender and receiver IFSC codes must differ</li>
     *   <li>Transaction timestamp must not be older than {@value #STALE_TRANSACTION_THRESHOLD_DAYS} days</li>
     * </ul>
     *
     * @param txn              the transaction to inspect
     * @param validationErrors mutable list to which error codes are appended
     */
    private static void validateCrossField(IncomingTransaction txn, List<String> validationErrors) {

        // Sender and receiver must not share the same IFSC (prevents same-branch self-routing)
        if (!isBlank(txn.getSenderIfsc()) && txn.getSenderIfsc().equalsIgnoreCase(txn.getReceiverIfsc())) {
            validationErrors.add("[ING-004] sender & receiver cannot be same");
        }

        // Reject transactions with a value date older than the stale-transaction threshold
        if (txn.getValueDate() != null) {
            LocalDateTime staleThresholdTimestamp = LocalDateTime.now().minusDays(STALE_TRANSACTION_THRESHOLD_DAYS);
            if (txn.getValueDate().isBefore(staleThresholdTimestamp)) {
                validationErrors.add("[ING-004] txnTimestamp too old");
            }
        }
    }


    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Validates an IFSC code against the RBI-specified format if the value is present.
     * Blank values are silently skipped — presence is enforced separately at the structural level.
     *
     * @param ifscCode         the IFSC string to validate
     * @param fieldName        the field name used in the error message (e.g., "senderIfsc")
     * @param validationErrors mutable list to which an error is appended on mismatch
     */
    private static void validateIfscCode(String ifscCode, String fieldName, List<String> validationErrors) {
        if (!isBlank(ifscCode) && !IFSC_CODE_PATTERN.matcher(ifscCode).matches()) {
            validationErrors.add("[ING-003] invalid " + fieldName);
        }
    }

    /**
     * Validates a SWIFT BIC code against the ISO 9362 format if the value is present.
     * Blank values are silently skipped — presence is enforced separately at the structural level.
     *
     * @param bicCode          the BIC string to validate
     * @param fieldName        the field name used in the error message (e.g., "senderBic")
     * @param validationErrors mutable list to which an error is appended on mismatch
     */
    private static void validateSwiftBicCode(String bicCode, String fieldName, List<String> validationErrors) {
        if (!isBlank(bicCode) && !SWIFT_BIC_PATTERN.matcher(bicCode).matches()) {
            validationErrors.add("[ING-003] invalid " + fieldName);
        }
    }

    /**
     * Returns {@code true} if the given string is {@code null}, empty, or contains
     * only whitespace characters.
     *
     * @param value the string to test
     * @return {@code true} if blank; {@code false} otherwise
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }


    // =========================================================================
    // Nested Result Value Object
    // =========================================================================

    /**
     * Immutable value object that carries the outcome of a {@link PayloadValidator#validate} call.
     *
     * <p>Contains:
     * <ul>
     *   <li>{@code passed}     — {@code true} if validation produced zero errors</li>
     *   <li>{@code errors}     — unmodifiable list of structured error codes and messages</li>
     *   <li>{@code sourceType} — the source channel that was validated (used in {@code toString})</li>
     * </ul>
     */
    public static final class ValidationResult {

        /** {@code true} if all validation levels passed with zero errors. */
        private final boolean passed;

        /** Ordered list of error codes and messages collected across all validation levels. */
        private final List<String> errors;

        /** The source channel that was validated; included in the string representation. */
        private final SourceType sourceType;

        /**
         * Package-private constructor — instances are created only by {@link PayloadValidator}.
         *
         * @param passed     {@code true} if validation succeeded
         * @param errors     list of error codes; must not be {@code null}
         * @param sourceType the originating source channel
         */
        ValidationResult(boolean passed, List<String> errors, SourceType sourceType) {
            this.passed     = passed;
            this.errors     = errors;
            this.sourceType = sourceType;
        }

        /**
         * Returns {@code true} if the transaction payload passed all validation levels.
         *
         * @return {@code true} on success; {@code false} if any errors were collected
         */
        public boolean isPassed() {
            return passed;
        }

        /**
         * Returns the list of structured error codes collected during validation.
         * The list is empty when {@link #isPassed()} is {@code true}.
         *
         * @return list of error strings; never {@code null}
         */
        public List<String> getErrors() {
            return errors;
        }

        /**
         * Returns all error messages joined into a single pipe-delimited string,
         * suitable for logging or storing in an {@code error_message} column.
         *
         * <p>Example: {@code "[ING-002] sourceRef is mandatory | [ING-003] amount must be > 0"}
         *
         * @return pipe-delimited error summary, or an empty string if there are no errors
         */
        public String getErrorSummary() {
            return String.join(" | ", errors);
        }

        /**
         * Returns a concise, log-safe representation of this validation result.
         * <p>Examples:
         * <pre>
         *   RTGS - VALID
         *   UPI  - INVALID → [ING-007] UPI max ₹1,00,000
         * </pre>
         */
        @Override
        public String toString() {
            return passed
                    ? sourceType + " - VALID"
                    : sourceType + " - INVALID → " + getErrorSummary();
        }
    }
}
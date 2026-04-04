package com.iispl.validation;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

public final class PayloadValidator {

    private static final BigDecimal RTGS_MINIMUM = new BigDecimal("200000.00");
    private static final BigDecimal UPI_MAXIMUM  = new BigDecimal("100000.00");
    private static final BigDecimal NEFT_MAXIMUM = new BigDecimal("1000000.00");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final Set<String> VALID_CURRENCIES = new HashSet<>(Arrays.asList(
            "INR","USD","GBP","EUR","JPY","AUD","CAD","SGD","AED","CHF","HKD","SEK","NOK","DKK"
    ));

    private static final Pattern IFSC_PATTERN =
            Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    private static final Pattern BIC_PATTERN =
            Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");

    private PayloadValidator() {}

    // ─────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────
    public static ValidationResult validate(IncomingTransaction txn, SourceType sourceType) {

        List<String> errors = new ArrayList<>();

        validateStructural(txn, errors);

        if (errors.isEmpty())
            validateBusinessRules(txn, sourceType, errors);

        if (errors.isEmpty())
            validateCrossField(txn, errors);

        return new ValidationResult(errors.isEmpty(), errors, sourceType);
    }

    // ─────────────────────────────────────────
    // LEVEL 1 — STRUCTURAL
    // ─────────────────────────────────────────
    private static void validateStructural(IncomingTransaction txn, List<String> errors) {

        if (isBlank(txn.getSourceRef()))
            errors.add("[ING-002] sourceRef is mandatory");

        if (txn.getTxnType() == null)
            errors.add("[ING-002] txnType is mandatory");

        if (txn.getAmount() == null)
            errors.add("[ING-002] amount is mandatory");
        else if (txn.getAmount().compareTo(ZERO) <= 0)
            errors.add("[ING-003] amount must be > 0");

        if (isBlank(txn.getCurrency()))
            errors.add("[ING-002] currency is mandatory");
        else if (txn.getCurrency().length() != 3)
            errors.add("[ING-003] currency must be 3 chars");
        else if (!VALID_CURRENCIES.contains(txn.getCurrency().toUpperCase()))
            errors.add("[ING-004] invalid currency: " + txn.getCurrency());

        // 🔥 NOW TIMESTAMP (NOT DATE)
        if (txn.getValueDate() == null)
            errors.add("[ING-002] txnTimestamp is mandatory");

        if (txn.getSourceSystem() == null)
            errors.add("[ING-002] sourceSystem required");

        if (isBlank(txn.getChannelCode()))
            errors.add("[ING-002] channelCode required");

        if (txn.getTxnStatus() == null)
            errors.add("[ING-002] txnStatus is mandatory");
    }

     
    private static void validateBusinessRules(IncomingTransaction txn,
                                              SourceType sourceType,
                                              List<String> errors) {

        BigDecimal amount = txn.getAmount();

        switch (sourceType) {

            case RTGS:
                if (amount.compareTo(RTGS_MINIMUM) < 0)
                    errors.add("[ING-004] RTGS minimum ₹2,00,000");

                validateIfsc(txn.getSenderIfsc(), "senderIfsc", errors);
                validateIfsc(txn.getReceiverIfsc(), "receiverIfsc", errors);

                if (!isBlank(txn.getSourceRef()) && txn.getSourceRef().length() != 16)
                    errors.add("[ING-003] RTGS UTR must be 16 chars");
                break;

            case UPI:
                if (amount.compareTo(UPI_MAXIMUM) > 0)
                    errors.add("[ING-007] UPI max ₹1,00,000");

                validateIfsc(txn.getSenderIfsc(), "senderIfsc", errors);
                validateIfsc(txn.getReceiverIfsc(), "receiverIfsc", errors);
                break;

            case NEFT:
                if (amount.compareTo(NEFT_MAXIMUM) > 0)
                    errors.add("[ING-007] NEFT max ₹10,00,000");

                validateIfsc(txn.getSenderIfsc(), "senderIfsc", errors);
                validateIfsc(txn.getReceiverIfsc(), "receiverIfsc", errors);
                break;

            case SWIFT:
                validateBic(txn.getSenderBic(), "senderBic", errors);
                validateBic(txn.getReceiverBic(), "receiverBic", errors);
                break;

            case FINTECH:
                if (txn.getGrossAmount() != null && txn.getFeeAmount() != null) {
                    BigDecimal expected =
                            txn.getGrossAmount().subtract(txn.getFeeAmount());

                    if (expected.compareTo(txn.getAmount()) != 0)
                        errors.add("[ING-003] net mismatch");
                }

                if (isBlank(txn.getPartnerName()))
                    errors.add("[ING-002] partnerName required");
                break;

            case CBS:
                validateIfsc(txn.getSenderIfsc(), "senderIfsc", errors);
                validateIfsc(txn.getReceiverIfsc(), "receiverIfsc", errors);
                break;

            default:
                break;
        }
    }

    // ─────────────────────────────────────────
    // LEVEL 3 — CROSS FIELD
    // ─────────────────────────────────────────
    private static void validateCrossField(IncomingTransaction txn, List<String> errors) {

        if (!isBlank(txn.getSenderIfsc())
                && txn.getSenderIfsc().equalsIgnoreCase(txn.getReceiverIfsc()))
            errors.add("[ING-004] sender & receiver cannot be same");

        // FIXED FOR TIMESTAMP
        if (txn.getValueDate() != null) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

            if (txn.getValueDate().isBefore(cutoff))
                errors.add("[ING-004] txnTimestamp too old");
        }
    }

    // ─────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────
    private static void validateIfsc(String ifsc, String name, List<String> errors) {
        if (!isBlank(ifsc) && !IFSC_PATTERN.matcher(ifsc).matches())
            errors.add("[ING-003] invalid " + name);
    }

    private static void validateBic(String bic, String name, List<String> errors) {
        if (!isBlank(bic) && !BIC_PATTERN.matcher(bic).matches())
            errors.add("[ING-003] invalid " + name);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ─────────────────────────────────────────
    // RESULT CLASS
    // ─────────────────────────────────────────
    public static final class ValidationResult {

        private final boolean passed;
        private final List<String> errors;
        private final SourceType sourceType;

        ValidationResult(boolean passed, List<String> errors, SourceType sourceType) {
            this.passed = passed;
            this.errors = errors;
            this.sourceType = sourceType;
        }

        public boolean isPassed() { return passed; }
        public List<String> getErrors() { return errors; }

        public String getErrorSummary() {
            return String.join(" | ", errors);
        }

        @Override
        public String toString() {
            return passed
                    ? "" + sourceType + " - VALID"
                    : "" + sourceType + " - INVALID → " + getErrorSummary();
        }
    }
}
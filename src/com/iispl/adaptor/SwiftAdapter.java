package com.iispl.adaptor;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;
import com.iispl.exception.IngestionException;
import com.iispl.intefaces.TransactionAdapter;
import com.iispl.validation.PayloadValidator;

/**
 * SWIFT Adapter — Immutable pipeline design.
 *
 * <p>
 * Responsible for parsing SWIFT MT-style tagged messages and transforming them
 * into a canonical {@link IncomingTransaction}.
 * </p>
 *
 * <p>
 * Expected format: Key-value tags such as:
 * <pre>
 * :20:REFERENCE
 * :32A:YYYY-MM-DD HH:MM:SS CUR AMOUNT
 * </pre>
 * </p>
 */
public class SwiftAdapter implements TransactionAdapter {

    /** Static source system reference for SWIFT */
    private static final SourceSystem SWIFT_SOURCE_SYSTEM = SourceSystem.SWIFT();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ─────────────────────────────────────────────────────────────
        // 1. INPUT VALIDATION (Guard Clause)
        // ─────────────────────────────────────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                IngestionException.ERR_NULL_PAYLOAD,
                SourceType.SWIFT,
                rawPayload,
                "rawPayload is null or empty"
            );
        }

        // ─────────────────────────────────────────────────────────────
        // 2. FIELD EXTRACTION FROM SWIFT TAGS
        // ─────────────────────────────────────────────────────────────
        String transactionReferenceNumber = extractTagValue(rawPayload, ":20:");
        String senderCustomerId           = extractTagValue(rawPayload, ":SENDER_CID:");
        String receiverCustomerId         = extractTagValue(rawPayload, ":RECEIVER_CID:");
        String senderAccountNumber        = extractTagValue(rawPayload, ":SENDER_ACC:");
        String receiverAccountNumber      = extractTagValue(rawPayload, ":RECEIVER_ACC:");

        // ─────────────────────────────────────────────────────────────
        // 3. PARSE :32A: FIELD (TIMESTAMP + CURRENCY + AMOUNT)
        // Format → YYYY-MM-DD HH:MM:SS CUR AMOUNT
        // ─────────────────────────────────────────────────────────────
        String field32AValue = extractTagValue(rawPayload, ":32A:");

        LocalDateTime transactionTimestamp;
        BigDecimal transactionAmount;
        String transactionCurrency;

        try {
            String[] fieldComponents = field32AValue.split(" ");

            String dateComponent = fieldComponents[0];
            String timeComponent = fieldComponents[1];

            transactionTimestamp = LocalDateTime.parse(dateComponent + "T" + timeComponent);
            transactionCurrency  = fieldComponents[2];
            transactionAmount    = new BigDecimal(fieldComponents[3]);

        } catch (Exception exception) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT,
                SourceType.SWIFT,
                rawPayload,
                "Invalid :32A: format → " + field32AValue
            );
        }

        // ─────────────────────────────────────────────────────────────
        // 4. TRANSACTION STATUS RESOLUTION
        // Default → SUCCESS (if not present)
        // ─────────────────────────────────────────────────────────────
        String transactionStatusRaw = safeExtractTagValue(rawPayload, ":STATUS:");

        TransactionStatus transactionStatus = transactionStatusRaw.isEmpty()
                ? TransactionStatus.SUCCESS
                : TransactionStatus.valueOf(transactionStatusRaw.toUpperCase());

        // ─────────────────────────────────────────────────────────────
        // 5. NORMALIZED PAYLOAD GENERATION
        // ─────────────────────────────────────────────────────────────
        String normalizedPayload = buildNormalizedPayload(
                transactionReferenceNumber,
                senderCustomerId,
                senderAccountNumber,
                receiverCustomerId,
                receiverAccountNumber,
                transactionAmount,
                transactionCurrency,
                transactionTimestamp,
                transactionStatus
        );

        // ─────────────────────────────────────────────────────────────
        // 6. BUILD IMMUTABLE TRANSACTION OBJECT
        // ─────────────────────────────────────────────────────────────
        IncomingTransaction incomingTransaction = new IncomingTransaction.Builder()
                .sourceSystem(SWIFT_SOURCE_SYSTEM)
                .channelCode("SWIFT")
                .rawPayload(rawPayload)
                .checksum(generateSha256Checksum(rawPayload))
                .createdBy("SWIFT-ADAPTER")
                .priority(3)

                .sourceRef(transactionReferenceNumber)

                .senderCustomerId(senderCustomerId)
                .receiverCustomerId(receiverCustomerId)

                .senderAccount(senderAccountNumber)
                .receiverAccount(receiverAccountNumber)

                // SWIFT uses BIC instead of IFSC
                .senderBic(senderAccountNumber)
                .receiverBic(receiverAccountNumber)

                .senderBankName(BankNameResolver.fromBic(senderAccountNumber))
                .receiverBankName(BankNameResolver.fromBic(receiverAccountNumber))

                .valueDate(transactionTimestamp)
                .currency(transactionCurrency)

                .amount(transactionAmount)
                .grossAmount(transactionAmount)
                .feeAmount(BigDecimal.ZERO)

                .txnType(TransactionType.CREDIT)
                .txnStatus(transactionStatus)

                .normalizedPayload(normalizedPayload)
                .build();

        // ─────────────────────────────────────────────────────────────
        // 7. VALIDATION + IMMUTABLE TRANSFORMATION
        // ─────────────────────────────────────────────────────────────
        PayloadValidator.ValidationResult validationResult =
                PayloadValidator.validate(incomingTransaction, SourceType.SWIFT);

        IncomingTransaction finalTransaction;

        if (!validationResult.isPassed()) {

            finalTransaction = incomingTransaction.toBuilder()
                    .processingStatus(ProcessingStatus.FAILED)
                    .errorMessage(validationResult.getErrorSummary())
                    .build();

        } else {

            ProcessingStatus processingStatus = ProcessingStatus.VALIDATED;

            if (incomingTransaction.getTxnStatus() == TransactionStatus.SUCCESS) {
                processingStatus = ProcessingStatus.QUEUED;
                SWIFT_SOURCE_SYSTEM.recordSuccess();
            }

            finalTransaction = incomingTransaction.toBuilder()
                    .processingStatus(processingStatus)
                    .build();
        }

        // ─────────────────────────────────────────────────────────────
        // 8. LOGGING
        // ─────────────────────────────────────────────────────────────
        logAdapterOutput(finalTransaction);

        return finalTransaction;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.SWIFT;
    }

    // ─────────────────────────────────────────────────────────────
    // NORMALIZED PAYLOAD BUILDER
    // ─────────────────────────────────────────────────────────────
    private String buildNormalizedPayload(
            String transactionReferenceNumber,
            String senderCustomerId,
            String senderAccountNumber,
            String receiverCustomerId,
            String receiverAccountNumber,
            BigDecimal transactionAmount,
            String transactionCurrency,
            LocalDateTime transactionTimestamp,
            TransactionStatus transactionStatus) {

        return "{"
             + "\"txn_id\":\"" + transactionReferenceNumber + "\","
             + "\"channel\":\"SWIFT\","
             + "\"txn_type\":\"CREDIT\","
             + "\"sender_cid\":\"" + senderCustomerId + "\","
             + "\"sender_acc\":\"" + senderAccountNumber + "\","
             + "\"receiver_cid\":\"" + receiverCustomerId + "\","
             + "\"receiver_acc\":\"" + receiverAccountNumber + "\","
             + "\"amount\":" + transactionAmount + ","
             + "\"currency\":\"" + transactionCurrency + "\","
             + "\"txn_timestamp\":\"" + transactionTimestamp + "\","
             + "\"status\":\"" + transactionStatus + "\""
             + "}";
    }

    // ─────────────────────────────────────────────────────────────
    // TAG EXTRACTION UTILITIES
    // ─────────────────────────────────────────────────────────────
    private String extractTagValue(String payload, String tagIdentifier) {

        int tagStartIndex = payload.indexOf(tagIdentifier);

        if (tagStartIndex == -1) {
            throw new IngestionException(
                IngestionException.ERR_MISSING_FIELD,
                SourceType.SWIFT,
                payload,
                "Missing tag: " + tagIdentifier
            );
        }

        int valueStartIndex = tagStartIndex + tagIdentifier.length();
        int valueEndIndex   = payload.indexOf("\n", valueStartIndex);

        return payload.substring(
                valueStartIndex,
                valueEndIndex == -1 ? payload.length() : valueEndIndex
        ).trim();
    }

    private String safeExtractTagValue(String payload, String tagIdentifier) {
        try {
            return extractTagValue(payload, tagIdentifier);
        } catch (Exception exception) {
            return "";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // LOGGING UTILITIES
    // ─────────────────────────────────────────────────────────────
    private void logAdapterOutput(IncomingTransaction transaction) {

        System.out.printf(
            "[ADAPTER ][%-18s][%-7s] REF=%-22s | AMT=%12s %-3s | STATUS=%-8s/%-10s%n",
            Thread.currentThread().getName(),
            safeValue(transaction.getChannelCode()),
            safeValue(transaction.getSourceRef()),
            formatAmount(transaction.getAmount()),
            safeValue(transaction.getCurrency()),
            safeValue(transaction.getTxnStatus()),
            safeValue(transaction.getProcessingStatus())
        );
    }

    private static String safeValue(Object value) {
        return value == null ? "N/A" : value.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : String.format("%,.2f", amount);
    }

    // ─────────────────────────────────────────────────────────────
    // CHECKSUM GENERATION
    // ─────────────────────────────────────────────────────────────
    private static String generateSha256Checksum(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(input.getBytes("UTF-8"));

            StringBuilder hexBuilder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                hexBuilder.append(String.format("%02x", hashByte));
            }

            return hexBuilder.toString();

        } catch (Exception exception) {
            return "CHECKSUM-ERROR";
        }
    }
}
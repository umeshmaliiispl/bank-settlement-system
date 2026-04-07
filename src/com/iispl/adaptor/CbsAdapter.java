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
 * CBS Adapter — Immutable ingestion pipeline implementation.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Parse raw CBS payload</li>
 *   <li>Transform into canonical IncomingTransaction</li>
 *   <li>Perform validation</li>
 *   <li>Return immutable final transaction object</li>
 * </ul>
 *
 * <p>
 * Design Principles:
 * <ul>
 *   <li>Immutable object creation using Builder pattern</li>
 *   <li>No in-place mutation (uses toBuilder for transformations)</li>
 *   <li>Fail-fast validation and strict input checks</li>
 * </ul>
 */
public class CbsAdapter implements TransactionAdapter {

    /** Constant reference for CBS source system */
    private static final SourceSystem cbsSourceSystem = SourceSystem.CBS();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ── Input Validation (Fail-Fast Guard) ───────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.CBS,
                    rawPayload,
                    "rawPayload is null or empty"
            );
        }

        // Split payload into fields (retain empty values)
        String[] payloadFields = rawPayload.split("\\|", -1);

        if (payloadFields.length < 10) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.CBS,
                    rawPayload,
                    "Expected minimum 10 fields, got: " + payloadFields.length
            );
        }

        // ── Timestamp Parsing ────────────────────────────────────────────────
        LocalDateTime transactionTimestamp;
        try {
            transactionTimestamp = LocalDateTime.parse(
                    safeTrim(payloadFields[7]).replace(" ", "T")
            );
        } catch (Exception exception) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.CBS,
                    rawPayload,
                    "Invalid timestamp: " + payloadFields[7]
            );
        }

        // ── Extract Core Transaction Data ────────────────────────────────────
        String senderAccountNumber = safeTrim(payloadFields[2]);
        String receiverAccountNumber = safeTrim(payloadFields[4]);
        BigDecimal transactionAmount = new BigDecimal(safeTrim(payloadFields[5]));

        String normalizedPayloadJson = buildNormalizedPayload(
                safeTrim(payloadFields[8]),
                safeTrim(payloadFields[0]),
                transactionAmount,
                safeTrim(payloadFields[6])
        );

        // ── Build Immutable Transaction Object ───────────────────────────────
        IncomingTransaction initialTransaction = new IncomingTransaction.Builder()
                .sourceSystem(cbsSourceSystem)
                .channelCode("CBS")
                .rawPayload(rawPayload)
                .checksum(generateSha256Checksum(rawPayload))
                .createdBy("CBS-ADAPTER")

                .txnType(TransactionType.valueOf(safeTrim(payloadFields[0]).toUpperCase()))

                .senderCustomerId(safeTrim(payloadFields[1]))
                .senderAccount(senderAccountNumber)
                .senderIfsc(senderAccountNumber) // CBS uses account as IFSC reference

                .receiverCustomerId(safeTrim(payloadFields[3]))
                .receiverAccount(receiverAccountNumber)
                .receiverIfsc(receiverAccountNumber)

                .amount(transactionAmount)
                .grossAmount(transactionAmount)
                .feeAmount(BigDecimal.ZERO)

                .currency(safeTrim(payloadFields[6]).toUpperCase())
                .valueDate(transactionTimestamp)

                .sourceRef(safeTrim(payloadFields[8]))
                .txnStatus(TransactionStatus.valueOf(safeTrim(payloadFields[9]).toUpperCase()))

                .senderBankName(BankNameResolver.fromIfsc(senderAccountNumber))
                .receiverBankName(BankNameResolver.fromIfsc(receiverAccountNumber))

                .priority(5)
                .normalizedPayload(normalizedPayloadJson)
                .build();

        // ── Validation Phase (Immutable Transformation) ──────────────────────
        PayloadValidator.ValidationResult validationResult =
                PayloadValidator.validate(initialTransaction, SourceType.CBS);

        IncomingTransaction finalTransaction;

        if (!validationResult.isPassed()) {

            // Validation failed → mark transaction as FAILED
            finalTransaction = initialTransaction.toBuilder()
                    .processingStatus(ProcessingStatus.FAILED)
                    .errorMessage(validationResult.getErrorSummary())
                    .build();

        } else {

            // Validation passed → determine processing status
            ProcessingStatus processingStatus = ProcessingStatus.VALIDATED;

            if (initialTransaction.getTxnStatus() == TransactionStatus.SUCCESS) {
                processingStatus = ProcessingStatus.QUEUED;
                cbsSourceSystem.recordSuccess();
            }

            finalTransaction = initialTransaction.toBuilder()
                    .processingStatus(processingStatus)
                    .build();
        }

        // ── Logging ─────────────────────────────────────────────────────────
        logTransaction(finalTransaction);

        return finalTransaction;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.CBS;
    }

    // ── Helper Methods ──────────────────────────────────────────────────────

    /**
     * Builds normalized JSON payload representation.
     */
    private String buildNormalizedPayload(String transactionReference,
                                          String transactionType,
                                          BigDecimal transactionAmount,
                                          String currencyCode) {

        return "{"
                + "\"txn_id\":\"" + transactionReference + "\","
                + "\"channel\":\"CBS\","
                + "\"txn_type\":\"" + transactionType + "\","
                + "\"amount\":" + transactionAmount + ","
                + "\"currency\":\"" + currencyCode + "\""
                + "}";
    }

    /**
     * Logs transaction details in structured format.
     */
    private void logTransaction(IncomingTransaction transaction) {
        System.out.printf(
                "[ADAPTER ][%-18s][%-7s] REF=%-22s | AMT=%12s %-3s | STATUS=%-8s/%-10s%n",
                Thread.currentThread().getName(),
                safeToString(transaction.getChannelCode()),
                safeToString(transaction.getSourceRef()),
                formatAmount(transaction.getAmount()),
                safeToString(transaction.getCurrency()),
                safeToString(transaction.getTxnStatus()),
                safeToString(transaction.getProcessingStatus())
        );
    }

    /**
     * Safely trims string (null-safe).
     */
    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Converts object to safe string representation.
     */
    private static String safeToString(Object value) {
        return value == null ? "N/A" : value.toString();
    }

    /**
     * Formats amount to standard currency representation.
     */
    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : String.format("%,.2f", amount);
    }

    /**
     * Generates SHA-256 checksum for payload integrity.
     */
    private static String generateSha256Checksum(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(input.getBytes("UTF-8"));

            StringBuilder hexStringBuilder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                hexStringBuilder.append(String.format("%02x", hashByte));
            }

            return hexStringBuilder.toString();

        } catch (Exception exception) {
            return "CHECKSUM-ERROR";
        }
    }
}
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
 * NEFT / UPI Adapter — Unified immutable ingestion pipeline.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Parse CSV-based payload</li>
 *   <li>Auto-detect source type (NEFT or UPI)</li>
 *   <li>Transform into canonical IncomingTransaction</li>
 *   <li>Validate and return immutable result</li>
 * </ul>
 *
 * <p>
 * Note:
 * A single adapter handles both NEFT and UPI formats, identified using
 * source reference prefix ("UPI-").
 */
public class NeftUpiAdapter implements TransactionAdapter {

    /** Source system references */
    private static final SourceSystem neftSourceSystem = SourceSystem.NEFT();
    private static final SourceSystem upiSourceSystem  = SourceSystem.UPI();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ── Input Validation (Fail-Fast Guard) ───────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.NEFT,
                    rawPayload,
                    "rawPayload is null or empty"
            );
        }

        // ── Parse CSV Payload ────────────────────────────────────────────────
        String[] payloadFields = rawPayload.split(",", -1);

        boolean isUpiTransaction =
                payloadFields[0].toUpperCase().startsWith("UPI-");

        SourceType detectedSourceType =
                isUpiTransaction ? SourceType.UPI : SourceType.NEFT;

        if (payloadFields.length < 10) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    detectedSourceType,
                    rawPayload,
                    "Expected 10 CSV fields, got: " + payloadFields.length
            );
        }

        // ── Timestamp Parsing ────────────────────────────────────────────────
        LocalDateTime transactionTimestamp;
        try {
            transactionTimestamp = LocalDateTime.parse(
                    safeTrim(payloadFields[8]).replace(" ", "T")
            );
        } catch (Exception exception) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    detectedSourceType,
                    rawPayload,
                    "Invalid timestamp: " + payloadFields[8]
            );
        }

        // ── Extract Core Transaction Fields ──────────────────────────────────
        String senderAccountNumber   = safeTrim(payloadFields[3]);
        String receiverAccountNumber = safeTrim(payloadFields[5]);

        BigDecimal transactionAmount =
                new BigDecimal(safeTrim(payloadFields[6]));

        String channelCode      = isUpiTransaction ? "UPI" : "NEFT";
        String transactionRef   = safeTrim(payloadFields[0]);
        String currencyCode     = safeTrim(payloadFields[7]).toUpperCase();

        TransactionType transactionType =
                TransactionType.valueOf(
                        safeTrim(payloadFields[1]).toUpperCase()
                );

        TransactionStatus transactionStatus =
                TransactionStatus.valueOf(
                        safeTrim(payloadFields[9]).toUpperCase()
                );

        // ── Normalized Payload Construction ──────────────────────────────────
        String normalizedPayloadJson = buildNormalizedPayload(
                transactionRef,
                channelCode,
                transactionType,
                safeTrim(payloadFields[2]),
                senderAccountNumber,
                safeTrim(payloadFields[4]),
                receiverAccountNumber,
                transactionAmount,
                currencyCode,
                transactionTimestamp,
                transactionStatus
        );

        // ── Build Immutable Transaction Object ───────────────────────────────
        IncomingTransaction initialTransaction = new IncomingTransaction.Builder()
                .sourceSystem(isUpiTransaction ? upiSourceSystem : neftSourceSystem)
                .channelCode(channelCode)
                .rawPayload(rawPayload)
                .checksum(generateSha256Checksum(rawPayload))
                .createdBy(isUpiTransaction ? "UPI-ADAPTER" : "NEFT-ADAPTER")

                .sourceRef(transactionRef)
                .txnType(transactionType)

                .senderCustomerId(safeTrim(payloadFields[2]))
                .senderAccount(senderAccountNumber)
                .senderIfsc(senderAccountNumber)

                .receiverCustomerId(safeTrim(payloadFields[4]))
                .receiverAccount(receiverAccountNumber)
                .receiverIfsc(receiverAccountNumber)

                .amount(transactionAmount)
                .grossAmount(transactionAmount)
                .feeAmount(BigDecimal.ZERO)

                .currency(currencyCode)
                .valueDate(transactionTimestamp)

                .txnStatus(transactionStatus)

                .senderBankName(BankNameResolver.fromIfsc(senderAccountNumber))
                .receiverBankName(BankNameResolver.fromIfsc(receiverAccountNumber))

                .priority(isUpiTransaction ? 6 : 4)
                .normalizedPayload(normalizedPayloadJson)
                .build();

        // ── Validation Phase ─────────────────────────────────────────────────
        PayloadValidator.ValidationResult validationResult =
                PayloadValidator.validate(initialTransaction, detectedSourceType);

        IncomingTransaction finalTransaction;

        if (!validationResult.isPassed()) {

            System.err.println("  [" + channelCode + "-ADAPTER][FAIL] " + validationResult);

            finalTransaction = initialTransaction.toBuilder()
                    .processingStatus(ProcessingStatus.FAILED)
                    .errorMessage(validationResult.getErrorSummary())
                    .build();

        } else {

            ProcessingStatus processingStatus = ProcessingStatus.VALIDATED;

            if (initialTransaction.getTxnStatus() == TransactionStatus.SUCCESS) {
                processingStatus = ProcessingStatus.QUEUED;

                if (isUpiTransaction) {
                    upiSourceSystem.recordSuccess();
                } else {
                    neftSourceSystem.recordSuccess();
                }
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
        return SourceType.NEFT;
    }

    // ── Normalized Payload Builder ──────────────────────────────────────────
    private String buildNormalizedPayload(
            String transactionReference,
            String channelCode,
            TransactionType transactionType,
            String senderCustomerId,
            String senderAccountNumber,
            String receiverCustomerId,
            String receiverAccountNumber,
            BigDecimal transactionAmount,
            String currencyCode,
            LocalDateTime transactionTimestamp,
            TransactionStatus transactionStatus) {

        return "{"
                + "\"txn_id\":\"" + transactionReference + "\","
                + "\"channel\":\"" + channelCode + "\","
                + "\"txn_type\":\"" + transactionType + "\","
                + "\"sender_cid\":\"" + senderCustomerId + "\","
                + "\"sender_acc\":\"" + senderAccountNumber + "\","
                + "\"receiver_cid\":\"" + receiverCustomerId + "\","
                + "\"receiver_acc\":\"" + receiverAccountNumber + "\","
                + "\"amount\":" + transactionAmount + ","
                + "\"currency\":\"" + currencyCode + "\","
                + "\"txn_timestamp\":\"" + transactionTimestamp + "\","
                + "\"status\":\"" + transactionStatus + "\""
                + "}";
    }

    // ── Logging ─────────────────────────────────────────────────────────────
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

    // ── Helper Methods ──────────────────────────────────────────────────────
    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeToString(Object value) {
        return value == null ? "N/A" : value.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : String.format("%,.2f", amount);
    }

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
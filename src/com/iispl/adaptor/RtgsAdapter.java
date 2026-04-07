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
 * RTGS Adapter — Immutable ingestion pipeline implementation.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Parse RTGS JSON payload</li>
 *   <li>Apply RTGS-specific business rules (minimum amount)</li>
 *   <li>Transform into canonical IncomingTransaction</li>
 *   <li>Validate and return immutable result</li>
 * </ul>
 *
 * <p>
 * Business Rule:
 * RTGS transactions must be >= ₹2,00,000.
 */
public class RtgsAdapter implements TransactionAdapter {

    /** Source system reference for RTGS */
    private static final SourceSystem rtgsSourceSystem = SourceSystem.RTGS();

    /** Minimum allowed RTGS transaction amount */
    private static final BigDecimal minimumRtgsAmount = new BigDecimal("200000");

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ── Input Validation (Fail-Fast Guard) ───────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.RTGS,
                    rawPayload,
                    "rawPayload is null or empty"
            );
        }

        // ── Extract Core Fields from Payload ─────────────────────────────────
        String transactionReference   = extractValue(rawPayload, "utr");
        String senderCustomerId       = extractValue(rawPayload, "senderCid");
        String receiverCustomerId     = extractValue(rawPayload, "receiverCid");

        String senderAccountNumber    = extractValue(rawPayload, "senderAcc");
        String receiverAccountNumber  = extractValue(rawPayload, "receiverAcc");

        String senderIfscCode         = extractValue(rawPayload, "senderIFSC");
        String receiverIfscCode       = extractValue(rawPayload, "receiverIFSC");

        BigDecimal transactionAmount  =
                new BigDecimal(extractValue(rawPayload, "amount"));

        String currencyCode           = extractValue(rawPayload, "currency");

        // ── Timestamp Parsing ────────────────────────────────────────────────
        String timestampString = extractValue(rawPayload, "txnTimestamp");

        LocalDateTime transactionTimestamp;
        try {
            transactionTimestamp = LocalDateTime.parse(
                    timestampString.replace(" ", "T")
            );
        } catch (Exception exception) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.RTGS,
                    rawPayload,
                    "Invalid txnTimestamp: " + timestampString
            );
        }

        // ── Derived Transaction Attributes ───────────────────────────────────
        String messageType = extractValue(rawPayload, "msgType");

        TransactionType transactionType =
                messageType.contains("CREDIT")
                        ? TransactionType.CREDIT
                        : TransactionType.DEBIT;

        String transactionStatusString =
                safeExtractValue(rawPayload, "status");

        TransactionStatus transactionStatus =
                transactionStatusString.isEmpty()
                        ? TransactionStatus.SUCCESS
                        : TransactionStatus.valueOf(
                                transactionStatusString.toUpperCase()
                        );

        // ── RTGS Business Rule Enforcement ───────────────────────────────────
        if (transactionAmount.compareTo(minimumRtgsAmount) < 0) {
            throw new IngestionException(
                    IngestionException.ERR_BUSINESS_RULE,
                    SourceType.RTGS,
                    rawPayload,
                    "RTGS amount below minimum limit (200000)"
            );
        }

        // ── Normalized Payload Construction ──────────────────────────────────
        String normalizedPayloadJson = buildNormalizedPayload(
                transactionReference,
                transactionType,
                senderCustomerId,
                senderAccountNumber,
                receiverCustomerId,
                receiverAccountNumber,
                transactionAmount,
                currencyCode,
                transactionTimestamp,
                transactionStatus
        );

        // ── Build Immutable Transaction Object ───────────────────────────────
        IncomingTransaction initialTransaction = new IncomingTransaction.Builder()
                .sourceSystem(rtgsSourceSystem)
                .channelCode("RTGS")
                .rawPayload(rawPayload)
                .checksum(generateSha256Checksum(rawPayload))
                .createdBy("RTGS-ADAPTER")
                .priority(1)

                .sourceRef(transactionReference)

                .senderCustomerId(senderCustomerId)
                .receiverCustomerId(receiverCustomerId)

                .senderAccount(senderAccountNumber)
                .receiverAccount(receiverAccountNumber)

                .senderIfsc(senderIfscCode)
                .receiverIfsc(receiverIfscCode)

                .amount(transactionAmount)
                .grossAmount(transactionAmount)
                .feeAmount(BigDecimal.ZERO)

                .currency(currencyCode)
                .valueDate(transactionTimestamp)

                .txnType(transactionType)
                .txnStatus(transactionStatus)

                .senderBankName(BankNameResolver.fromIfsc(senderIfscCode))
                .receiverBankName(BankNameResolver.fromIfsc(receiverIfscCode))

                .normalizedPayload(normalizedPayloadJson)
                .build();

        // ── Validation Phase ─────────────────────────────────────────────────
        PayloadValidator.ValidationResult validationResult =
                PayloadValidator.validate(initialTransaction, SourceType.RTGS);

        IncomingTransaction finalTransaction;

        if (!validationResult.isPassed()) {

            finalTransaction = initialTransaction.toBuilder()
                    .processingStatus(ProcessingStatus.FAILED)
                    .errorMessage(validationResult.getErrorSummary())
                    .build();

        } else {

            ProcessingStatus processingStatus = ProcessingStatus.VALIDATED;

            if (initialTransaction.getTxnStatus() == TransactionStatus.SUCCESS) {
                processingStatus = ProcessingStatus.QUEUED;
                rtgsSourceSystem.recordSuccess();
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
        return SourceType.RTGS;
    }

    // ── Normalized Payload Builder ──────────────────────────────────────────
    private String buildNormalizedPayload(
            String transactionReference,
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
                + "\"channel\":\"RTGS\","
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

    // ── Payload Extractors ──────────────────────────────────────────────────
    private String extractValue(String jsonPayload, String fieldKey) {
        String searchKey = "\"" + fieldKey + "\"";

        int keyIndex = jsonPayload.indexOf(searchKey);
        if (keyIndex == -1) {
            throw new IngestionException(
                    IngestionException.ERR_MISSING_FIELD,
                    SourceType.RTGS,
                    jsonPayload,
                    "Missing field: " + fieldKey
            );
        }

        int valueStartIndex = jsonPayload.indexOf("\"", keyIndex + searchKey.length() + 1);
        int valueEndIndex   = jsonPayload.indexOf("\"", valueStartIndex + 1);

        return jsonPayload.substring(valueStartIndex + 1, valueEndIndex);
    }

    private String safeExtractValue(String jsonPayload, String fieldKey) {
        try {
            return extractValue(jsonPayload, fieldKey);
        } catch (Exception exception) {
            return "";
        }
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
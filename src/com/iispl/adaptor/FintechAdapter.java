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
 * Fintech Adapter — Immutable, production-ready ingestion pipeline.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Parse JSON-based fintech payload</li>
 *   <li>Extract and normalize transaction data</li>
 *   <li>Apply business transformations (fees, partner detection, IFSC derivation)</li>
 *   <li>Validate and produce immutable transaction output</li>
 * </ul>
 */
public class FintechAdapter implements TransactionAdapter {

    /** Constant reference for FINTECH source system */
    private static final SourceSystem fintechSourceSystem = SourceSystem.FINTECH();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ── Input Validation (Fail-Fast Guard) ───────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.FINTECH,
                    rawPayload,
                    "Payload is null or empty"
            );
        }

        // ── Extract Core Fields from Payload ─────────────────────────────────
        String senderAccountNumber   = extractValue(rawPayload, "sender_acc_no");
        String receiverAccountNumber = extractValue(rawPayload, "receiver_acc_no");

        BigDecimal grossTransactionAmount =
                new BigDecimal(extractValue(rawPayload, "amount"));

        String remarksText = safeExtractValue(rawPayload, "remarks");

        // ── Fee & Net Amount Calculation ─────────────────────────────────────
        BigDecimal feeAmount = extractFeeAmount(remarksText);
        BigDecimal netTransactionAmount = grossTransactionAmount.subtract(feeAmount);

        // ── Partner Identification ───────────────────────────────────────────
        String partnerName = safeExtractValue(rawPayload, "partner_name");
        if (partnerName == null || partnerName.trim().isEmpty()) {
            partnerName = derivePartnerNameFromRemarks(remarksText);
        }

        // ── Timestamp Parsing ────────────────────────────────────────────────
        String timestampString = extractValue(rawPayload, "txn_timestamp");

        LocalDateTime transactionTimestamp;
        try {
            transactionTimestamp = LocalDateTime.parse(
                    timestampString.replace(" ", "T")
            );
        } catch (Exception exception) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.FINTECH,
                    rawPayload,
                    "Invalid txn_timestamp: " + timestampString
            );
        }

        // ── Core Transaction Metadata ────────────────────────────────────────
        String transactionReference = extractValue(rawPayload, "txn_id");
        String currencyCode         = extractValue(rawPayload, "currency");

        TransactionType transactionType =
                TransactionType.valueOf(
                        extractValue(rawPayload, "txn_type").toUpperCase()
                );

        TransactionStatus transactionStatus =
                TransactionStatus.valueOf(
                        extractValue(rawPayload, "status").toUpperCase()
                );

        // ── IFSC Derivation (Bank Identification) ────────────────────────────
        String senderIfscCode   = deriveIfscCode(senderAccountNumber);
        String receiverIfscCode = deriveIfscCode(receiverAccountNumber);

        // ── Normalized Payload Construction ──────────────────────────────────
        String normalizedPayloadJson = buildNormalizedPayload(
                transactionReference,
                transactionType,
                extractValue(rawPayload, "sender_cid"),
                senderAccountNumber,
                extractValue(rawPayload, "receiver_cid"),
                receiverAccountNumber,
                grossTransactionAmount,
                feeAmount,
                netTransactionAmount,
                currencyCode,
                transactionTimestamp,
                transactionStatus,
                partnerName
        );

        // ── Build Immutable Transaction Object ───────────────────────────────
        IncomingTransaction initialTransaction = new IncomingTransaction.Builder()
                .sourceSystem(fintechSourceSystem)
                .channelCode("FINTECH")
                .rawPayload(rawPayload)
                .checksum(generateSha256Checksum(rawPayload))
                .createdBy("FINTECH-ADAPTER")
                .priority(7)

                .sourceRef(transactionReference)
                .txnType(transactionType)

                .senderCustomerId(extractValue(rawPayload, "sender_cid"))
                .senderAccount(senderAccountNumber)
                .senderIfsc(senderIfscCode)

                .receiverCustomerId(extractValue(rawPayload, "receiver_cid"))
                .receiverAccount(receiverAccountNumber)
                .receiverIfsc(receiverIfscCode)

                .senderBankName(BankNameResolver.fromIfsc(senderIfscCode))
                .receiverBankName(BankNameResolver.fromIfsc(receiverIfscCode))

                .grossAmount(grossTransactionAmount)
                .feeAmount(feeAmount)
                .amount(netTransactionAmount)

                .currency(currencyCode)
                .valueDate(transactionTimestamp)

                .txnStatus(transactionStatus)

                .partnerName(partnerName)
                .merchantId(safeExtractValue(rawPayload, "merchant_id"))

                .normalizedPayload(normalizedPayloadJson)
                .build();

        // ── Validation Phase ─────────────────────────────────────────────────
        PayloadValidator.ValidationResult validationResult =
                PayloadValidator.validate(initialTransaction, SourceType.FINTECH);

        IncomingTransaction finalTransaction;

        if (!validationResult.isPassed()) {

            finalTransaction = initialTransaction.toBuilder()
                    .processingStatus(ProcessingStatus.FAILED)
                    .errorMessage(validationResult.getErrorSummary())
                    .build();

        } else if (initialTransaction.getTxnStatus() == TransactionStatus.SUCCESS) {

            finalTransaction = initialTransaction.toBuilder()
                    .processingStatus(ProcessingStatus.QUEUED)
                    .build();

            fintechSourceSystem.recordSuccess();

        } else {

            finalTransaction = initialTransaction.toBuilder()
                    .processingStatus(ProcessingStatus.FAILED)
                    .errorMessage("Transaction not successful at source")
                    .build();
        }

        // ── Logging ─────────────────────────────────────────────────────────
        logTransaction(finalTransaction);

        return finalTransaction;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.FINTECH;
    }

    // ── Normalized Payload Builder ──────────────────────────────────────────
    private String buildNormalizedPayload(
            String transactionReference,
            TransactionType transactionType,
            String senderCustomerId,
            String senderAccountNumber,
            String receiverCustomerId,
            String receiverAccountNumber,
            BigDecimal grossAmount,
            BigDecimal feeAmount,
            BigDecimal netAmount,
            String currencyCode,
            LocalDateTime transactionTimestamp,
            TransactionStatus transactionStatus,
            String partnerName) {

        return "{"
                + "\"txn_id\":\"" + transactionReference + "\","
                + "\"channel\":\"FINTECH\","
                + "\"txn_type\":\"" + transactionType + "\","
                + "\"sender_cid\":\"" + senderCustomerId + "\","
                + "\"sender_acc\":\"" + senderAccountNumber + "\","
                + "\"receiver_cid\":\"" + receiverCustomerId + "\","
                + "\"receiver_acc\":\"" + receiverAccountNumber + "\","
                + "\"gross_amount\":" + grossAmount + ","
                + "\"fee_amount\":" + feeAmount + ","
                + "\"net_amount\":" + netAmount + ","
                + "\"currency\":\"" + currencyCode + "\","
                + "\"txn_timestamp\":\"" + transactionTimestamp + "\","
                + "\"status\":\"" + transactionStatus + "\","
                + "\"partner_name\":\"" + partnerName + "\""
                + "}";
    }

    // ── Payload Extractors ──────────────────────────────────────────────────
    private String extractValue(String jsonPayload, String fieldKey) {
        String searchKey = "\"" + fieldKey + "\"";

        int keyIndex = jsonPayload.indexOf(searchKey);
        if (keyIndex == -1) {
            throw new IngestionException(
                    IngestionException.ERR_MISSING_FIELD,
                    SourceType.FINTECH,
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

    // ── Fee Extraction Logic ────────────────────────────────────────────────
    private BigDecimal extractFeeAmount(String remarksText) {
        try {
            if (remarksText != null) {
                String[] remarkParts = remarksText.split(":");
                if (remarkParts.length > 1) {
                    return new BigDecimal(
                            remarkParts[1].replaceAll("[^0-9.]", "")
                    );
                }
            }
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
    }

    // ── Partner Detection Logic ─────────────────────────────────────────────
    private String derivePartnerNameFromRemarks(String remarksText) {
        if (remarksText == null) return "";

        String normalizedRemarks = remarksText.toLowerCase();

        if (normalizedRemarks.contains("phonepe")) return "PHONEPE";
        if (normalizedRemarks.contains("googlepay") || normalizedRemarks.contains("gpay")) return "GPAY";
        if (normalizedRemarks.contains("paytm")) return "PAYTM";

        return "";
    }

    // ── IFSC Derivation Logic ───────────────────────────────────────────────
    
 // ── IFSC Derivation Logic ───────────────────────────────────────────────
    /**
     * Derives a representative IFSC code from a Fintech account number.
     *
     * <p>Fintech payloads carry account numbers (not raw IFSC codes), so the
     * IFSC is inferred from the account number prefix — which mirrors the
     * bank's standard IFSC prefix convention.
     *
     * <p>This is used solely for bank-name resolution and reporting;
     * it is not used for actual settlement routing.
     *
     * @param accountNumber the sender or receiver account number
     * @return a representative IFSC code, or "UNKN0000000" if unrecognized
     */
    private String deriveIfscCode(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return "UNKN0000000";
        }

        String acc = accountNumber.trim().toUpperCase();

        // ── Public Sector Banks ─────────────────────────────────────────────
        if (acc.startsWith("SBIN"))  return "SBIN0000001";   // State Bank of India
        if (acc.startsWith("BKID"))  return "BKID0000001";   // Bank of India
        if (acc.startsWith("BARB"))  return "BARB0000001";   // Bank of Baroda
        if (acc.startsWith("PUNB"))  return "PUNB0000001";   // Punjab National Bank
        if (acc.startsWith("CNRB"))  return "CNRB0000001";   // Canara Bank
        if (acc.startsWith("UBIN"))  return "UBIN0000001";   // Union Bank of India
        if (acc.startsWith("IOBA"))  return "IOBA0000001";   // Indian Overseas Bank
        if (acc.startsWith("IDIB"))  return "IDIB0000001";   // Indian Bank
        if (acc.startsWith("CBIN"))  return "CBIN0000001";   // Central Bank of India
        if (acc.startsWith("UCBA"))  return "UCBA0000001";   // UCO Bank
        if (acc.startsWith("BODB"))  return "BODB0000001";   // Bank of Maharashtra
        if (acc.startsWith("PSIB"))  return "PSIB0000001";   // Punjab & Sind Bank

        // ── Major Private Sector Banks ──────────────────────────────────────
        if (acc.startsWith("HDFC"))  return "HDFC0000001";   // HDFC Bank
        if (acc.startsWith("ICIC"))  return "ICIC0000001";   // ICICI Bank
        if (acc.startsWith("AXIS"))  return "UTIB0000001";   // Axis Bank (UTIB prefix)
        if (acc.startsWith("UTIB"))  return "UTIB0000001";   // Axis Bank (alternate)
        if (acc.startsWith("KKBK"))  return "KKBK0000001";   // Kotak Mahindra Bank
        if (acc.startsWith("YESB"))  return "YESB0000001";   // Yes Bank
        if (acc.startsWith("INDB"))  return "INDB0000001";   // IndusInd Bank
        if (acc.startsWith("IDFB"))  return "IDFB0000001";   // IDFC FIRST Bank
        if (acc.startsWith("FDRL"))  return "FDRL0000001";   // Federal Bank
        if (acc.startsWith("KVBL"))  return "KVBL0000001";   // Karur Vysya Bank
        if (acc.startsWith("SIBL"))  return "SIBL0000001";   // South Indian Bank
        if (acc.startsWith("DBSS"))  return "DBSS0000001";   // DBS Bank India
        if (acc.startsWith("DCBL"))  return "DCBL0000001";   // DCB Bank
        if (acc.startsWith("KARB"))  return "KARB0000001";   // Karnataka Bank
        if (acc.startsWith("JAKA"))  return "JAKA0000001";   // J&K Bank
        if (acc.startsWith("CSBK"))  return "CSBK0000001";   // CSB Bank
        if (acc.startsWith("RBLB"))  return "RBLB0000001";   // RBL Bank
        if (acc.startsWith("RATN"))  return "RATN0000001";   // RBL Bank (alternate)
        if (acc.startsWith("TMBL"))  return "TMBL0000001";   // Tamilnad Mercantile
        if (acc.startsWith("AUBL"))  return "AUBL0000001";   // AU Small Finance Bank
        if (acc.startsWith("ESFB"))  return "ESFB0000001";   // Equitas Small Finance
        if (acc.startsWith("UJVN"))  return "UFSB0000001";   // Ujjivan Small Finance
        if (acc.startsWith("UFSB"))  return "UFSB0000001";   // Ujjivan Small Finance

        // ── Payment Banks ───────────────────────────────────────────────────
        if (acc.startsWith("AIRP"))  return "AIRP0000001";   // Airtel Payments Bank
        if (acc.startsWith("FINO"))  return "FINO0000001";   // Fino Payments Bank
        if (acc.startsWith("IPOS"))  return "IPOS0000001";   // India Post Payments Bank
        if (acc.startsWith("PAYT"))  return "PAYT0000001";   // Paytm Payments Bank

        // ── Foreign Banks ───────────────────────────────────────────────────
        if (acc.startsWith("CITI"))  return "CITI0000001";   // Citibank India
        if (acc.startsWith("HSBC"))  return "HSBC0000001";   // HSBC India
        if (acc.startsWith("DEUT"))  return "DEUT0000001";   // Deutsche Bank India
        if (acc.startsWith("SCBL"))  return "SCBL0000001";   // Standard Chartered India
        if (acc.startsWith("BARC"))  return "BARC0000001";   // Barclays India
        if (acc.startsWith("BOFA"))  return "BOFA0000001";   // Bank of America India
        if (acc.startsWith("JPMO"))  return "JPMO0000001";   // JPMorgan Chase India
        if (acc.startsWith("MHCB"))  return "MHCB0000001";   // Mizuho Bank India
        if (acc.startsWith("SMBC"))  return "SMBC0000001";   // SMBC India
        if (acc.startsWith("ANZB"))  return "ANZB0000001";   // ANZ Bank India
        if (acc.startsWith("BNPP"))  return "BNPP0000001";   // BNP Paribas India

        // ── RBI / Internal ──────────────────────────────────────────────────
        if (acc.startsWith("RBIS"))  return "RBIS0000001";   // Reserve Bank of India
        if (acc.startsWith("IISPL")) return "IISP0000001";   // IISPL Internal account
        if (acc.startsWith("IISP"))  return "IISP0000001";   // IISPL Internal account

        // ── Unrecognized ────────────────────────────────────────────────────
        return "UNKN0000000";
    }
    
//    private String deriveIfscCode(String accountNumber) {
//        if (accountNumber == null) return "";
//
//        if (accountNumber.startsWith("HDFC")) return "HDFC0000001";
//        if (accountNumber.startsWith("AXIS")) return "UTIB0000001";
//        if (accountNumber.startsWith("ICIC")) return "ICIC0000001";
//
//        return "UNKN0000000";
//    }
//    
    

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
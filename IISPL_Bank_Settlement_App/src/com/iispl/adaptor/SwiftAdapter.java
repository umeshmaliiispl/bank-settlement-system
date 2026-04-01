package com.iispl.adaptor;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.*;
import com.iispl.exception.IngestionException;
import com.iispl.intefaces.TransactionAdapter;
import com.iispl.validation.PayloadValidator;

public class SwiftAdapter implements TransactionAdapter {

    private static final SourceSystem SWIFT_SOURCE = SourceSystem.SWIFT();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty())
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.SWIFT,
                    rawPayload,
                    "rawPayload is null or empty");

        IncomingTransaction txn = new IncomingTransaction();

        txn.setSourceSystem(SWIFT_SOURCE);
        txn.setChannelCode("SWIFT");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("SWIFT-ADAPTER");
        txn.setPriority(3);
        txn.setTxnType(TransactionType.CREDIT);

        // ── Mandatory Tags ─────────────────────
        txn.setSourceRef(extractTag(rawPayload, ":20:"));

        String field32A = extractTag(rawPayload, ":32A:");

        if (field32A.length() < 10)
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.SWIFT,
                    rawPayload,
                    "Invalid :32A: format");

        String yymmdd = field32A.substring(0, 6);
        String currency = field32A.substring(6, 9);
        String amount = field32A.substring(9).trim();

        txn.setCurrency(currency);

        try {
            txn.setAmount(new BigDecimal(amount));
            txn.setGrossAmount(txn.getAmount());
            txn.setFeeAmount(BigDecimal.ZERO);
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.SWIFT,
                    rawPayload,
                    "Invalid amount in :32A:");
        }

        txn.setValueDate(parseSwiftDate(yymmdd));

        // ── Optional Fields ───────────────────
        txn.setSenderIfsc(safeTag(rawPayload, ":52A:"));
        txn.setReceiverIfsc(safeTag(rawPayload, ":57A:"));
        txn.setSenderBankName(BankNameResolver.fromBic(txn.getSenderIfsc()));
        txn.setReceiverBankName(BankNameResolver.fromBic(txn.getReceiverIfsc()));

        txn.setNormalizedPayload(buildNormalized(txn));

        // 🔥 NEW: STATUS (MANDATORY FOR YOUR SYSTEM)
        String status = safeTag(rawPayload, ":STATUS:");

        try {
            txn.setTxnStatus(
                    status.isEmpty()
                            ? TransactionStatus.SUCCESS   // default fallback
                            : TransactionStatus.valueOf(status.toUpperCase())
            );
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.SWIFT,
                    rawPayload,
                    "Invalid STATUS: " + status);
        }

        // ── Validation ───────────────────────
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, SourceType.SWIFT);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
        } else {
            txn.setProcessingStatus(
                    txn.getTxnStatus() == TransactionStatus.SUCCESS
                            ? ProcessingStatus.QUEUED
                            : ProcessingStatus.VALIDATED
            );
            SWIFT_SOURCE.recordSuccess();
        }

        System.out.println("  [SWIFT-ADAPTER] " + txn.toAuditString());

        return txn;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.SWIFT;
    }

    // ───────────────────────── HELPERS ─────────────────────────

    private String extractTag(String payload, String tag) {
        int i = payload.indexOf(tag);
        if (i == -1)
            throw new IngestionException(
                    IngestionException.ERR_MISSING_FIELD,
                    SourceType.SWIFT,
                    payload,
                    "Missing tag: " + tag);

        int start = i + tag.length();
        int end = payload.indexOf("\n", start);

        return payload.substring(start, end == -1 ? payload.length() : end).trim();
    }

    private String safeTag(String payload, String tag) {
        try {
            return extractTag(payload, tag);
        } catch (Exception e) {
            return "";
        }
    }

    private LocalDate parseSwiftDate(String yymmdd) {
        int yy = Integer.parseInt(yymmdd.substring(0, 2));
        int mm = Integer.parseInt(yymmdd.substring(2, 4));
        int dd = Integer.parseInt(yymmdd.substring(4, 6));
        int yyyy = (yy < 50) ? 2000 + yy : 1900 + yy;
        return LocalDate.of(yyyy, mm, dd);
    }

    private String buildNormalized(IncomingTransaction txn) {
        return "{"
                + "\"source\":\"SWIFT\","
                + "\"ref\":\"" + txn.getSourceRef() + "\","
                + "\"amount\":" + txn.getAmount() + ","
                + "\"currency\":\"" + txn.getCurrency() + "\","
                + "\"valueDate\":\"" + txn.getValueDate() + "\","
                + "\"status\":\"" + txn.getTxnStatus() + "\""
                + "}";
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
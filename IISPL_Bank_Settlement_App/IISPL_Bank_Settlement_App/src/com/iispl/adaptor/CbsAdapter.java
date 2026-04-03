package com.iispl.adaptor;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;
import com.iispl.exception.IngestionException;
import com.iispl.intefaces.TransactionAdapter;
import com.iispl.validation.PayloadValidator;

public class CbsAdapter implements TransactionAdapter {

    private static final SourceSystem CBS_SOURCE = SourceSystem.CBS();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ── Guard ─────────────────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty())
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.CBS,
                    rawPayload,
                    "rawPayload is null or empty");

        // ── Parse pipe-delimited (NOW 8 fields) ───────────
        String[] f = rawPayload.split("\\|", -1);

        if (f.length < 8) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.CBS,  
                    rawPayload,
                    "Expected 8 pipe-delimited fields (including status), got: " + f.length
                            + " | Payload: " + rawPayload
            );
        }

        IncomingTransaction txn = new IncomingTransaction();

        // ── Source Info ───────────────────────────────────
        txn.setSourceSystem(CBS_SOURCE);
        txn.setChannelCode("CBS");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("CBS-ADAPTER");

        // ── Core Mapping ──────────────────────────────────
        try {
            txn.setTxnType(TransactionType.valueOf(trim(f[0]).toUpperCase()));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.CBS,
                    rawPayload,
                    "Invalid TXN_TYPE: " + f[0]);
        }

        txn.setSenderIfsc(trim(f[1]).toUpperCase());
        txn.setReceiverIfsc(trim(f[2]).toUpperCase());

        try {
            txn.setAmount(new BigDecimal(trim(f[3])));
            txn.setGrossAmount(txn.getAmount());
            txn.setFeeAmount(BigDecimal.ZERO);
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.CBS,
                    rawPayload,
                    "Invalid amount: " + f[3]);
        }

        txn.setCurrency(trim(f[4]).toUpperCase());

        try {
            txn.setValueDate(LocalDate.parse(trim(f[5])));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.CBS,
                    rawPayload,
                    "Invalid date: " + f[5]);
        }

        txn.setSourceRef(trim(f[6]));

        // 🔥 NEW: Transaction Status
        try {
            txn.setTxnStatus(TransactionStatus.valueOf(trim(f[7]).toUpperCase()));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.CBS,
                    rawPayload,
                    "Invalid txn status: " + f[7]);
        }

        // ── Bank Names ────────────────────────────────────
        txn.setSenderBankName(BankNameResolver.fromIfsc(txn.getSenderIfsc()));
        txn.setReceiverBankName(BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        // ── Priority ──────────────────────────────────────
        txn.setPriority(5);

        // ── Normalized Payload ────────────────────────────
        txn.setNormalizedPayload(buildNormalized(txn));

        // ── Validation ────────────────────────────────────
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, SourceType.CBS);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
            System.err.println("  [CBS-ADAPTER][FAIL] " + vr);
        } else {
            txn.setProcessingStatus(ProcessingStatus.VALIDATED);

            // 🔥 IMPORTANT: Only SUCCESS goes forward
            if (txn.getTxnStatus() == TransactionStatus.SUCCESS) {
                txn.setProcessingStatus(ProcessingStatus.QUEUED);
                CBS_SOURCE.recordSuccess();
            }
        }

        System.out.println("  [CBS-ADAPTER] " + txn.toAuditString());
        return txn;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.CBS;
    }

    // ─────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────

    private String buildNormalized(IncomingTransaction txn) {
        return "{"
                + "\"source\":\"CBS\","
                + "\"ref\":\"" + txn.getSourceRef() + "\","
                + "\"type\":\"" + txn.getTxnType() + "\","
                + "\"senderIfsc\":\"" + txn.getSenderIfsc() + "\","
                + "\"receiverIfsc\":\"" + txn.getReceiverIfsc() + "\","
                + "\"amount\":" + txn.getAmount() + ","
                + "\"currency\":\"" + txn.getCurrency() + "\","
                + "\"valueDate\":\"" + txn.getValueDate() + "\","
                + "\"status\":\"" + txn.getTxnStatus() + "\""
                + "}";
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
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

public class NeftUpiAdapter implements TransactionAdapter {

    private static final SourceSystem NEFT_SOURCE = SourceSystem.NEFT();
    private static final SourceSystem UPI_SOURCE = SourceSystem.UPI();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ── Guard ─────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.NEFT,
                    rawPayload,
                    "rawPayload is null or empty"
            );
        }

        String[] f = rawPayload.split(",", -1);

        boolean isUpi = f[0].toUpperCase().startsWith("UPI-");
        SourceType sourceType = isUpi ? SourceType.UPI : SourceType.NEFT;

        // ── Format check (8 columns) ──────────
        if (f.length < 8) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    sourceType,
                    rawPayload,
                    "Expected 8 CSV fields (including status), got: " + f.length
            );
        }

        IncomingTransaction txn = new IncomingTransaction();

        // ── Source Info ───────────────────────
        txn.setSourceSystem(isUpi ? UPI_SOURCE : NEFT_SOURCE);
        txn.setChannelCode(isUpi ? "UPI" : "NEFT");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy(isUpi ? "UPI-ADAPTER" : "NEFT-ADAPTER");

        // ── Core Mapping ─────────────────────
        txn.setSourceRef(trim(f[0]));

        try {
            txn.setTxnType(TransactionType.valueOf(trim(f[1]).toUpperCase()));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    sourceType,
                    rawPayload,
                    "Invalid TXN_TYPE: " + f[1]
            );
        }

        txn.setSenderIfsc(trim(f[2]).toUpperCase());
        txn.setReceiverIfsc(trim(f[3]).toUpperCase());

        try {
            txn.setAmount(new BigDecimal(trim(f[4])));
            txn.setGrossAmount(txn.getAmount());
            txn.setFeeAmount(BigDecimal.ZERO);
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    sourceType,
                    rawPayload,
                    "Invalid AMOUNT: " + f[4]
            );
        }

        txn.setCurrency(trim(f[5]).toUpperCase());

        try {
            txn.setValueDate(LocalDate.parse(trim(f[6])));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    sourceType,
                    rawPayload,
                    "Invalid VALUE_DATE: " + f[6]
            );
        }

        // 🔥 STATUS (SAFE)
        try {
            txn.setTxnStatus(
                    TransactionStatus.valueOf(trim(f[7]).toUpperCase())
            );
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    sourceType,
                    rawPayload,
                    "Invalid STATUS: " + f[7]
            );
        }

        // ── Bank Names ───────────────────────
        txn.setSenderBankName(BankNameResolver.fromIfsc(txn.getSenderIfsc()));
        txn.setReceiverBankName(BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        // ── Validation ───────────────────────
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, sourceType);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
            System.err.println("  [" + txn.getChannelCode() + "-ADAPTER][FAIL] " + vr);
        } else {
            txn.setProcessingStatus(
                    txn.getTxnStatus() == TransactionStatus.SUCCESS
                            ? ProcessingStatus.QUEUED
                            : ProcessingStatus.VALIDATED
            );
        }

        // 🔥 Audit log
        System.out.println("  [" + txn.getChannelCode() + "-ADAPTER] " + txn.toAuditString());

        return txn;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.NEFT;
    }

    // ─────────────────────────────
    // HELPERS
    // ─────────────────────────────

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
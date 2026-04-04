package com.iispl.adaptor;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.*;
import com.iispl.exception.IngestionException;
import com.iispl.intefaces.TransactionAdapter;
import com.iispl.validation.PayloadValidator;

public class FintechAdapter implements TransactionAdapter {

    private static final SourceSystem FINTECH_SOURCE = SourceSystem.FINTECH();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ── Guard ─────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.FINTECH,
                    rawPayload,
                    "Payload is null or empty"
            );
        }

        IncomingTransaction txn = new IncomingTransaction();

        // ── Source Info ───────────────────────
        txn.setSourceSystem(FINTECH_SOURCE);
        txn.setChannelCode("FINTECH");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("FINTECH-ADAPTER");

        txn.setPriority(7);

        // ── Core Mapping ─────────────────────

        txn.setSourceRef(extract(rawPayload, "txn_id"));

        txn.setTxnType(
                TransactionType.valueOf(extract(rawPayload, "txn_type").toUpperCase())
        );

        txn.setSenderCustomerId(extract(rawPayload, "sender_cid"));
        txn.setSenderAccount(extract(rawPayload, "sender_acc_no"));

        txn.setReceiverCustomerId(extract(rawPayload, "receiver_cid"));
        txn.setReceiverAccount(extract(rawPayload, "receiver_acc_no"));

        txn.setSenderIfsc(txn.getSenderAccount());
        txn.setReceiverIfsc(txn.getReceiverAccount());

        txn.setSenderBankName(
                BankNameResolver.fromIfsc(txn.getSenderIfsc()));
        txn.setReceiverBankName(
                BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        // ── Amount + Fee Logic ───────────────
        BigDecimal amount = new BigDecimal(extract(rawPayload, "amount"));

        String remarks = safeExtract(rawPayload, "remarks");
        BigDecimal fee = extractFee(remarks);

        txn.setGrossAmount(amount);
        txn.setFeeAmount(fee);
        txn.setAmount(amount.subtract(fee));

        txn.setCurrency(extract(rawPayload, "currency"));

        // FIXED: STORE FULL TIMESTAMP (NO toLocalDate)
        String ts = extract(rawPayload, "txn_timestamp");

        LocalDateTime txnTime;
        try {
            txnTime = LocalDateTime.parse(ts.replace(" ", "T"));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.FINTECH,
                    rawPayload,
                    "Invalid txn_timestamp: " + ts
            );
        }

        txn.setValueDate(txnTime); 

        // 🔥 STATUS
        txn.setTxnStatus(
                TransactionStatus.valueOf(extract(rawPayload, "status").toUpperCase())
        );

        // ── Partner Info ─────────────────────
        txn.setPartnerName(safeExtract(rawPayload, "partner_name"));
        txn.setMerchantId(safeExtract(rawPayload, "merchant_id"));

        // ── Normalized Payload ───────────────
        txn.setNormalizedPayload(buildNormalized(txn));

        // ── Validation ───────────────────────
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, SourceType.FINTECH);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
        } else {
            txn.setProcessingStatus(ProcessingStatus.VALIDATED);

            if (txn.getTxnStatus() == TransactionStatus.SUCCESS) {
                txn.setProcessingStatus(ProcessingStatus.QUEUED);
                FINTECH_SOURCE.recordSuccess();
            }
        }

        // ── Audit Log ────────────────────────
        System.out.printf(
        	    "[ADAPTER ][%-18s][%-7s] REF=%-22s | AMT=%12s %-3s | STATUS=%-8s/%-10s%n",
        	    Thread.currentThread().getName(),
        	    safe(txn.getChannelCode()),
        	    safe(txn.getSourceRef()),
        	    formatAmount(txn.getAmount()),
        	    safe(txn.getCurrency()),
        	    safe(txn.getTxnStatus()),
        	    safe(txn.getProcessingStatus())
        	);
        return txn;
    }
    

	private String formatAmount(java.math.BigDecimal amt) {
		if (amt == null)
			return "0.00";
		return String.format("%,.2f", amt);
	}

	private String safe(Object val) {
		return val == null ? "N/A" : val.toString();
	}

    @Override
    public SourceType getSourceType() {
        return SourceType.FINTECH;
    }

    // ─────────────────────────────
    // NORMALIZED FORMAT
    // ─────────────────────────────
    private String buildNormalized(IncomingTransaction txn) {
        return "{"
                + "\"txn_id\":\"" + txn.getSourceRef() + "\","
                + "\"channel\":\"FINTECH\","
                + "\"txn_type\":\"" + txn.getTxnType() + "\","
                + "\"sender_cid\":\"" + txn.getSenderCustomerId() + "\","
                + "\"sender_acc\":\"" + txn.getSenderAccount() + "\","
                + "\"receiver_cid\":\"" + txn.getReceiverCustomerId() + "\","
                + "\"receiver_acc\":\"" + txn.getReceiverAccount() + "\","
                + "\"gross_amount\":" + txn.getGrossAmount() + ","
                + "\"fee_amount\":" + txn.getFeeAmount() + ","
                + "\"net_amount\":" + txn.getAmount() + ","
                + "\"currency\":\"" + txn.getCurrency() + "\","
                + "\"txn_timestamp\":\"" + txn.getValueDate() + "\","
                + "\"status\":\"" + txn.getTxnStatus() + "\""
                + "}";
    }

    private String extract(String json, String key) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);

        if (i == -1) {
            throw new IngestionException(
                    IngestionException.ERR_MISSING_FIELD,
                    SourceType.FINTECH,
                    json,
                    "Missing field: " + key
            );
        }

        int start = json.indexOf("\"", i + k.length() + 1);
        int end = json.indexOf("\"", start + 1);

        return json.substring(start + 1, end);
    }

    private String safeExtract(String json, String key) {
        try {
            return extract(json, key);
        } catch (Exception e) {
            return "";
        }
    }

    private BigDecimal extractFee(String remarks) {
        try {
            if (remarks != null && remarks.contains(":")) {
                String feeStr = remarks.split(":")[1].trim();
                return new BigDecimal(feeStr);
            }
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
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
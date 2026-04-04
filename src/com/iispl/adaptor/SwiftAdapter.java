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

public class SwiftAdapter implements TransactionAdapter {

    private static final SourceSystem SWIFT_SOURCE = SourceSystem.SWIFT();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ─────────────────────────────────────────
        // 1. GUARD
        // ─────────────────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.SWIFT,
                    rawPayload,
                    "rawPayload is null or empty"
            );
        }

        IncomingTransaction txn = new IncomingTransaction();

        // ─────────────────────────────────────────
        // 2. SOURCE INFO
        // ─────────────────────────────────────────
        txn.setSourceSystem(SWIFT_SOURCE);
        txn.setChannelCode("SWIFT");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("SWIFT-ADAPTER");

        // 🔥 MEDIUM PRIORITY
        txn.setPriority(3);

        // ─────────────────────────────────────────
        // 3. CORE MAPPING
        // ─────────────────────────────────────────
        txn.setSourceRef(extractTag(rawPayload, ":20:"));

        txn.setSenderCustomerId(extractTag(rawPayload, ":SENDER_CID:"));
        txn.setReceiverCustomerId(extractTag(rawPayload, ":RECEIVER_CID:"));

        txn.setSenderAccount(extractTag(rawPayload, ":SENDER_ACC:"));
        txn.setReceiverAccount(extractTag(rawPayload, ":RECEIVER_ACC:"));

        // SWIFT uses BIC
        txn.setSenderBic(txn.getSenderAccount());
        txn.setReceiverBic(txn.getReceiverAccount());

        txn.setSenderBankName(
                BankNameResolver.fromBic(txn.getSenderBic()));
        txn.setReceiverBankName(
                BankNameResolver.fromBic(txn.getReceiverBic()));

        // ─────────────────────────────────────────
        // 🔥 4. 32A PARSING (MAIN FIX)
        // ─────────────────────────────────────────
        // Format: YYYY-MM-DD HH:MM:SS CUR AMOUNT
        String field32A = extractTag(rawPayload, ":32A:");

        try {
            String[] parts = field32A.split(" ");

            String date = parts[0];
            String time = parts[1];

            LocalDateTime ts = LocalDateTime.parse(date + "T" + time);

            // ✅ STORE FULL TIMESTAMP
            txn.setValueDate(ts);

            txn.setCurrency(parts[2]);
            txn.setAmount(new BigDecimal(parts[3]));

            txn.setGrossAmount(txn.getAmount());
            txn.setFeeAmount(BigDecimal.ZERO);

        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.SWIFT,
                    rawPayload,
                    "Invalid :32A: format → " + field32A
            );
        }

        // ─────────────────────────────────────────
        // 5. TXN TYPE
        // ─────────────────────────────────────────
        txn.setTxnType(TransactionType.CREDIT);

        // ─────────────────────────────────────────
        // 6. STATUS
        // ─────────────────────────────────────────
        String status = safeTag(rawPayload, ":STATUS:");

        txn.setTxnStatus(
                status.isEmpty()
                        ? TransactionStatus.SUCCESS
                        : TransactionStatus.valueOf(status.toUpperCase())
        );

        // ─────────────────────────────────────────
        // 7. NORMALIZED PAYLOAD
        // ─────────────────────────────────────────
        txn.setNormalizedPayload(buildNormalized(txn));

        // ─────────────────────────────────────────
        // 8. VALIDATION
        // ─────────────────────────────────────────
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, SourceType.SWIFT);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
        } else {
            txn.setProcessingStatus(ProcessingStatus.VALIDATED);

            if (txn.getTxnStatus() == TransactionStatus.SUCCESS) {
                txn.setProcessingStatus(ProcessingStatus.QUEUED);
                SWIFT_SOURCE.recordSuccess();
            }
        }

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
        return SourceType.SWIFT;
    }

    // ─────────────────────────────────────────
    // NORMALIZED FORMAT
    // ─────────────────────────────────────────
    private String buildNormalized(IncomingTransaction txn) {
        return "{"
                + "\"txn_id\":\"" + txn.getSourceRef() + "\","
                + "\"channel\":\"SWIFT\","
                + "\"txn_type\":\"" + txn.getTxnType() + "\","
                + "\"sender_cid\":\"" + txn.getSenderCustomerId() + "\","
                + "\"sender_acc\":\"" + txn.getSenderAccount() + "\","
                + "\"receiver_cid\":\"" + txn.getReceiverCustomerId() + "\","
                + "\"receiver_acc\":\"" + txn.getReceiverAccount() + "\","
                + "\"amount\":" + txn.getAmount() + ","
                + "\"currency\":\"" + txn.getCurrency() + "\","
                + "\"txn_timestamp\":\"" + txn.getValueDate() + "\","
                + "\"status\":\"" + txn.getTxnStatus() + "\""
                + "}";
    }

    // ─────────────────────────────────────────
    // TAG EXTRACTOR
    // ─────────────────────────────────────────
    private String extractTag(String payload, String tag) {

        int i = payload.indexOf(tag);

        if (i == -1) {
            throw new IngestionException(
                    IngestionException.ERR_MISSING_FIELD,
                    SourceType.SWIFT,
                    payload,
                    "Missing tag: " + tag
            );
        }

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

    // ─────────────────────────────────────────
    // CHECKSUM
    // ─────────────────────────────────────────
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
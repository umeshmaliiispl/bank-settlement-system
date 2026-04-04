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

public class RtgsAdapter implements TransactionAdapter {

    private static final SourceSystem RTGS_SOURCE = SourceSystem.RTGS();
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("200000");

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ─────────────────────────────────────────
        // 1. GUARD
        // ─────────────────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                    IngestionException.ERR_NULL_PAYLOAD,
                    SourceType.RTGS,
                    rawPayload,
                    "rawPayload is null or empty"
            );
        }

        IncomingTransaction txn = new IncomingTransaction();

        // ─────────────────────────────────────────
        // 2. SOURCE INFO
        // ─────────────────────────────────────────
        txn.setSourceSystem(RTGS_SOURCE);
        txn.setChannelCode("RTGS");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("RTGS-ADAPTER");

        // 🔥 HIGH PRIORITY
        txn.setPriority(1);

        // ─────────────────────────────────────────
        // 3. CORE MAPPING
        // ─────────────────────────────────────────

        txn.setSourceRef(extract(rawPayload, "utr"));

        txn.setSenderCustomerId(extract(rawPayload, "senderCid"));
        txn.setReceiverCustomerId(extract(rawPayload, "receiverCid"));

        txn.setSenderAccount(extract(rawPayload, "senderAcc"));
        txn.setReceiverAccount(extract(rawPayload, "receiverAcc"));

        txn.setSenderIfsc(extract(rawPayload, "senderIFSC"));
        txn.setReceiverIfsc(extract(rawPayload, "receiverIFSC"));

        // ─────────────────────────────────────────
        // 4. AMOUNT
        // ─────────────────────────────────────────
        txn.setAmount(new BigDecimal(extract(rawPayload, "amount")));
        txn.setGrossAmount(txn.getAmount());
        txn.setFeeAmount(BigDecimal.ZERO);

        txn.setCurrency(extract(rawPayload, "currency"));

        
        String tsStr = extract(rawPayload, "txnTimestamp");

        LocalDateTime ts;
        try {
            ts = LocalDateTime.parse(tsStr.replace(" ", "T"));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    SourceType.RTGS,
                    rawPayload,
                    "Invalid txnTimestamp: " + tsStr
            );
        }

         txn.setValueDate(ts);

       
        String msgType = extract(rawPayload, "msgType");

        txn.setTxnType(
                msgType.contains("CREDIT")
                        ? TransactionType.CREDIT
                        : TransactionType.DEBIT
        );
 
        String status = safeExtract(rawPayload, "status");

        txn.setTxnStatus(
                status.isEmpty()
                        ? TransactionStatus.SUCCESS
                        : TransactionStatus.valueOf(status.toUpperCase())
        );

      
        txn.setSenderBankName(
                BankNameResolver.fromIfsc(txn.getSenderIfsc()));

        txn.setReceiverBankName(
                BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        
        if (txn.getAmount().compareTo(MIN_AMOUNT) < 0) {
            throw new IngestionException(
                    IngestionException.ERR_BUSINESS_RULE,
                    SourceType.RTGS,
                    rawPayload,
                    "RTGS amount below minimum limit (200000)"
            );
        }

       
        txn.setNormalizedPayload(buildNormalized(txn));

        // ─────────────────────────────────────────
        // 11. VALIDATION
        // ─────────────────────────────────────────
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, SourceType.RTGS);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
        } else {
            txn.setProcessingStatus(ProcessingStatus.VALIDATED);

            if (txn.getTxnStatus() == TransactionStatus.SUCCESS) {
                txn.setProcessingStatus(ProcessingStatus.QUEUED);
                RTGS_SOURCE.recordSuccess();
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
        return SourceType.RTGS;
    }

    // ─────────────────────────────────────────
    // NORMALIZED JSON
    // ─────────────────────────────────────────
    private String buildNormalized(IncomingTransaction txn) {
        return "{"
                + "\"txn_id\":\"" + txn.getSourceRef() + "\","
                + "\"channel\":\"RTGS\","
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
    // JSON EXTRACTOR
    // ─────────────────────────────────────────
    private String extract(String json, String key) {

        String k = "\"" + key + "\"";
        int i = json.indexOf(k);

        if (i == -1) {
            throw new IngestionException(
                    IngestionException.ERR_MISSING_FIELD,
                    SourceType.RTGS,
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
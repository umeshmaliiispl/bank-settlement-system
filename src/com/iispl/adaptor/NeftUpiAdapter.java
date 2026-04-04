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

public class NeftUpiAdapter implements TransactionAdapter {

    private static final SourceSystem NEFT_SOURCE = SourceSystem.NEFT();
    private static final SourceSystem UPI_SOURCE  = SourceSystem.UPI();

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

        // ── Parse CSV ─────────────────────────
        String[] f = rawPayload.split(",", -1);

        boolean isUpi = f[0].toUpperCase().startsWith("UPI-");
        SourceType sourceType = isUpi ? SourceType.UPI : SourceType.NEFT;

        if (f.length < 10) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    sourceType,
                    rawPayload,
                    "Expected 10 CSV fields, got: " + f.length
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

        txn.setTxnType(
                TransactionType.valueOf(trim(f[1]).toUpperCase())
        );

        txn.setSenderCustomerId(trim(f[2]));
        txn.setSenderAccount(trim(f[3]));

        txn.setReceiverCustomerId(trim(f[4]));
        txn.setReceiverAccount(trim(f[5]));

        txn.setSenderIfsc(txn.getSenderAccount());
        txn.setReceiverIfsc(txn.getReceiverAccount());

        // ── Amount ───────────────────────────
        txn.setAmount(new BigDecimal(trim(f[6])));
        txn.setGrossAmount(txn.getAmount());
        txn.setFeeAmount(BigDecimal.ZERO);

        txn.setCurrency(trim(f[7]).toUpperCase());

        // 🔥 FIXED: STORE FULL TIMESTAMP
        LocalDateTime txnTime;

        try {
            txnTime = LocalDateTime.parse(trim(f[8]).replace(" ", "T"));
        } catch (Exception e) {
            throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT,
                    sourceType,
                    rawPayload,
                    "Invalid timestamp: " + f[8]
            );
        }

        txn.setValueDate(txnTime); // ✅ FULL LocalDateTime

        // ── STATUS ───────────────────────────
        txn.setTxnStatus(
                TransactionStatus.valueOf(trim(f[9]).toUpperCase())
        );

        // ── Bank Names ───────────────────────
        txn.setSenderBankName(
                BankNameResolver.fromIfsc(txn.getSenderIfsc()));

        txn.setReceiverBankName(
                BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        // ── Priority ─────────────────────────
        txn.setPriority(isUpi ? 6 : 4);

        // ── Normalized Payload ───────────────
        txn.setNormalizedPayload(buildNormalized(txn));

        // ── Validation ───────────────────────
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, sourceType);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
            System.err.println("  [" + txn.getChannelCode() + "-ADAPTER][FAIL] " + vr);
        } else {
            txn.setProcessingStatus(ProcessingStatus.VALIDATED);

            if (txn.getTxnStatus() == TransactionStatus.SUCCESS) {
                txn.setProcessingStatus(ProcessingStatus.QUEUED);

                if (isUpi) {
                    UPI_SOURCE.recordSuccess();
                } else {
                    NEFT_SOURCE.recordSuccess();
                }
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
        return SourceType.NEFT;
    }

    // ─────────────────────────────
    // NORMALIZED FORMAT
    // ─────────────────────────────
    private String buildNormalized(IncomingTransaction txn) {
        return "{"
                + "\"txn_id\":\"" + txn.getSourceRef() + "\","
                + "\"channel\":\"" + txn.getChannelCode() + "\","
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
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

public class CbsAdapter implements TransactionAdapter {

    private static final SourceSystem CBS_SOURCE = SourceSystem.CBS();

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IngestionException(
                IngestionException.ERR_NULL_PAYLOAD,
                SourceType.CBS,
                rawPayload,
                "rawPayload is null or empty"
            );
        }

        String[] f = rawPayload.split("\\|", -1);

        if (f.length < 10) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT,
                SourceType.CBS,
                rawPayload,
                "Expected minimum 10 fields, got: " + f.length
            );
        }

        IncomingTransaction txn = new IncomingTransaction();

        txn.setSourceSystem(CBS_SOURCE);
        txn.setChannelCode("CBS");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("CBS-ADAPTER");

        txn.setTxnType(TransactionType.valueOf(trim(f[0]).toUpperCase()));

        txn.setSenderCustomerId(trim(f[1]));
        txn.setSenderAccount(trim(f[2]));

        txn.setReceiverCustomerId(trim(f[3]));
        txn.setReceiverAccount(trim(f[4]));

        txn.setSenderIfsc(txn.getSenderAccount());
        txn.setReceiverIfsc(txn.getReceiverAccount());

        txn.setAmount(new BigDecimal(trim(f[5])));
        txn.setGrossAmount(txn.getAmount());
        txn.setFeeAmount(BigDecimal.ZERO);

        txn.setCurrency(trim(f[6]).toUpperCase());

        LocalDateTime ts;
        try {
            ts = LocalDateTime.parse(trim(f[7]).replace(" ", "T"));
        } catch (Exception e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT,
                SourceType.CBS,
                rawPayload,
                "Invalid timestamp: " + f[7]
            );
        }

        txn.setValueDate(ts);
        txn.setSourceRef(trim(f[8]));
        txn.setTxnStatus(TransactionStatus.valueOf(trim(f[9]).toUpperCase()));

        txn.setSenderBankName(BankNameResolver.fromIfsc(txn.getSenderIfsc()));
        txn.setReceiverBankName(BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        txn.setPriority(5);
        txn.setNormalizedPayload(buildNormalized(txn));

        // VALIDATION
        PayloadValidator.ValidationResult vr =
                PayloadValidator.validate(txn, SourceType.CBS);

        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());

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

        } else {
            txn.setProcessingStatus(ProcessingStatus.VALIDATED);

            if (txn.getTxnStatus() == TransactionStatus.SUCCESS) {
                txn.setProcessingStatus(ProcessingStatus.QUEUED);
                CBS_SOURCE.recordSuccess();
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


	private String safe(Object val) {
		return val == null ? "N/A" : val.toString();
	}

    @Override
    public SourceType getSourceType() {
        return SourceType.CBS;
    }

    // ─────────────────────────────
    // HELPERS
    // ─────────────────────────────

    private String buildNormalized(IncomingTransaction txn) {
        return "{"
                + "\"txn_id\":\"" + txn.getSourceRef() + "\","
                + "\"channel\":\"CBS\","
                + "\"txn_type\":\"" + txn.getTxnType() + "\","
                + "\"amount\":" + txn.getAmount() + ","
                + "\"currency\":\"" + txn.getCurrency() + "\""
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

    private String formatAmount(BigDecimal amt) {
        if (amt == null) return "0.00";
        return String.format("%,.2f", amt);
    }
}
package com.iispl.adaptor;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.ProtocolType;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;
import com.iispl.intefaces.TransactionAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CbsAdapter — Adapter for Core Banking System (CBS).
 *
 * CBS sends pipe-delimited flat records:
 *   FORMAT  : TXN_TYPE|ACCOUNT_NO|AMOUNT|CURRENCY|VALUE_DATE|REF_NO
 *   EXAMPLE : CREDIT|ACC001001|25000.00|INR|2025-07-18|CBS-TXN-001
 *
 * Protocol : DIRECT_DB (CBS writes to staging table, we read it)
 */
public class CbsAdapter implements TransactionAdapter {

    // Static SourceSystem — created once, reused for every CBS transaction
    private static final SourceSystem CBS_SOURCE;

    static {
        CBS_SOURCE = new SourceSystem("CBS", ProtocolType.DIRECT_DB);
        CBS_SOURCE.setContactEmail("cbs@bank.in");
        CBS_SOURCE.setActive(true);
    }

    /**
     * Parse CBS pipe-delimited record → canonical IncomingTransaction.
     *
     * RAW PAYLOAD EXAMPLE:
     *   "CREDIT|ACC001001|25000.00|INR|2025-07-18|CBS-TXN-20250718-001"
     *
     *   Index 0 → TransactionType  (CREDIT / DEBIT)
     *   Index 1 → Account Number   (stored in sourceRef)
     *   Index 2 → Amount
     *   Index 3 → Currency
     *   Index 4 → Value Date       (yyyy-MM-dd)
     *   Index 5 → CBS Reference No
     */
    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("[CbsAdapter] rawPayload cannot be null or empty");
        }

        String[] fields = rawPayload.split("\\|");

        if (fields.length < 6) {
            throw new IllegalArgumentException(
                "[CbsAdapter] Invalid CBS record. Expected 6 fields, got: " + fields.length
                + " | Payload: " + rawPayload
            );
        }

        IncomingTransaction txn = new IncomingTransaction();

        // Map CBS fields → IncomingTransaction
        txn.setSourceSystem(CBS_SOURCE);
        txn.setTxnType(TransactionType.valueOf(fields[0].trim().toUpperCase()));
        txn.setSourceRef(fields[5].trim());                         // CBS Ref No
        txn.setAmount(new BigDecimal(fields[2].trim()));
        txn.setCurrency(fields[3].trim().toUpperCase());
        txn.setValueDate(LocalDate.parse(fields[4].trim()));
        txn.setRawPayload(rawPayload);
        txn.setNormalizedPayload(buildNormalized(fields));
        txn.setProcessingStatus(ProcessingStatus.QUEUED);
        txn.setCreatedBy("CBS-ADAPTER");

        System.out.println("[CbsAdapter] Adapted → " + txn);
        return txn;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.CBS;
    }

    // Build a clean normalized JSON-like string for audit/logging
    private String buildNormalized(String[] f) {
        return "{\"source\":\"CBS\","
             + "\"txnType\":\"" + f[0].trim() + "\","
             + "\"account\":\"" + f[1].trim() + "\","
             + "\"amount\":" + f[2].trim() + ","
             + "\"currency\":\"" + f[3].trim() + "\","
             + "\"valueDate\":\"" + f[4].trim() + "\","
             + "\"ref\":\"" + f[5].trim() + "\"}";
    }
}

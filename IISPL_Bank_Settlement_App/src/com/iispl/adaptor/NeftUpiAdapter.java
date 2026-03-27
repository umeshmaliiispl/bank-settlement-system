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
 * NeftUpiAdapter — Adapter for NEFT and UPI domestic retail payments.
 *
 * One adapter handles BOTH NEFT and UPI because:
 *   - Both are RBI-regulated domestic payment rails
 *   - Both share similar flat-file / REST format
 *   - SourceType is detected automatically from the reference prefix
 *
 * ─────────────────────────────────────────────────────────────────
 * NEFT — National Electronic Funds Transfer
 *   PROTOCOL   : FLAT_FILE (batch file from NPCI)
 *   SETTLEMENT : Batch (every 30 min, 6 AM – 8 PM on working days)
 *   LIMIT      : No minimum, max ₹10 lakh typically
 *   REF PREFIX : "NEFT-"
 *
 * UPI — Unified Payments Interface
 *   PROTOCOL   : REST_API (push from NPCI / payment apps)
 *   SETTLEMENT : Real-time (24×7)
 *   LIMIT      : ₹1 to ₹1,00,000 per transaction (RBI limit)
 *   REF PREFIX : "UPI-"
 * ─────────────────────────────────────────────────────────────────
 *
 * RAW PAYLOAD FORMAT (CSV — same for both NEFT and UPI):
 *   REF_NO,TXN_TYPE,SENDER_VPA_OR_ACCT,RECEIVER_VPA_OR_ACCT,AMOUNT,CURRENCY,VALUE_DATE
 *
 * NEFT EXAMPLE:
 *   "NEFT-20250718-00123,CREDIT,SBIN0001234-ACC9876,HDFC0005678-ACC1234,50000.00,INR,2025-07-18"
 *
 * UPI EXAMPLE:
 *   "UPI-20250718-XYZ99,CREDIT,rahul@okaxis,priya@ybl,2500.00,INR,2025-07-18"
 */
public class NeftUpiAdapter implements TransactionAdapter {

    private static final SourceSystem NEFT_SOURCE;
    private static final SourceSystem UPI_SOURCE;

    // UPI per-transaction limit: ₹1,00,000
    private static final BigDecimal UPI_MAX_AMOUNT = new BigDecimal("100000.00");

    static {
        NEFT_SOURCE = new SourceSystem("NEFT", ProtocolType.FLAT_FILE);
        NEFT_SOURCE.setContactEmail("neft@rbi.in");
        NEFT_SOURCE.setActive(true);

        UPI_SOURCE = new SourceSystem("UPI", ProtocolType.REST_API);
        UPI_SOURCE.setContactEmail("upi@npci.in");
        UPI_SOURCE.setActive(true);
    }

    /**
     * Parse NEFT/UPI CSV record → canonical IncomingTransaction.
     * Source type auto-detected from reference prefix (NEFT- or UPI-).
     */
    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("[NeftUpiAdapter] rawPayload cannot be null");
        }

        String[] fields = rawPayload.split(",");
        if (fields.length < 7) {
            throw new IllegalArgumentException(
                "[NeftUpiAdapter] Invalid record. Expected 7 fields, got: "
                + fields.length + " | Payload: " + rawPayload
            );
        }

        String refNo    = fields[0].trim();
        String txnType  = fields[1].trim().toUpperCase();
        String amount   = fields[4].trim();
        String currency = fields[5].trim().toUpperCase();
        String valDate  = fields[6].trim();

        // Auto-detect: NEFT or UPI based on reference prefix
        boolean isUpi = refNo.toUpperCase().startsWith("UPI-");

        IncomingTransaction txn = new IncomingTransaction();
        txn.setSourceSystem(isUpi ? UPI_SOURCE : NEFT_SOURCE);
        txn.setSourceRef(refNo);
        txn.setTxnType(TransactionType.valueOf(txnType));
        txn.setAmount(new BigDecimal(amount));
        txn.setCurrency(currency);
        txn.setValueDate(LocalDate.parse(valDate));
        txn.setRawPayload(rawPayload);
        txn.setCreatedBy(isUpi ? "UPI-ADAPTER" : "NEFT-ADAPTER");

        // UPI business rule: max ₹1,00,000
        if (isUpi && txn.getAmount().compareTo(UPI_MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                "[NeftUpiAdapter] UPI transaction " + refNo
                + " exceeds max limit ₹1,00,000. Amount: " + txn.getAmount()
            );
        }

        txn.setNormalizedPayload(buildNormalized(txn, fields, isUpi));
        txn.setProcessingStatus(ProcessingStatus.QUEUED);

        System.out.println("[NeftUpiAdapter] Adapted (" + (isUpi ? "UPI" : "NEFT") + ") → " + txn);
        return txn;
    }

    /**
     * Returns SourceType.NEFT as the primary type.
     * AdapterRegistry registers this adapter for BOTH NEFT and UPI.
     */
    @Override
    public SourceType getSourceType() {
        return SourceType.NEFT;
    }

    private String buildNormalized(IncomingTransaction txn, String[] f, boolean isUpi) {
        return "{\"source\":\"" + (isUpi ? "UPI" : "NEFT") + "\","
             + "\"ref\":\"" + txn.getSourceRef() + "\","
             + "\"txnType\":\"" + txn.getTxnType() + "\","
             + "\"sender\":\"" + f[2].trim() + "\","
             + "\"receiver\":\"" + f[3].trim() + "\","
             + "\"amount\":" + txn.getAmount() + ","
             + "\"currency\":\"" + txn.getCurrency() + "\","
             + "\"valueDate\":\"" + txn.getValueDate() + "\"}";
    }
}

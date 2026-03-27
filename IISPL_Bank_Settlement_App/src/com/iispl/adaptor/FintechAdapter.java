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
 * FintechAdapter — Adapter for third-party Fintech API partners.
 *
 * Fintech partners (PhonePe, Razorpay, Paytm, Stripe, etc.) push transactions
 * via REST webhooks in their own JSON format.
 *
 * PROTOCOL : REST_API (webhook POST — partner pushes to our endpoint)
 *
 * RAW PAYLOAD FORMAT (Fintech partner JSON):
 * {
 *   "partner_ref"   : "RPY-20250718-INV-001",
 *   "partner_name"  : "Razorpay",
 *   "payment_type"  : "CREDIT",
 *   "gross_amount"  : "10000.00",
 *   "fee_amount"    : "200.00",
 *   "net_amount"    : "9800.00",
 *   "currency"      : "INR",
 *   "settlement_date": "2025-07-18",
 *   "merchant_id"   : "MERCH_XYZ_001",
 *   "status"        : "SUCCESS"
 * }
 *
 * Key fields:
 *   partner_ref     → sourceRef (globally unique per Fintech partner)
 *   net_amount      → amount used for settlement (gross minus fee)
 *   fee_amount      → used to create a separate FEE transaction if needed
 */
public class FintechAdapter implements TransactionAdapter {

    private static final SourceSystem FINTECH_SOURCE;

    static {
        FINTECH_SOURCE = new SourceSystem("FINTECH", ProtocolType.REST_API);
        FINTECH_SOURCE.setContactEmail("api@fintech.in");
        FINTECH_SOURCE.setActive(true);
    }

    /**
     * Parse Fintech partner JSON webhook → canonical IncomingTransaction.
     *
     * Settlement amount used = net_amount (after Fintech fee deduction).
     * If fee_amount > 0, a separate FEE transaction can be raised by the
     * settlement engine (out of scope for this adapter).
     */
    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("[FintechAdapter] rawPayload cannot be null");
        }

        // Validate: only process SUCCESS status
        String status = extractJson(rawPayload, "status");
        if (!"SUCCESS".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(
                "[FintechAdapter] Skipping non-SUCCESS transaction. Status: " + status
                + " | Ref: " + safeExtract(rawPayload, "partner_ref")
            );
        }

        IncomingTransaction txn = new IncomingTransaction();
        txn.setSourceSystem(FINTECH_SOURCE);
        txn.setRawPayload(rawPayload);
        txn.setCreatedBy("FINTECH-ADAPTER");

        txn.setSourceRef(extractJson(rawPayload, "partner_ref"));
        txn.setCurrency(extractJson(rawPayload, "currency").toUpperCase());
        txn.setValueDate(LocalDate.parse(extractJson(rawPayload, "settlement_date")));

        // Use net_amount for settlement (gross - fee)
        txn.setAmount(new BigDecimal(extractJson(rawPayload, "net_amount")));

        // Map payment_type → TransactionType
        String paymentType = extractJson(rawPayload, "payment_type").toUpperCase();
        switch (paymentType) {
            case "CREDIT"  : txn.setTxnType(TransactionType.CREDIT);    break;
            case "DEBIT"   : txn.setTxnType(TransactionType.DEBIT);     break;
            case "REFUND"  : txn.setTxnType(TransactionType.REVERSAL);  break;
            case "FEE"     : txn.setTxnType(TransactionType.FEE);       break;
            default        : txn.setTxnType(TransactionType.CREDIT);    break;
        }

        txn.setNormalizedPayload(buildNormalized(txn, rawPayload));
        txn.setProcessingStatus(ProcessingStatus.QUEUED);

        System.out.println("[FintechAdapter] Adapted → " + txn);
        return txn;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.FINTECH;
    }

    /**
     * Lightweight JSON field extractor — no external library needed.
     */
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) {
            throw new IllegalArgumentException("[FintechAdapter] Missing JSON field: " + key);
        }
        int colonIndex = json.indexOf(":", keyIndex);
        int start = json.indexOf("\"", colonIndex + 1);
        int end   = json.indexOf("\"", start + 1);
        return json.substring(start + 1, end).trim();
    }

    // Safe extract — returns "" if field missing (for non-critical fields)
    private String safeExtract(String json, String key) {
        try { return extractJson(json, key); } catch (Exception e) { return ""; }
    }

    private String buildNormalized(IncomingTransaction txn, String raw) {
        String partner = safeExtract(raw, "partner_name");
        String merchant = safeExtract(raw, "merchant_id");
        String feeAmt = safeExtract(raw, "fee_amount");
        return "{\"source\":\"FINTECH\","
             + "\"partner\":\"" + partner + "\","
             + "\"ref\":\"" + txn.getSourceRef() + "\","
             + "\"txnType\":\"" + txn.getTxnType() + "\","
             + "\"netAmount\":" + txn.getAmount() + ","
             + "\"feeAmount\":" + (feeAmt.isEmpty() ? "0" : feeAmt) + ","
             + "\"currency\":\"" + txn.getCurrency() + "\","
             + "\"merchant\":\"" + merchant + "\","
             + "\"settlementDate\":\"" + txn.getValueDate() + "\"}";
    }
}

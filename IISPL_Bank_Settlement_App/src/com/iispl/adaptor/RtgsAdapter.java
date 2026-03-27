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
 * RtgsAdapter — Adapter for Real Time Gross Settlement (RTGS).
 *
 * RTGS sends high-value transactions as JSON via Message Queue (RBI gateway).
 * Minimum transaction value: ₹2,00,000
 *
 * PROTOCOL : MESSAGE_QUEUE (RabbitMQ / ActiveMQ)
 *
 * RAW PAYLOAD FORMAT (JSON string):
 * {
 *   "msgType"    : "RTGS_CREDIT",
 *   "senderIFSC" : "SBIN0001234",
 *   "receiverIFSC": "HDFC0005678",
 *   "amount"     : "500000.00",
 *   "currency"   : "INR",
 *   "valueDate"  : "2025-07-18",
 *   "utr"        : "SBIN225001234567",
 *   "remarks"    : "Vendor Payment"
 * }
 *
 * Key field: UTR (Unique Transaction Reference) — RBI assigned, mandatory
 */
public class RtgsAdapter implements TransactionAdapter {

    private static final SourceSystem RTGS_SOURCE;
    private static final BigDecimal RTGS_MINIMUM = new BigDecimal("200000.00");

    static {
        RTGS_SOURCE = new SourceSystem("RTGS", ProtocolType.MESSAGE_QUEUE);
        RTGS_SOURCE.setContactEmail("rtgs@rbi.in");
        RTGS_SOURCE.setActive(true);
    }

    /**
     * Parse RTGS JSON payload → canonical IncomingTransaction.
     *
     * NOTE: Using simple string parsing (no JSON library needed in Core Java).
     * Team can upgrade to org.json / Jackson if added to classpath.
     */
    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("[RtgsAdapter] rawPayload cannot be null");
        }

        IncomingTransaction txn = new IncomingTransaction();
        txn.setSourceSystem(RTGS_SOURCE);
        txn.setRawPayload(rawPayload);
        txn.setCreatedBy("RTGS-ADAPTER");

        // Simple JSON field extraction (no external library)
        txn.setSourceRef(extractJson(rawPayload, "utr"));
        txn.setAmount(new BigDecimal(extractJson(rawPayload, "amount")));
        txn.setCurrency(extractJson(rawPayload, "currency"));
        txn.setValueDate(LocalDate.parse(extractJson(rawPayload, "valueDate")));

        // Map RTGS msgType → TransactionType
        String msgType = extractJson(rawPayload, "msgType").toUpperCase();
        if (msgType.contains("CREDIT")) {
            txn.setTxnType(TransactionType.CREDIT);
        } else if (msgType.contains("DEBIT")) {
            txn.setTxnType(TransactionType.DEBIT);
        } else {
            txn.setTxnType(TransactionType.INTRABANK);
        }

        // RTGS business rule: amount must be >= ₹2,00,000
        if (txn.getAmount().compareTo(RTGS_MINIMUM) < 0) {
            throw new IllegalArgumentException(
                "[RtgsAdapter] RTGS amount " + txn.getAmount()
                + " is below minimum ₹2,00,000. Use NEFT instead."
            );
        }

        txn.setNormalizedPayload(buildNormalized(txn));
        txn.setProcessingStatus(ProcessingStatus.QUEUED);

        System.out.println("[RtgsAdapter] Adapted → " + txn);
        return txn;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.RTGS;
    }

    /**
     * Lightweight JSON field extractor.
     * Extracts value for a given key from a simple flat JSON string.
     * Example: extractJson("{\"amount\":\"500000\"}", "amount") → "500000"
     */
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) {
            throw new IllegalArgumentException("[RtgsAdapter] Missing field: " + key);
        }
        int colonIndex = json.indexOf(":", keyIndex);
        int start = json.indexOf("\"", colonIndex + 1);
        int end   = json.indexOf("\"", start + 1);
        return json.substring(start + 1, end).trim();
    }

    private String buildNormalized(IncomingTransaction txn) {
        return "{\"source\":\"RTGS\","
             + "\"utr\":\"" + txn.getSourceRef() + "\","
             + "\"txnType\":\"" + txn.getTxnType() + "\","
             + "\"amount\":" + txn.getAmount() + ","
             + "\"currency\":\"" + txn.getCurrency() + "\","
             + "\"valueDate\":\"" + txn.getValueDate() + "\"}";
    }
}

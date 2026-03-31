
package com.iispl.adaptor;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SourceSystem;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SourceType;
import com.iispl.enums.TransactionType;
import com.iispl.exception.IngestionException;
import com.iispl.intefaces.TransactionAdapter;
import com.iispl.validation.PayloadValidator;

/**
 * RtgsAdapter — Enterprise adapter for Real Time Gross Settlement (RTGS).
 *
 * ─────────────────────────────────────────────────────────────────────────
 * SOURCE    : RTGS (RBI Real Time Gross Settlement)
 * PROTOCOL  : MESSAGE_QUEUE — RabbitMQ / ActiveMQ (RBI gateway feed)
 * FORMAT    : JSON string, one object per MQ message
 * PRIORITY  : 1 (HIGHEST — RTGS is individually settled, no netting)
 * MINIMUM   : ₹2,00,000 per transaction (RBI mandate)
 * KEY FIELD : utr — RBI Unique Transaction Reference (16 chars, globally unique)
 * ─────────────────────────────────────────────────────────────────────────
 *
 * JSON FIELD MAPPING:
 *   "msgType"      → txnType     (RTGS_CREDIT → CREDIT, RTGS_DEBIT → DEBIT)
 *   "senderIFSC"   → senderIfsc
 *   "receiverIFSC" → receiverIfsc
 *   "amount"       → amount
 *   "currency"     → currency
 *   "valueDate"    → valueDate   (yyyy-MM-dd)
 *   "utr"          → sourceRef   (16-char RBI reference — THE primary key)
 *   "remarks"      → narration   (optional)
 *
 * SAMPLE DATA (rtgs_transactions.csv):
 *   {"msgType":"RTGS_CREDIT","senderIFSC":"SBIN0001234","receiverIFSC":"HDFC0005678",
 *    "amount":"500000.00","currency":"INR","valueDate":"2025-07-18","utr":"SBIN225001234567"}
 *
 * VALIDATION APPLIED:
 *   - Business: amount >= ₹2,00,000 (hard-fail — not to FAILED, it throws)
 *   - Business: UTR must be exactly 16 chars
 *   - Structural + cross-field from PayloadValidator
 */
public class RtgsAdapter implements TransactionAdapter {

    private static final SourceSystem RTGS_SOURCE   = SourceSystem.RTGS();
    private static final BigDecimal   RTGS_MINIMUM  = new BigDecimal("200000.00");

    // ─────────────────────────────────────────────────────────────────────────
    // ADAPT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty())
            throw new IngestionException(
                IngestionException.ERR_NULL_PAYLOAD, SourceType.RTGS,
                rawPayload, "rawPayload is null or empty");

        // ── Parse JSON (no external library — pure Java string parsing) ────────
        IncomingTransaction txn = new IncomingTransaction();
        txn.setSourceSystem(RTGS_SOURCE);
        txn.setChannelCode("RTGS");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("RTGS-ADAPTER");
        txn.setPriority(1);   // RTGS = maximum priority

        // Extract mandatory fields
        txn.setSourceRef(extract(rawPayload,    "utr",          SourceType.RTGS));
        txn.setSenderIfsc(extract(rawPayload,   "senderIFSC",   SourceType.RTGS).toUpperCase());
        txn.setReceiverIfsc(extract(rawPayload, "receiverIFSC", SourceType.RTGS).toUpperCase());
        txn.setCurrency(extract(rawPayload,     "currency",     SourceType.RTGS).toUpperCase());

        try {
            txn.setAmount(new BigDecimal(extract(rawPayload, "amount", SourceType.RTGS)));
            txn.setGrossAmount(txn.getAmount());
            txn.setFeeAmount(BigDecimal.ZERO);
        } catch (NumberFormatException e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.RTGS, rawPayload,
                "Invalid 'amount' value: " + safeExtract(rawPayload, "amount"));
        }

        try {
            txn.setValueDate(LocalDate.parse(extract(rawPayload, "valueDate", SourceType.RTGS)));
        } catch (Exception e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.RTGS, rawPayload,
                "Invalid 'valueDate' — expected yyyy-MM-dd, got: "
                + safeExtract(rawPayload, "valueDate"));
        }

        // Map msgType → TransactionType
        String msgType = extract(rawPayload, "msgType", SourceType.RTGS).toUpperCase();
        if      (msgType.contains("CREDIT"))    txn.setTxnType(TransactionType.CREDIT);
        else if (msgType.contains("DEBIT"))     txn.setTxnType(TransactionType.DEBIT);
        else                                    txn.setTxnType(TransactionType.INTRABANK);

        // Optional fields
        txn.setNarration(safeExtract(rawPayload, "remarks"));
        txn.setSchemeCode(safeExtract(rawPayload, "schemeCode"));

        // Resolve bank names from IFSC
        txn.setSenderBankName(BankNameResolver.fromIfsc(txn.getSenderIfsc()));
        txn.setReceiverBankName(BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        // ── Business rule: RBI minimum ────────────────────────────────────────
        // Throw early (before full validation) — RTGS below minimum is a fatal error
        if (txn.getAmount().compareTo(RTGS_MINIMUM) < 0) {
            throw new IngestionException(
                IngestionException.ERR_BUSINESS_RULE, SourceType.RTGS, rawPayload,
                "RTGS amount ₹" + txn.getAmount()
                + " is below RBI minimum ₹2,00,000. UTR=" + txn.getSourceRef()
                + ". Use NEFT for amounts below ₹2 lakh.");
        }

        txn.setNormalizedPayload(buildNormalized(txn));

        // ── Full 3-level validation ───────────────────────────────────────────
        PayloadValidator.ValidationResult vr = PayloadValidator.validate(txn, SourceType.RTGS);
        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
            System.err.println("  [RTGS-ADAPTER][FAIL] " + vr);
        } else {
            txn.setProcessingStatus(ProcessingStatus.QUEUED);
            RTGS_SOURCE.recordSuccess();
        }

        System.out.println("  [RTGS-ADAPTER] " + txn.toAuditString());
        return txn;
    }

    @Override
    public SourceType getSourceType() { return SourceType.RTGS; }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS — lightweight JSON field extractor (no external library)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extract value of a JSON string field from flat JSON.
     * Works for simple key-value pairs where value is a quoted string.
     * Example: extract(json, "utr") → "SBIN225001234567"
     */
    private String extract(String json, String key, SourceType src) {
        String search = "\"" + key + "\"";
        int ki = json.indexOf(search);
        if (ki == -1)
            throw new IngestionException(
                IngestionException.ERR_MISSING_FIELD, src, json,
                "Missing mandatory field: '" + key + "'");
        int ci = json.indexOf(":", ki);
        int s  = json.indexOf("\"", ci + 1);
        if (s == -1)
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, src, json,
                "Field '" + key + "' has no quoted value");
        int e = json.indexOf("\"", s + 1);
        return json.substring(s + 1, e).trim();
    }

    /** Silent extract — returns empty string if field missing. */
    private String safeExtract(String json, String key) {
        try { return extract(json, key, SourceType.RTGS); }
        catch (Exception e) { return ""; }
    }

    private String buildNormalized(IncomingTransaction txn) {
        return "{"
             + "\"source\":\"RTGS\","
             + "\"utr\":\""          + txn.getSourceRef()       + "\","
             + "\"msgType\":\""      + txn.getTxnType()         + "\","
             + "\"senderIfsc\":\""   + txn.getSenderIfsc()      + "\","
             + "\"senderBank\":\""   + txn.getSenderBankName()  + "\","
             + "\"receiverIfsc\":\"" + txn.getReceiverIfsc()    + "\","
             + "\"receiverBank\":\"" + txn.getReceiverBankName() + "\","
             + "\"amount\":"         + txn.getAmount()           + ","
             + "\"currency\":\""    + txn.getCurrency()         + "\","
             + "\"valueDate\":\""   + txn.getValueDate()        + "\","
             + "\"narration\":\""   + txn.getNarration()        + "\","
             + "\"checksum\":\""    + txn.getChecksum()         + "\""
             + "}";
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return "CHECKSUM-ERROR"; }
    }
}


//package com.iispl.adaptor;
//
//import com.iispl.entity.IncomingTransaction;
//import com.iispl.entity.SourceSystem;
//import com.iispl.enums.ProcessingStatus;
//import com.iispl.enums.ProtocolType;
//import com.iispl.enums.SourceType;
//import com.iispl.enums.TransactionType;
//import com.iispl.intefaces.TransactionAdapter;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//
///**
// * RtgsAdapter — Adapter for Real Time Gross Settlement (RTGS).
// *
// * RTGS sends high-value transactions as JSON via Message Queue (RBI gateway).
// * Minimum transaction value: ₹2,00,000
// *
// * PROTOCOL : MESSAGE_QUEUE (RabbitMQ / ActiveMQ)
// *
// * RAW PAYLOAD FORMAT (JSON string):
// * {
// *   "msgType"    : "RTGS_CREDIT",
// *   "senderIFSC" : "SBIN0001234",
// *   "receiverIFSC": "HDFC0005678",
// *   "amount"     : "500000.00",
// *   "currency"   : "INR",
// *   "valueDate"  : "2025-07-18",
// *   "utr"        : "SBIN225001234567",
// *   "remarks"    : "Vendor Payment"
// * }
// *
// * Key field: UTR (Unique Transaction Reference) — RBI assigned, mandatory
// */
//public class RtgsAdapter implements TransactionAdapter {
//
//    private static final SourceSystem RTGS_SOURCE;
//    private static final BigDecimal RTGS_MINIMUM = new BigDecimal("200000.00");
//
//    static {
//        RTGS_SOURCE = new SourceSystem("RTGS", ProtocolType.MESSAGE_QUEUE);
//        RTGS_SOURCE.setContactEmail("rtgs@rbi.in");
//        RTGS_SOURCE.setActive(true);
//    }
//
//    /**
//     * Parse RTGS JSON payload → canonical IncomingTransaction.
//     *
//     * NOTE: Using simple string parsing (no JSON library needed in Core Java).
//     * Team can upgrade to org.json / Jackson if added to classpath.
//     */
//    @Override
//    public IncomingTransaction adapt(String rawPayload) {
//
//        if (rawPayload == null || rawPayload.trim().isEmpty()) {
//            throw new IllegalArgumentException("[RtgsAdapter] rawPayload cannot be null");
//        }
//
//        IncomingTransaction txn = new IncomingTransaction();
//        txn.setSourceSystem(RTGS_SOURCE);
//        txn.setRawPayload(rawPayload);
//        txn.setCreatedBy("RTGS-ADAPTER");
//
//        // Simple JSON field extraction (no external library)
//        txn.setSourceRef(extractJson(rawPayload, "utr"));
//        txn.setAmount(new BigDecimal(extractJson(rawPayload, "amount")));
//        txn.setCurrency(extractJson(rawPayload, "currency"));
//        txn.setValueDate(LocalDate.parse(extractJson(rawPayload, "valueDate")));
//
//        // Map RTGS msgType → TransactionType
//        String msgType = extractJson(rawPayload, "msgType").toUpperCase();
//        if (msgType.contains("CREDIT")) {
//            txn.setTxnType(TransactionType.CREDIT);
//        } else if (msgType.contains("DEBIT")) {
//            txn.setTxnType(TransactionType.DEBIT);
//        } else {
//            txn.setTxnType(TransactionType.INTRABANK);
//        }
//
//        // RTGS business rule: amount must be >= ₹2,00,000
//        if (txn.getAmount().compareTo(RTGS_MINIMUM) < 0) {
//            throw new IllegalArgumentException(
//                "[RtgsAdapter] RTGS amount " + txn.getAmount()
//                + " is below minimum ₹2,00,000. Use NEFT instead."
//            );
//        }
//
//        txn.setNormalizedPayload(buildNormalized(txn));
//        txn.setProcessingStatus(ProcessingStatus.QUEUED);
//
//        System.out.println("[RtgsAdapter] Adapted → " + txn);
//        return txn;
//    }
//
//    @Override
//    public SourceType getSourceType() {
//        return SourceType.RTGS;
//    }
//
//    /**
//     * Lightweight JSON field extractor.
//     * Extracts value for a given key from a simple flat JSON string.
//     * Example: extractJson("{\"amount\":\"500000\"}", "amount") → "500000"
//     */
//    private String extractJson(String json, String key) {
//        String search = "\"" + key + "\"";
//        int keyIndex = json.indexOf(search);
//        if (keyIndex == -1) {
//            throw new IllegalArgumentException("[RtgsAdapter] Missing field: " + key);
//        }
//        int colonIndex = json.indexOf(":", keyIndex);
//        int start = json.indexOf("\"", colonIndex + 1);
//        int end   = json.indexOf("\"", start + 1);
//        return json.substring(start + 1, end).trim();
//    }
//
//    private String buildNormalized(IncomingTransaction txn) {
//        return "{\"source\":\"RTGS\","
//             + "\"utr\":\"" + txn.getSourceRef() + "\","
//             + "\"txnType\":\"" + txn.getTxnType() + "\","
//             + "\"amount\":" + txn.getAmount() + ","
//             + "\"currency\":\"" + txn.getCurrency() + "\","
//             + "\"valueDate\":\"" + txn.getValueDate() + "\"}";
//    }
//}

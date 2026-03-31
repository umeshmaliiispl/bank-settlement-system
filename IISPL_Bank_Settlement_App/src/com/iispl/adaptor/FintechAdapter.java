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
 * FintechAdapter — Enterprise adapter for third-party Fintech API partners.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * SOURCE    : Fintech Partner API Gateway
 * PROTOCOL  : REST_API — webhook POST from Fintech partners
 * FORMAT    : JSON (one object per webhook call / one line per file dump)
 * PRIORITY  : 7 (LOW — partner settlements aggregated, not time-critical)
 * PARTNERS  : Razorpay, Paytm, PhonePe, Cashfree, Stripe, PayU
 * ─────────────────────────────────────────────────────────────────────────
 *
 * SETTLEMENT AMOUNT RULE:
 *   amount     = net_amount  = gross_amount - fee_amount
 *   feeAmount  = fee_amount  (tracked separately for reconciliation)
 *   grossAmount= gross_amount
 *
 *   IISPL always RECEIVES from Fintech partners (we are the acquiring bank).
 *
 * FILTERING RULE:
 *   Only transactions with status = "SUCCESS" are processed.
 *   PENDING, FAILED, CANCELLED → throw IngestionException.
 *
 * JSON FIELD MAPPING:
 *   "partner_ref"     → sourceRef    (unique ref from Fintech partner)
 *   "partner_name"    → partnerName  "Razorpay" / "Paytm" / "PhonePe"
 *   "payment_type"    → txnType      CREDIT/DEBIT/REFUND→REVERSAL/FEE
 *   "gross_amount"    → grossAmount
 *   "fee_amount"      → feeAmount
 *   "net_amount"      → amount       (this is what gets settled)
 *   "currency"        → currency
 *   "settlement_date" → valueDate    (yyyy-MM-dd)
 *   "merchant_id"     → merchantId
 *   "status"          → guard: must be "SUCCESS"
 *
 * SAMPLE DATA (fintech_transactions.csv):
 *   {"partner_ref":"RPY-20250718-INV-001","partner_name":"Razorpay",
 *    "payment_type":"CREDIT","gross_amount":"10000.00","fee_amount":"200.00",
 *    "net_amount":"9800.00","currency":"INR","settlement_date":"2025-07-18",
 *    "merchant_id":"MERCH_XYZ_001","status":"SUCCESS"}
 *
 * VALIDATION APPLIED:
 *   - guard: status must be SUCCESS (throws immediately otherwise)
 *   - partnerName mandatory
 *   - net_amount must equal gross - fee (cross-field consistency)
 *   - Standard structural + currency checks from PayloadValidator
 */
public class FintechAdapter implements TransactionAdapter {

    private static final SourceSystem FINTECH_SOURCE = SourceSystem.FINTECH();

    // ─────────────────────────────────────────────────────────────────────────
    // ADAPT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty())
            throw new IngestionException(
                IngestionException.ERR_NULL_PAYLOAD, SourceType.FINTECH,
                rawPayload, "rawPayload is null or empty");

        // ── Guard: only SUCCESS status gets processed ──────────────────────────
        String status = safeExtract(rawPayload, "status");
        if (!"SUCCESS".equalsIgnoreCase(status)) {
            throw new IngestionException(
                IngestionException.ERR_BUSINESS_RULE, SourceType.FINTECH, rawPayload,
                "Skipping non-SUCCESS transaction. "
                + "status='" + status + "' "
                + "ref='"    + safeExtract(rawPayload, "partner_ref") + "' "
                + "partner='" + safeExtract(rawPayload, "partner_name") + "'");
        }

        IncomingTransaction txn = new IncomingTransaction();
        txn.setSourceSystem(FINTECH_SOURCE);
        txn.setChannelCode("FINTECH");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("FINTECH-ADAPTER");
        txn.setPriority(7);   // LOW — batch aggregated partner settlement

        // ── Extract mandatory fields ──────────────────────────────────────────
        txn.setSourceRef(extract(rawPayload, "partner_ref"));
        txn.setPartnerName(extract(rawPayload, "partner_name"));
        txn.setMerchantId(safeExtract(rawPayload, "merchant_id"));

        // Amount fields — critical: validate gross - fee = net
        BigDecimal grossAmt, feeAmt, netAmt;
        try {
            grossAmt = new BigDecimal(extract(rawPayload, "gross_amount"));
            feeAmt   = new BigDecimal(extract(rawPayload, "fee_amount"));
            netAmt   = new BigDecimal(extract(rawPayload, "net_amount"));
        } catch (NumberFormatException e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.FINTECH, rawPayload,
                "Invalid amount field — not a valid decimal. "
                + "gross='" + safeExtract(rawPayload, "gross_amount")
                + "' fee='" + safeExtract(rawPayload, "fee_amount")
                + "' net='" + safeExtract(rawPayload, "net_amount") + "'");
        }

        txn.setGrossAmount(grossAmt);
        txn.setFeeAmount(feeAmt);
        txn.setAmount(netAmt);      // settlement amount = net (after fee deduction)

        // Currency
        txn.setCurrency(extract(rawPayload, "currency").toUpperCase());

        // Value date
        try {
            txn.setValueDate(LocalDate.parse(extract(rawPayload, "settlement_date")));
        } catch (Exception e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.FINTECH, rawPayload,
                "Invalid 'settlement_date' — expected yyyy-MM-dd, got: "
                + safeExtract(rawPayload, "settlement_date"));
        }

        // Map payment_type → TransactionType
        String paymentType = extract(rawPayload, "payment_type").toUpperCase();
        switch (paymentType) {
            case "CREDIT":  txn.setTxnType(TransactionType.CREDIT);   break;
            case "DEBIT":   txn.setTxnType(TransactionType.DEBIT);    break;
            case "REFUND":  txn.setTxnType(TransactionType.REVERSAL); break;
            case "FEE":     txn.setTxnType(TransactionType.FEE);      break;
            default:
                throw new IngestionException(
                    IngestionException.ERR_INVALID_FORMAT, SourceType.FINTECH, rawPayload,
                    "Unknown payment_type: '" + paymentType
                    + "'. Expected: CREDIT / DEBIT / REFUND / FEE");
        }

        // Sender = Fintech partner, Receiver = IISPL (we always receive)
        txn.setSenderBankName(txn.getPartnerName());
        txn.setReceiverIfsc("IISPL0000001");   // IISPL Bank's own IFSC
        txn.setReceiverBankName("IISPL Bank");

        // Remarks for audit trail
        txn.setRemarks("Partner=" + txn.getPartnerName()
                      + " | Merchant=" + txn.getMerchantId()
                      + " | Gross=" + grossAmt
                      + " | Fee="   + feeAmt
                      + " | Net="   + netAmt);

        txn.setNormalizedPayload(buildNormalized(txn, grossAmt, feeAmt));

        // ── Full 3-level validation ───────────────────────────────────────────
        PayloadValidator.ValidationResult vr = PayloadValidator.validate(txn, SourceType.FINTECH);
        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
            System.err.println("  [FINTECH-ADAPTER][FAIL] " + vr);
        } else {
            txn.setProcessingStatus(ProcessingStatus.QUEUED);
            FINTECH_SOURCE.recordSuccess();
        }

        System.out.println("  [FINTECH-ADAPTER] " + txn.toAuditString());
        return txn;
    }

    @Override
    public SourceType getSourceType() { return SourceType.FINTECH; }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS — lightweight JSON field extractor (no external library)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extract value of a JSON string field.
     * Handles both string values ("key":"value") and numeric values ("key":123).
     * Throws IngestionException if field is missing.
     */
    private String extract(String json, String key) {
        String search = "\"" + key + "\"";
        int ki = json.indexOf(search);
        if (ki == -1)
            throw new IngestionException(
                IngestionException.ERR_MISSING_FIELD, SourceType.FINTECH, json,
                "Missing mandatory JSON field: '" + key + "'");

        int ci = json.indexOf(":", ki + search.length());
        if (ci == -1)
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.FINTECH, json,
                "Malformed JSON around field: '" + key + "'");

        // Skip whitespace after colon
        int valueStart = ci + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;

        if (json.charAt(valueStart) == '"') {
            // String value
            int s = valueStart;
            int e = json.indexOf("\"", s + 1);
            return json.substring(s + 1, e).trim();
        } else {
            // Numeric value — read until comma or }
            int end = valueStart;
            while (end < json.length()
                   && json.charAt(end) != ','
                   && json.charAt(end) != '}') end++;
            return json.substring(valueStart, end).trim();
        }
    }

    /** Silent extract — returns "" if field missing. */
    private String safeExtract(String json, String key) {
        try { return extract(json, key); }
        catch (Exception e) { return ""; }
    }

    private String buildNormalized(IncomingTransaction txn,
                                   BigDecimal gross, BigDecimal fee) {
        return "{"
             + "\"source\":\"FINTECH\","
             + "\"ref\":\""         + txn.getSourceRef()    + "\","
             + "\"partner\":\""     + txn.getPartnerName()  + "\","
             + "\"merchant\":\""    + txn.getMerchantId()   + "\","
             + "\"type\":\""        + txn.getTxnType()      + "\","
             + "\"grossAmount\":"   + gross                  + ","
             + "\"feeAmount\":"     + fee                    + ","
             + "\"netAmount\":"     + txn.getAmount()        + ","
             + "\"currency\":\""   + txn.getCurrency()      + "\","
             + "\"settlementDate\":\""  + txn.getValueDate() + "\","
             + "\"receiver\":\"IISPL Bank\","
             + "\"checksum\":\""   + txn.getChecksum()      + "\""
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
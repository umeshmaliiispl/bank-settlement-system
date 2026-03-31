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
 * SwiftAdapter — Enterprise adapter for SWIFT cross-border payments (MT103).
 *
 * ─────────────────────────────────────────────────────────────────────────
 * SOURCE    : SWIFT Alliance Gateway
 * PROTOCOL  : MESSAGE_QUEUE (SWIFT Alliance Access → RabbitMQ)
 * FORMAT    : MT103 (Single Customer Credit Transfer) — colon-tagged lines
 * PRIORITY  : 3 (HIGH — cross-border, FX conversion required)
 * KEY FIELD : :20: TRN — Transaction Reference Number (max 16 chars)
 * ─────────────────────────────────────────────────────────────────────────
 *
 * MT103 TAG MAPPING:
 *   :20:  → sourceRef       Transaction Reference Number (TRN)
 *   :32A: → valueDate(6) + currency(3) + amount  e.g. "250718USD15000.00"
 *   :50K: → senderName      Ordering customer (free text)
 *   :52A: → senderBic       Sender's bank BIC   e.g. "SBININBB"
 *   :57A: → receiverBic     Receiver's bank BIC e.g. "HDFCINBB"
 *   :59:  → receiverName    Beneficiary name
 *   :70:  → remarks         Remittance information
 *   :71A: → chargeType      OUR / BEN / SHA (who bears SWIFT charges)
 *
 * SAMPLE RECORD (swift_transactions.csv):
 *   :20:SWIFT-REF-20250718-001
 *   :52A:SBININBB
 *   :57A:HDFCINBB
 *   :32A:250718USD15000.00
 *   :50K:JOHN DOE, NEW YORK, USA
 *   :59:JANE DOE, MUMBAI, INDIA
 *   :70:Invoice Payment Q2 2025
 *   :71A:SHA
 *
 * :32A: FORMAT BREAKDOWN:
 *   "250718"     → YYMMDD → 2025-07-18
 *   "USD"        → ISO 4217 currency
 *   "15000.00"   → amount (rest of field after 9 chars)
 *
 * SWIFT DATE CONVENTION:
 *   YY < 50  → 2000 + YY  (e.g. 25 → 2025)
 *   YY >= 50 → 1900 + YY  (e.g. 99 → 1999)
 */
public class SwiftAdapter implements TransactionAdapter {

    private static final SourceSystem SWIFT_SOURCE = SourceSystem.SWIFT();

    // ─────────────────────────────────────────────────────────────────────────
    // ADAPT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty())
            throw new IngestionException(
                IngestionException.ERR_NULL_PAYLOAD, SourceType.SWIFT,
                rawPayload, "rawPayload is null or empty");

        IncomingTransaction txn = new IncomingTransaction();
        txn.setSourceSystem(SWIFT_SOURCE);
        txn.setChannelCode("SWIFT");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("SWIFT-ADAPTER");
        txn.setPriority(3);                          // HIGH — cross-border
        txn.setTxnType(TransactionType.CREDIT);      // MT103 = customer credit transfer always

        // ── Extract mandatory MT103 tags ──────────────────────────────────────

        // :20: Transaction Reference Number
        txn.setSourceRef(extractTag(rawPayload, ":20:"));

        // :32A: Value Date + Currency + Amount
        String field32A = extractTag(rawPayload, ":32A:");
        if (field32A.length() < 10)
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.SWIFT, rawPayload,
                ":32A: field too short. Expected YYMMDD+CCY(3)+AMOUNT, got: '" + field32A + "'");

        String yymmdd   = field32A.substring(0, 6);          // e.g. "250718"
        String currency = field32A.substring(6, 9);          // e.g. "USD"
        String amount   = field32A.substring(9).trim();      // e.g. "15000.00"

        txn.setCurrency(currency.toUpperCase());

        try {
            txn.setAmount(new BigDecimal(amount));
            txn.setGrossAmount(txn.getAmount());
            txn.setFeeAmount(BigDecimal.ZERO);
        } catch (NumberFormatException e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.SWIFT, rawPayload,
                "Invalid amount in :32A: field: '" + amount + "'");
        }

        try {
            txn.setValueDate(parseSwiftDate(yymmdd));
        } catch (Exception e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.SWIFT, rawPayload,
                "Invalid date in :32A: field: '" + yymmdd + "'. Expected YYMMDD.");
        }

        // ── Extract optional MT103 tags ───────────────────────────────────────
        txn.setSenderBic(safeTag(rawPayload,    ":52A:"));   // Sender bank BIC
        txn.setReceiverBic(safeTag(rawPayload,  ":57A:"));   // Receiver bank BIC
        txn.setSenderName(safeTag(rawPayload,   ":50K:"));   // Ordering customer
        txn.setReceiverName(safeTag(rawPayload, ":59:"));    // Beneficiary
        txn.setRemarks(safeTag(rawPayload,      ":70:"));    // Remittance info
        txn.setChargeType(safeTag(rawPayload,   ":71A:"));   // OUR/BEN/SHA

        // Resolve bank names from BIC
        txn.setSenderBankName(BankNameResolver.fromBic(txn.getSenderBic()));
        txn.setReceiverBankName(BankNameResolver.fromBic(txn.getReceiverBic()));

        txn.setNormalizedPayload(buildNormalized(txn));

        // ── Full 3-level validation ───────────────────────────────────────────
        PayloadValidator.ValidationResult vr = PayloadValidator.validate(txn, SourceType.SWIFT);
        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
            System.err.println("  [SWIFT-ADAPTER][FAIL] " + vr);
        } else {
            txn.setProcessingStatus(ProcessingStatus.QUEUED);
            SWIFT_SOURCE.recordSuccess();
        }

        System.out.println("  [SWIFT-ADAPTER] " + txn.toAuditString());
        return txn;
    }

    @Override
    public SourceType getSourceType() { return SourceType.SWIFT; }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extract the value after an MT103 colon-tag in a multi-line payload.
     * Reads from after the tag to end-of-line.
     * Throws IngestionException if tag not found.
     *
     * Example: extractTag(":20:SWIFT-REF\n:32A:...", ":20:") → "SWIFT-REF"
     */
    private String extractTag(String payload, String tag) {
        int ti = payload.indexOf(tag);
        if (ti == -1)
            throw new IngestionException(
                IngestionException.ERR_MISSING_FIELD, SourceType.SWIFT, payload,
                "Missing mandatory MT103 tag: " + tag);
        int valueStart = ti + tag.length();
        int lineEnd    = payload.indexOf("\n", valueStart);
        String value   = (lineEnd == -1)
            ? payload.substring(valueStart)
            : payload.substring(valueStart, lineEnd);
        return value.trim();
    }

    /** Silent tag extractor — returns "" if tag not present. */
    private String safeTag(String payload, String tag) {
        try { return extractTag(payload, tag); }
        catch (Exception e) { return ""; }
    }

    /**
     * Convert SWIFT 6-digit date YYMMDD → LocalDate.
     * SWIFT convention: YY < 50 → 2000 + YY, YY >= 50 → 1900 + YY
     */
    private LocalDate parseSwiftDate(String yymmdd) {
        int yy   = Integer.parseInt(yymmdd.substring(0, 2));
        int mm   = Integer.parseInt(yymmdd.substring(2, 4));
        int dd   = Integer.parseInt(yymmdd.substring(4, 6));
        int yyyy = (yy < 50) ? (2000 + yy) : (1900 + yy);
        return LocalDate.of(yyyy, mm, dd);
    }

    private String buildNormalized(IncomingTransaction txn) {
        return "{"
             + "\"source\":\"SWIFT\","
             + "\"trn\":\""           + txn.getSourceRef()        + "\","
             + "\"senderBic\":\""     + txn.getSenderBic()        + "\","
             + "\"senderBank\":\""    + txn.getSenderBankName()   + "\","
             + "\"senderName\":\""    + esc(txn.getSenderName())  + "\","
             + "\"receiverBic\":\""   + txn.getReceiverBic()      + "\","
             + "\"receiverBank\":\""  + txn.getReceiverBankName() + "\","
             + "\"receiverName\":\""  + esc(txn.getReceiverName()) + "\","
             + "\"amount\":"          + txn.getAmount()            + ","
             + "\"currency\":\""     + txn.getCurrency()          + "\","
             + "\"valueDate\":\""    + txn.getValueDate()         + "\","
             + "\"chargeType\":\""   + txn.getChargeType()        + "\","
             + "\"remarks\":\""      + esc(txn.getRemarks())      + "\","
             + "\"checksum\":\""     + txn.getChecksum()          + "\""
             + "}";
    }

    /** Escape double-quotes in free-text fields for safe JSON embedding. */
    private String esc(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
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
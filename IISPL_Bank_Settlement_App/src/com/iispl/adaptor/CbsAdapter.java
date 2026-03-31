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
 * CbsAdapter — Enterprise adapter for the Core Banking System (CBS).
 *
 * ─────────────────────────────────────────────────────────────────────────
 * SOURCE    : CBS (Core Banking System)
 * PROTOCOL  : DIRECT_DB — CBS writes to a staging table; we poll and read
 * FORMAT    : Pipe-delimited flat record (7 fields)
 * PRIORITY  : 5 (NORMAL — CBS is intraday batch, not real-time gross)
 * ─────────────────────────────────────────────────────────────────────────
 *
 * FIELD MAPPING (index → IncomingTransaction field):
 *   [0] TXN_TYPE      → txnType         (CREDIT / DEBIT)
 *   [1] SENDER_IFSC   → senderIfsc      e.g. SBIN0001234
 *   [2] RECEIVER_IFSC → receiverIfsc    e.g. HDFC0005678
 *   [3] AMOUNT        → amount          e.g. 25000.00
 *   [4] CURRENCY      → currency        e.g. INR
 *   [5] VALUE_DATE    → valueDate       yyyy-MM-dd
 *   [6] REF_NO        → sourceRef       e.g. CBS-TXN-20250718-001
 *
 * SAMPLE RECORDS (cbs_transactions.csv):
 *   CREDIT|SBIN0001234|HDFC0005678|25000.00|INR|2025-07-18|CBS-TXN-20250718-001
 *   DEBIT|HDFC0005678|SBIN0001234|12500.50|INR|2025-07-18|CBS-TXN-20250718-002
 *   CREDIT|ICIC0009999|AXIS0001122|75000.00|INR|2025-07-18|CBS-TXN-20250718-003
 *
 * VALIDATION APPLIED:
 *   - Structural: 7 fields present, amount > 0, valid currency
 *   - Business  : IFSC format check (both sender and receiver)
 *   - Cross-field: sender IFSC ≠ receiver IFSC; valueDate not stale
 */
public class CbsAdapter implements TransactionAdapter {

    // Single SourceSystem instance — created once, reused for all CBS txns
    private static final SourceSystem CBS_SOURCE = SourceSystem.CBS();

    // ─────────────────────────────────────────────────────────────────────────
    // ADAPT — parse raw payload → IncomingTransaction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse CBS pipe-delimited record → canonical IncomingTransaction.
     *
     * @param rawPayload  pipe-delimited CBS record string
     * @return            validated IncomingTransaction (status = QUEUED or FAILED)
     */
    @Override
    public IncomingTransaction adapt(String rawPayload) {

        // ── Guard: null / empty ───────────────────────────────────────────────
        if (rawPayload == null || rawPayload.trim().isEmpty())
            throw new IngestionException(
                IngestionException.ERR_NULL_PAYLOAD, SourceType.CBS,
                rawPayload, "rawPayload is null or empty");

        // ── Parse pipe-delimited fields ───────────────────────────────────────
        String[] f = rawPayload.split("\\|", -1);

        if (f.length < 7)
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.CBS, rawPayload,
                "Expected 7 pipe-delimited fields, got: " + f.length
                + " | Payload: " + rawPayload);

        // ── Build canonical IncomingTransaction ───────────────────────────────
        IncomingTransaction txn = new IncomingTransaction();

        // [A] Source identification
        txn.setSourceSystem(CBS_SOURCE);
        txn.setChannelCode("CBS");
        txn.setRawPayload(rawPayload);
        txn.setChecksum(sha256(rawPayload));
        txn.setCreatedBy("CBS-ADAPTER");

        // [B] Transaction core — map CSV columns
        try {
            txn.setTxnType(TransactionType.valueOf(trim(f[0]).toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.CBS, rawPayload,
                "Unknown TXN_TYPE: '" + f[0].trim() + "'. Expected: CREDIT or DEBIT");
        }

        try {
            txn.setAmount(new BigDecimal(trim(f[3])));
            txn.setGrossAmount(txn.getAmount()); // CBS: gross = net (no fee)
            txn.setFeeAmount(BigDecimal.ZERO);
        } catch (NumberFormatException e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.CBS, rawPayload,
                "Invalid AMOUNT: '" + f[3].trim() + "' is not a valid decimal number");
        }

        txn.setCurrency(trim(f[4]).toUpperCase());

        try {
            txn.setValueDate(LocalDate.parse(trim(f[5])));
        } catch (Exception e) {
            throw new IngestionException(
                IngestionException.ERR_INVALID_FORMAT, SourceType.CBS, rawPayload,
                "Invalid VALUE_DATE: '" + f[5].trim() + "'. Expected: yyyy-MM-dd");
        }

        txn.setSourceRef(trim(f[6]));

        // [C] Sender details
        txn.setSenderIfsc(trim(f[1]).toUpperCase());
        txn.setSenderBankName(BankNameResolver.fromIfsc(txn.getSenderIfsc()));

        // [D] Receiver details
        txn.setReceiverIfsc(trim(f[2]).toUpperCase());
        txn.setReceiverBankName(BankNameResolver.fromIfsc(txn.getReceiverIfsc()));

        // [E] Priority (CBS = normal)
        txn.setPriority(5);

        // Normalized JSON snapshot for audit
        txn.setNormalizedPayload(buildNormalized(txn));

        // ── Validate (3-level) ────────────────────────────────────────────────
        PayloadValidator.ValidationResult vr = PayloadValidator.validate(txn, SourceType.CBS);
        if (!vr.isPassed()) {
            txn.setProcessingStatus(ProcessingStatus.FAILED);
            txn.setErrorMessage(vr.getErrorSummary());
            System.err.println("  [CBS-ADAPTER][FAIL] " + vr);
        } else {
            txn.setProcessingStatus(ProcessingStatus.QUEUED);
            CBS_SOURCE.recordSuccess();
        }

        System.out.println("  [CBS-ADAPTER] " + txn.toAuditString());
        return txn;
    }

    @Override
    public SourceType getSourceType() { return SourceType.CBS; }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String buildNormalized(IncomingTransaction txn) {
        return "{"
             + "\"source\":\"CBS\","
             + "\"ref\":\""         + txn.getSourceRef()       + "\","
             + "\"type\":\""        + txn.getTxnType()          + "\","
             + "\"senderIfsc\":\""  + txn.getSenderIfsc()       + "\","
             + "\"senderBank\":\""  + txn.getSenderBankName()   + "\","
             + "\"receiverIfsc\":\"" + txn.getReceiverIfsc()    + "\","
             + "\"receiverBank\":\"" + txn.getReceiverBankName() + "\","
             + "\"amount\":"        + txn.getAmount()            + ","
             + "\"currency\":\""   + txn.getCurrency()          + "\","
             + "\"valueDate\":\""  + txn.getValueDate()         + "\","
             + "\"checksum\":\""   + txn.getChecksum()          + "\""
             + "}";
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

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


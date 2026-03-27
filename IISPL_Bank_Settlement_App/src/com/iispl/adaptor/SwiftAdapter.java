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
 * SwiftAdapter — Adapter for SWIFT cross-border international payments.
 *
 * SWIFT messages arrive as MT103 (single customer credit transfer) format
 * via Message Queue. The adapter extracts key fields from the MT103 structure.
 *
 * PROTOCOL : MESSAGE_QUEUE
 *
 * RAW PAYLOAD — Simplified MT103 key fields (colon-prefixed tags):
 *   :20:  Transaction Reference Number (TRN)
 *   :32A: Value Date + Currency + Amount  (format: YYMMDDCCCAMOUNT)
 *   :50K: Ordering Customer (Sender)
 *   :59:  Beneficiary Customer (Receiver)
 *   :70:  Remittance Information / Remarks
 *   :71A: Details of Charges (OUR/BEN/SHA)
 *
 * EXAMPLE rawPayload:
 *   ":20:SWIFT-REF-20250718\n:32A:250718USD15000.00\n:50K:JOHN DOE\n:59:JANE DOE\n:70:Invoice Payment\n:71A:SHA"
 *
 * Key field: TRN (Transaction Reference Number) — SWIFT assigned, globally unique
 */
public class SwiftAdapter implements TransactionAdapter {

    private static final SourceSystem SWIFT_SOURCE;

    static {
        SWIFT_SOURCE = new SourceSystem("SWIFT", ProtocolType.MESSAGE_QUEUE);
        SWIFT_SOURCE.setContactEmail("swift@bank.in");
        SWIFT_SOURCE.setActive(true);
    }

    /**
     * Parse SWIFT MT103 payload → canonical IncomingTransaction.
     *
     * MT103 tag :32A: encodes: YYMMDD + CURRENCY(3) + AMOUNT
     * Example  : "250718USD15000.00"
     *             → valueDate = 2025-07-18
     *             → currency  = USD
     *             → amount    = 15000.00
     */
    @Override
    public IncomingTransaction adapt(String rawPayload) {

        if (rawPayload == null || rawPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("[SwiftAdapter] rawPayload cannot be null");
        }

        IncomingTransaction txn = new IncomingTransaction();
        txn.setSourceSystem(SWIFT_SOURCE);
        txn.setRawPayload(rawPayload);
        txn.setCreatedBy("SWIFT-ADAPTER");
        txn.setTxnType(TransactionType.CREDIT);  // MT103 = customer credit transfer

        // Extract :20: Transaction Reference Number
        txn.setSourceRef(extractTag(rawPayload, ":20:"));

        // Extract :32A: → split into date + currency + amount
        String field32A = extractTag(rawPayload, ":32A:");
        // format: YYMMDD(6) + CCY(3) + AMOUNT(rest)
        if (field32A.length() < 10) {
            throw new IllegalArgumentException("[SwiftAdapter] Invalid :32A: field: " + field32A);
        }
        String yymmdd   = field32A.substring(0, 6);   // e.g. "250718"
        String currency = field32A.substring(6, 9);    // e.g. "USD"
        String amount   = field32A.substring(9);        // e.g. "15000.00"

        txn.setCurrency(currency.toUpperCase());
        txn.setAmount(new BigDecimal(amount.trim()));
        txn.setValueDate(parseSwiftDate(yymmdd));

        txn.setNormalizedPayload(buildNormalized(txn, rawPayload));
        txn.setProcessingStatus(ProcessingStatus.QUEUED);

        System.out.println("[SwiftAdapter] Adapted → " + txn);
        return txn;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.SWIFT;
    }

    /**
     * Extract value after an MT103 tag from multi-line payload.
     * Example: extractTag(payload, ":20:") → "SWIFT-REF-20250718"
     */
    private String extractTag(String payload, String tag) {
        int tagIndex = payload.indexOf(tag);
        if (tagIndex == -1) {
            throw new IllegalArgumentException("[SwiftAdapter] Missing tag: " + tag);
        }
        int valueStart = tagIndex + tag.length();
        int lineEnd = payload.indexOf("\n", valueStart);
        String value = (lineEnd == -1)
            ? payload.substring(valueStart)
            : payload.substring(valueStart, lineEnd);
        return value.trim();
    }

    /**
     * Convert SWIFT 6-digit date YYMMDD → LocalDate.
     * SWIFT convention: YY >= 50 → 1900s, YY < 50 → 2000s
     */
    private LocalDate parseSwiftDate(String yymmdd) {
        int yy = Integer.parseInt(yymmdd.substring(0, 2));
        int mm = Integer.parseInt(yymmdd.substring(2, 4));
        int dd = Integer.parseInt(yymmdd.substring(4, 6));
        int yyyy = (yy < 50) ? (2000 + yy) : (1900 + yy);
        return LocalDate.of(yyyy, mm, dd);
    }

    private String buildNormalized(IncomingTransaction txn, String raw) {
        String remarks = "";
        try { remarks = extractTag(raw, ":70:"); } catch (Exception ignored) {}
        return "{\"source\":\"SWIFT\","
             + "\"trn\":\"" + txn.getSourceRef() + "\","
             + "\"txnType\":\"MT103_CREDIT\","
             + "\"amount\":" + txn.getAmount() + ","
             + "\"currency\":\"" + txn.getCurrency() + "\","
             + "\"valueDate\":\"" + txn.getValueDate() + "\","
             + "\"remarks\":\"" + remarks + "\"}";
    }
}

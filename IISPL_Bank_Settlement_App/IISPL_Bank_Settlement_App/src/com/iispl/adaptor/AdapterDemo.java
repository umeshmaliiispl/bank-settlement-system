package com.iispl.adaptor;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

/**
 * AdapterDemo — Quick smoke test for ALL 5 adapters.
 *
 * Run this class to verify:
 *   1. AdapterRegistry initialises correctly
 *   2. Each adapter parses its sample payload without errors
 *   3. Canonical IncomingTransaction fields are populated correctly
 *
 * HOW TO RUN (from project root):
 *   javac -d bin src/com/iispl/**\/*.java
 *   java -cp bin com.iispl.adaptor.AdapterDemo
 *
 * Expected output: 5 "Adapted →" lines, one per source system.
 */
public class AdapterDemo {

    public static void main(String[] args) {

        System.out.println("=================================================");
        System.out.println("  IISPL Bank Settlement — Adapter Pattern Demo   ");
        System.out.println("=================================================\n");

        AdapterRegistry registry = AdapterRegistry.getInstance();

        int passed = 0;
        int failed = 0;

        // ── TEST 1: CBS ────────────────────────────────────────────────────────
        System.out.println("\n--- TEST 1: CBS Adapter (pipe-delimited flat record) ---");
        try {
            String cbsPayload = "CREDIT|ACC001001|25000.00|INR|2025-07-18|CBS-TXN-20250718-001";
            IncomingTransaction txn = registry.adapt(SourceType.CBS, cbsPayload);
            assert txn.getAmount().toString().equals("25000.00") : "Amount mismatch";
            assert txn.getCurrency().equals("INR") : "Currency mismatch";
            assert txn.getSourceRef().equals("CBS-TXN-20250718-001") : "SourceRef mismatch";
            System.out.println("    PASS ✓  amount=" + txn.getAmount()
                + "  currency=" + txn.getCurrency()
                + "  ref=" + txn.getSourceRef());
            passed++;
        } catch (Exception e) {
            System.out.println("    FAIL ✗  " + e.getMessage()); failed++;
        }

        // ── TEST 2: RTGS ───────────────────────────────────────────────────────
        System.out.println("\n--- TEST 2: RTGS Adapter (JSON via Message Queue) ---");
        try {
            String rtgsPayload = "{"
                + "\"msgType\":\"RTGS_CREDIT\","
                + "\"senderIFSC\":\"SBIN0001234\","
                + "\"receiverIFSC\":\"HDFC0005678\","
                + "\"amount\":\"500000.00\","
                + "\"currency\":\"INR\","
                + "\"valueDate\":\"2025-07-18\","
                + "\"utr\":\"SBIN225001234567\","
                + "\"remarks\":\"Vendor Payment\""
                + "}";
            IncomingTransaction txn = registry.adapt(SourceType.RTGS, rtgsPayload);
            assert txn.getAmount().toString().equals("500000.00") : "Amount mismatch";
            assert txn.getSourceRef().equals("SBIN225001234567") : "UTR mismatch";
            System.out.println("    PASS ✓  amount=" + txn.getAmount()
                + "  utr=" + txn.getSourceRef());
            passed++;
        } catch (Exception e) {
            System.out.println("    FAIL ✗  " + e.getMessage()); failed++;
        }

        // ── TEST 3: SWIFT ──────────────────────────────────────────────────────
        System.out.println("\n--- TEST 3: SWIFT Adapter (MT103 cross-border) ---");
        try {
            String swiftPayload =
                ":20:SWIFT-REF-20250718\n"
              + ":32A:250718USD15000.00\n"
              + ":50K:JOHN DOE, NEW YORK\n"
              + ":59:JANE DOE, MUMBAI\n"
              + ":70:Invoice Payment Q2\n"
              + ":71A:SHA";
            IncomingTransaction txn = registry.adapt(SourceType.SWIFT, swiftPayload);
            assert txn.getCurrency().equals("USD") : "Currency mismatch";
            assert txn.getSourceRef().equals("SWIFT-REF-20250718") : "TRN mismatch";
            System.out.println("    PASS ✓  amount=" + txn.getAmount()
                + "  currency=" + txn.getCurrency()
                + "  trn=" + txn.getSourceRef());
            passed++;
        } catch (Exception e) {
            System.out.println("    FAIL ✗  " + e.getMessage()); failed++;
        }

        // ── TEST 4: NEFT ───────────────────────────────────────────────────────
        System.out.println("\n--- TEST 4: NEFT Adapter (batch flat file) ---");
        try {
            String neftPayload = "NEFT-20250718-00123,CREDIT,SBIN0001234-ACC9876,HDFC0005678-ACC1234,50000.00,INR,2025-07-18";
            IncomingTransaction txn = registry.adapt(SourceType.NEFT, neftPayload);
            assert txn.getSourceRef().equals("NEFT-20250718-00123") : "Ref mismatch";
            System.out.println("    PASS ✓  amount=" + txn.getAmount()
                + "  ref=" + txn.getSourceRef());
            passed++;
        } catch (Exception e) {
            System.out.println("    FAIL ✗  " + e.getMessage()); failed++;
        }

        // ── TEST 5: UPI ────────────────────────────────────────────────────────
        System.out.println("\n--- TEST 5: UPI Adapter (real-time REST push) ---");
        try {
            String upiPayload = "UPI-20250718-XYZ99,CREDIT,rahul@okaxis,priya@ybl,2500.00,INR,2025-07-18";
            IncomingTransaction txn = registry.adapt(SourceType.UPI, upiPayload);
            assert txn.getSourceRef().equals("UPI-20250718-XYZ99") : "Ref mismatch";
            System.out.println("    PASS ✓  amount=" + txn.getAmount()
                + "  ref=" + txn.getSourceRef());
            passed++;
        } catch (Exception e) {
            System.out.println("    FAIL ✗  " + e.getMessage()); failed++;
        }

        // ── TEST 6: FINTECH ────────────────────────────────────────────────────
        System.out.println("\n--- TEST 6: Fintech Adapter (webhook REST JSON) ---");
        try {
            String fintechPayload = "{"
                + "\"partner_ref\":\"RPY-20250718-INV-001\","
                + "\"partner_name\":\"Razorpay\","
                + "\"payment_type\":\"CREDIT\","
                + "\"gross_amount\":\"10000.00\","
                + "\"fee_amount\":\"200.00\","
                + "\"net_amount\":\"9800.00\","
                + "\"currency\":\"INR\","
                + "\"settlement_date\":\"2025-07-18\","
                + "\"merchant_id\":\"MERCH_XYZ_001\","
                + "\"status\":\"SUCCESS\""
                + "}";
            IncomingTransaction txn = registry.adapt(SourceType.FINTECH, fintechPayload);
            assert txn.getAmount().toString().equals("9800.00") : "Net amount mismatch";
            assert txn.getSourceRef().equals("RPY-20250718-INV-001") : "Ref mismatch";
            System.out.println("    PASS ✓  netAmount=" + txn.getAmount()
                + "  ref=" + txn.getSourceRef());
            passed++;
        } catch (Exception e) {
            System.out.println("    FAIL ✗  " + e.getMessage()); failed++;
        }

        // ── SUMMARY ────────────────────────────────────────────────────────────
        System.out.println("\n=================================================");
        System.out.println("  RESULTS: " + passed + " passed  |  " + failed + " failed");
        System.out.println("=================================================");
        if (failed == 0) {
            System.out.println("  All adapters working correctly!");
            System.out.println("  Ready for Member 4 to wire into BlockingQueue.");
        } else {
            System.out.println("  Fix failing adapters before handing off to team.");
        }
    }
}

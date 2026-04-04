//
////UMESH MALI
////package com.iispl;
//
//
//import java.math.BigDecimal;
//import java.sql.Connection;
//import java.sql.DatabaseMetaData;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.iispl.config.DatabaseConfig;
//
///**
// * IntegrationTest - Umesh M runs this on Day 2 afternoon.
// *
// * Tests the full pipeline end-to-end against the live Neon DB:
// *   1. DB connectivity
// *   2. All tables exist and have seed data
// *   3. Insert a test IncomingTransaction
// *   4. Create a SettlementBatch and link a SettlementRecord
// *   5. Insert an AuditLog entry
// *   6. Insert a ReconciliationEntry
// *   7. Verify counts
// *   8. Rollback — leave DB clean after test
// *
// * Run: java -cp ".:lib/*" com.banksettlement.IntegrationTest
// */
//public class IntegrationTest {
//
//    private static final List<String> PASSED = new ArrayList<>();
//    private static final List<String> FAILED  = new ArrayList<>();
//
//    public static void main(String[] args) throws Exception {
//        System.out.println("=== Bank Settlement Integration Test ===\n");
//
//        Connection conn = null;
//        try {
//            conn = DatabaseConfig.getConnection();
//            conn.setAutoCommit(false);   // all inserts are rolled back at end
//
//            test1_DbConnectivity(conn);
//            test2_AllTablesExist(conn);
//            test3_SeedDataLoaded(conn);
//            long incomingId = test4_InsertIncomingTransaction(conn);
//            String batchId  = test5_InsertSettlementBatch(conn);
//            test6_InsertSettlementRecord(conn, batchId, incomingId);
//            test7_InsertAuditLog(conn, incomingId);
//            test8_InsertReconciliationEntry(conn);
//            test9_VerifyRowCounts(conn);
//
//            conn.rollback();  // clean up — don't pollute the shared DB
//            System.out.println("\n[ROLLBACK] All test data rolled back. DB is clean.\n");
//
//        } catch (Exception e) {
//            if (conn != null) conn.rollback();
//            FAILED.add("Unexpected exception: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            DatabaseConfig.closeConnection();
//        }
//
//        printSummary();
//    }
//
//    // ── Test 1 ────────────────────────────────────────────────────────────────
//    private static void test1_DbConnectivity(Connection conn) throws SQLException {
//        String name = "DB connectivity";
//        try {
//            DatabaseMetaData meta = conn.getMetaData();
//            System.out.println("[1] Connected to: " + meta.getURL());
//            pass(name);
//        } catch (Exception e) { fail(name, e.getMessage()); }
//    }
//
//    // ── Test 2 ────────────────────────────────────────────────────────────────
//    private static void test2_AllTablesExist(Connection conn) throws SQLException {
//        String name = "All tables exist";
//        String[] tables = {
//            "source_system", "customer", "account", "incoming_transaction",
//            "settlement_batch", "settlement_record", "netting_position",
//            "settlement_instruction", "exchange_rate", "audit_log", "reconciliation_entry"
//        };
//        try {
//            DatabaseMetaData meta = conn.getMetaData();
//            for (String table : tables) {
//                ResultSet rs = meta.getTables(null, "public", table, new String[]{"TABLE"});
//                if (!rs.next()) throw new Exception("Table missing: " + table);
//                rs.close();
//            }
//            System.out.println("[2] All " + tables.length + " tables found.");
//            pass(name);
//        } catch (Exception e) { fail(name, e.getMessage()); }
//    }
//
//    // ── Test 3 ────────────────────────────────────────────────────────────────
//    private static void test3_SeedDataLoaded(Connection conn) throws SQLException {
//        String name = "Seed data loaded";
//        try {
//            int sourceCount = queryCount(conn, "SELECT COUNT(*) FROM source_system");
//            int custCount   = queryCount(conn, "SELECT COUNT(*) FROM customer");
//            int acctCount   = queryCount(conn, "SELECT COUNT(*) FROM account");
//            if (sourceCount < 6) throw new Exception("Expected 6 source_system rows, got " + sourceCount);
//            if (custCount   < 3) throw new Exception("Expected 3 customer rows, got " + custCount);
//            if (acctCount   < 4) throw new Exception("Expected 4 account rows, got " + acctCount);
//            System.out.println("[3] Seed data: " + sourceCount + " sources, " + custCount + " customers, " + acctCount + " accounts.");
//            pass(name);
//        } catch (Exception e) { fail(name, e.getMessage()); }
//    }
//
//    // ── Test 4 ────────────────────────────────────────────────────────────────
//    private static long test4_InsertIncomingTransaction(Connection conn) throws SQLException {
//        String name = "Insert IncomingTransaction";
//        try {
//            long sourceId = queryLong(conn, "SELECT id FROM source_system WHERE system_code='CBS'");
//            String sql = "INSERT INTO incoming_transaction (source_system_id, source_ref, txn_type, amount, currency, processing_status, created_by) " +
//                         "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
//            try (PreparedStatement ps = conn.prepareStatement(sql)) {
//                ps.setLong(1, sourceId);
//                ps.setString(2, "CBS-TEST-" + System.currentTimeMillis());
//                ps.setString(3, "CREDIT");
//                ps.setBigDecimal(4, new BigDecimal("50000.00"));
//                ps.setString(5, "INR");
//                ps.setString(6, "QUEUED");
//                ps.setString(7, "integration-test");
//                ResultSet rs = ps.executeQuery();
//                rs.next();
//                long id = rs.getLong(1);
//                System.out.println("[4] IncomingTransaction inserted with id=" + id);
//                pass(name);
//                return id;
//            }
//        } catch (Exception e) { fail(name, e.getMessage()); return -1; }
//    }
//
//    // ── Test 5 ────────────────────────────────────────────────────────────────
//    private static String test5_InsertSettlementBatch(Connection conn) throws SQLException {
//        String name = "Insert SettlementBatch";
//        String batchId = "BATCH-TEST-" + System.currentTimeMillis();
//        try {
//            String sql = "INSERT INTO settlement_batch (batch_id, batch_status, total_transactions, total_amount, run_by, created_by) " +
//                         "VALUES (?, ?, ?, ?, ?, ?)";
//            try (PreparedStatement ps = conn.prepareStatement(sql)) {
//                ps.setString(1, batchId);
//                ps.setString(2, "RUNNING");
//                ps.setInt(3, 1);
//                ps.setBigDecimal(4, new BigDecimal("50000.00"));
//                ps.setString(5, "team-lead");
//                ps.setString(6, "integration-test");
//                ps.executeUpdate();
//            }
//            System.out.println("[5] SettlementBatch inserted: " + batchId);
//            pass(name);
//            return batchId;
//        } catch (Exception e) { fail(name, e.getMessage()); return batchId; }
//    }
//
//    // ── Test 6 ────────────────────────────────────────────────────────────────
//    private static void test6_InsertSettlementRecord(Connection conn, String batchId, long txnId) throws SQLException {
//        String name = "Insert SettlementRecord";
//        try {
//            String sql = "INSERT INTO settlement_record (batch_id, incoming_txn_id, settled_amount, settled_status, created_by) " +
//                         "VALUES (?, ?, ?, ?, ?)";
//            try (PreparedStatement ps = conn.prepareStatement(sql)) {
//                ps.setString(1, batchId);
//                ps.setLong(2, txnId);
//                ps.setBigDecimal(3, new BigDecimal("50000.00"));
//                ps.setString(4, "SETTLED");
//                ps.setString(5, "integration-test");
//                ps.executeUpdate();
//            }
//            System.out.println("[6] SettlementRecord inserted for batch=" + batchId);
//            pass(name);
//        } catch (Exception e) { fail(name, e.getMessage()); }
//    }
//
//    // ── Test 7 ────────────────────────────────────────────────────────────────
//    private static void test7_InsertAuditLog(Connection conn, long txnId) throws SQLException {
//        String name = "Insert AuditLog";
//        try {
//            String sql = "INSERT INTO audit_log (entity_type, entity_id, action, new_value, changed_by, ip_address, created_by) " +
//                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
//            try (PreparedStatement ps = conn.prepareStatement(sql)) {
//                ps.setString(1, "IncomingTransaction");
//                ps.setLong(2, txnId);
//                ps.setString(3, "CREATE");
//                ps.setString(4, "{\"status\":\"QUEUED\"}");
//                ps.setString(5, "integration-test");
//                ps.setString(6, "127.0.0.1");
//                ps.setString(7, "integration-test");
//                ps.executeUpdate();
//            }
//            System.out.println("[7] AuditLog inserted for entity_id=" + txnId);
//            pass(name);
//        } catch (Exception e) { fail(name, e.getMessage()); }
//    }
//
//    // ── Test 8 ────────────────────────────────────────────────────────────────
//    private static void test8_InsertReconciliationEntry(Connection conn) throws SQLException {
//        String name = "Insert ReconciliationEntry";
//        try {
//            long acctId = queryLong(conn, "SELECT id FROM account WHERE account_number='ACC001001'");
//            String sql = "INSERT INTO reconciliation_entry (account_id, expected_amount, actual_amount, recon_status, created_by) " +
//                         "VALUES (?, ?, ?, ?, ?)";
//            try (PreparedStatement ps = conn.prepareStatement(sql)) {
//                ps.setLong(1, acctId);
//                ps.setBigDecimal(2, new BigDecimal("500000.00"));
//                ps.setBigDecimal(3, new BigDecimal("550000.00"));
//                ps.setString(4, "UNMATCHED");
//                ps.setString(5, "integration-test");
//                ps.executeUpdate();
//            }
//            System.out.println("[8] ReconciliationEntry inserted (variance = 50000)");
//            pass(name);
//        } catch (Exception e) { fail(name, e.getMessage()); }
//    }
//
//    // ── Test 9 ────────────────────────────────────────────────────────────────
//    private static void test9_VerifyRowCounts(Connection conn) throws SQLException {
//        String name = "Row count verification";
//        try {
//            System.out.println("[9] Row counts (including test rows, pre-rollback):");
//            String[] tables = {"incoming_transaction","settlement_batch","settlement_record","audit_log","reconciliation_entry"};
//            for (String t : tables) {
//                int count = queryCount(conn, "SELECT COUNT(*) FROM " + t);
//                System.out.println("    " + t + ": " + count);
//            }
//            pass(name);
//        } catch (Exception e) { fail(name, e.getMessage()); }
//    }
//
//    // ── Helpers ───────────────────────────────────────────────────────────────
//    private static int queryCount(Connection conn, String sql) throws SQLException {
//        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
//            rs.next(); return rs.getInt(1);
//        }
//    }
//
//    private static long queryLong(Connection conn, String sql) throws SQLException {
//        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
//            rs.next(); return rs.getLong(1);
//        }
//    }
//
//    private static void pass(String name) { PASSED.add(name); System.out.println("    PASS: " + name); }
//    private static void fail(String name, String reason) { FAILED.add(name + ": " + reason); System.out.println("    FAIL: " + name + " — " + reason); }
//
//    private static void printSummary() {
//        System.out.println("\n========================================");
//        System.out.println("  RESULTS: " + PASSED.size() + " passed, " + FAILED.size() + " failed");
//        System.out.println("========================================");
//        if (!FAILED.isEmpty()) {
//            System.out.println("FAILURES:");
//            FAILED.forEach(f -> System.out.println("  ✗ " + f));
//        } else {
//            System.out.println("  All tests passed! DB is correctly set up.");
//        }
//    }
//}
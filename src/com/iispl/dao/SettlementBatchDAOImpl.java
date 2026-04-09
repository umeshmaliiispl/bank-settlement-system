package com.iispl.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementBatch;
import com.iispl.entity.SettlementRecord;
import com.iispl.enums.BatchStatus;

public class SettlementBatchDAOImpl implements SettlementBatchDAO {

    // ─────────────────────────────────────────────────────────────
    // SAVE BATCH
    // ─────────────────────────────────────────────────────────────
    @Override
    public void save(SettlementBatch batch) {

    	String sql = "INSERT INTO settlement_batch "
    		    + "(batch_id, batch_date, batch_status, run_by, run_at) "
    		    + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1,    batch.getBatchId());
            ps.setDate(2,      Date.valueOf(batch.getBatchDate()));
            ps.setString(3,    batch.getBatchStatus().name());
            ps.setString(4,    batch.getRunBy());
            ps.setTimestamp(5, Timestamp.valueOf(batch.getRunAt()));

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY BATCH ID
    // ─────────────────────────────────────────────────────────────
    @Override
    public SettlementBatch findByBatchId(String batchId) {

        String sql = "SELECT * FROM settlement_batch WHERE batch_id = ?";
        SettlementBatch batch = null;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, batchId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                batch = new SettlementBatch();

                batch.setBatchId(rs.getString("batch_id"));

                batch.setBatchDate(
                    rs.getTimestamp("batch_date").toLocalDateTime().toLocalDate()
                );

                batch.setBatchStatus(
                    BatchStatus.valueOf(rs.getString("batch_status"))
                );

                batch.setRunBy(rs.getString("run_by"));

                if (rs.getTimestamp("run_at") != null) {
                    batch.setRunAt(rs.getTimestamp("run_at").toLocalDateTime());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return batch;
    }

    // ─────────────────────────────────────────────────────────────
    // CREATE SETTLEMENT BATCH — returns batchId
    // ─────────────────────────────────────────────────────────────
    @Override
    public String createSettlementBatch(List<IncomingTransaction> txns) {

        if (txns == null || txns.isEmpty()) {
            System.out.println("No transactions available for settlement.");
            return null;
        }

        SettlementRecordDAO recordDAO = new SettlementRecordDAOImpl();

        // RBI-style Batch ID
        String batchId = "B-"
                + java.time.LocalDate.now().toString().replace("-", "")
                + "-" + System.currentTimeMillis();

        SettlementBatch batch = new SettlementBatch();
        batch.setBatchId(batchId);
        batch.setBatchDate(java.time.LocalDate.now());
        batch.setBatchStatus(BatchStatus.SCHEDULED);
        batch.setTotalTransactions(txns.size());

        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        for (IncomingTransaction txn : txns) {
            totalAmount = totalAmount.add(txn.getAmount());
        }

        batch.setTotalAmount(totalAmount);
        batch.setRunBy("SYSTEM");
        batch.setRunAt(java.time.LocalDateTime.now());

        // STEP 1: Save Batch header
        try {
            save(batch);
            System.out.println("✅ Batch Created: " + batchId);
        } catch (Exception e) {
            System.out.println("❌ Batch creation failed. Stopping settlement.");
            e.printStackTrace();
            return null;
        }

        // STEP 2: Save Settlement Records
        for (IncomingTransaction txn : txns) {

            SettlementRecord record = new SettlementRecord();
            record.setBatchId(batchId);

            // ✅ FIX: getIncomingTxnId() returns the DB 'id' column value
            record.setIncomingTxnId(txn.getIncomingTxnId());

            recordDAO.save(record);

            System.out.printf(
                "Record Created | Batch=%-30s | TxnId=%d%n",
                batchId,
                txn.getIncomingTxnId()
            );
        }

        System.out.println("Settlement Batch Completed!");

        // ✅ RETURN batchId so service can call report
        return batchId;
    }
    
    
    @Override
    public List<SettlementBatch> getSettlementBatch() {

        List<SettlementBatch> list = new ArrayList<>();

        String sql = "SELECT * FROM settlement_batch ORDER BY batch_date DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                SettlementBatch batch = new SettlementBatch();

                batch.setBatchId(rs.getString("batch_id"));

                // batch_date → TIMESTAMP → convert to LocalDate
                batch.setBatchDate(
                    rs.getTimestamp("batch_date").toLocalDateTime().toLocalDate()
                );

                batch.setBatchStatus(
                    com.iispl.enums.BatchStatus.valueOf(rs.getString("batch_status"))
                );

                batch.setRunBy(rs.getString("run_by"));

                // run_at → TIMESTAMP → LocalDateTime
                if (rs.getTimestamp("run_at") != null) {
                    batch.setRunAt(
                        rs.getTimestamp("run_at").toLocalDateTime()
                    );
                }

                list.add(batch);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
    

    // ─────────────────────────────────────────────────────────────
    // PRINT BANK-WISE SETTLEMENT REPORT
    // ─────────────────────────────────────────────────────────────
    public void printBankWiseSettlement(String batchId) {

        // This SQL calculates credit and debit per bank
        // Credit  = bank is RECEIVER (money coming in)
        // Debit   = bank is SENDER   (money going out)
        String sql = "SELECT "
                + "    bank_name, "
                + "    SUM(credit) AS total_credit, "
                + "    SUM(debit)  AS total_debit, "
                + "    SUM(credit - debit) AS net_amount "
                + "FROM ( "

                // CREDIT SIDE — Receiver banks
                + "    SELECT "
                + "        it.receiver_bank_name AS bank_name, "
                + "        it.amount AS credit, "
                + "        0 AS debit "
                + "    FROM settlement_record sr "
                + "    JOIN incoming_transaction it ON sr.incoming_txn_id = it.id "
                + "    WHERE sr.batch_id = ? "

                + "    UNION ALL "

                // DEBIT SIDE — Sender banks
                + "    SELECT "
                + "        it.sender_bank_name AS bank_name, "
                + "        0 AS credit, "
                + "        it.amount AS debit "
                + "    FROM settlement_record sr "
                + "    JOIN incoming_transaction it ON sr.incoming_txn_id = it.id "
                + "    WHERE sr.batch_id = ? "

                + ") t "
                + "GROUP BY bank_name "
                + "ORDER BY bank_name";

        // Collect settlement types for the header line
        String settlementTypeSql = "SELECT DISTINCT it.channel_code "
                + "FROM settlement_record sr "
                + "JOIN incoming_transaction it ON sr.incoming_txn_id = it.id "
                + "WHERE sr.batch_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement typePs = conn.prepareStatement(settlementTypeSql)) {

            typePs.setString(1, batchId);
            ResultSet typeRs = typePs.executeQuery();

            // Build "UPI / NEFT / RTGS" style string
            StringBuilder settlementTypes = new StringBuilder();
            while (typeRs.next()) {
                if (settlementTypes.length() > 0) {
                    settlementTypes.append(" / ");
                }
                settlementTypes.append(typeRs.getString("channel_code"));
            }

            // Now run the main bank-wise query
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, batchId);
                ps.setString(2, batchId);

                ResultSet rs = ps.executeQuery();

                double totalCredit = 0;
                double totalDebit  = 0;

                // ── HEADER ──────────────────────────────────────────────
                System.out.println("\n=========================================================");
                System.out.println("            BANK SETTLEMENT SUMMARY REPORT");
                System.out.println("=========================================================");

                // Format date like "07-Apr-2026"
                java.time.LocalDate today = java.time.LocalDate.now();
                String formattedDate = String.format("%02d-%s-%d",
                        today.getDayOfMonth(),
                        today.getMonth().getDisplayName(
                            java.time.format.TextStyle.SHORT,
                            java.util.Locale.ENGLISH
                        ),
                        today.getYear()
                );

                System.out.println("Settlement Date : " + formattedDate);
                System.out.println("Batch ID        : " + batchId);
                System.out.println("Settlement Type : " + settlementTypes);

                // ── TABLE HEADER ─────────────────────────────────────────
                System.out.println("\n---------------------------------------------------------");
                System.out.printf(
                    "| %-16s | %-12s | %-11s | %-11s |\n",
                    "Bank Name", "Total Credit", "Total Debit", "Net Amt"
                );
                System.out.println("---------------------------------------------------------");

                // ── TABLE ROWS ───────────────────────────────────────────
                while (rs.next()) {

                    String bank   = safe(rs.getString("bank_name"));
                    double credit = rs.getDouble("total_credit");
                    double debit  = rs.getDouble("total_debit");
                    double net    = rs.getDouble("net_amount");

                    totalCredit += credit;
                    totalDebit  += debit;

                    // +1,50,000 or -1,50,000
                    String netFormatted = net >= 0
                            ? "+" + formatAmount(net)
                            : formatAmount(net);

                    System.out.printf(
                        "| %-16s | %-12s | %-11s | %-11s |\n",
                        bank,
                        formatAmount(credit),
                        formatAmount(debit),
                        netFormatted
                    );
                }

                System.out.println("---------------------------------------------------------");

                double finalNet = totalCredit - totalDebit;

                // ── NET SUMMARY ──────────────────────────────────────────
                System.out.println("\nNet Settlement Summary:");
                System.out.println("---------------------------------------------------------");
                System.out.println("Total Credits  : " + formatAmount(totalCredit));
                System.out.println("Total Debits   : " + formatAmount(totalDebit));
                System.out.println("Settlement Net : " + formatAmount(finalNet)
                        + (finalNet == 0 ? " (Balanced)" : " (MISMATCH)"));
                System.out.println("---------------------------------------------------------");

                // ── INSTRUCTIONS ─────────────────────────────────────────
                System.out.println("\nSettlement Instructions:");
                System.out.println("---------------------------------------------------------");
                System.out.println("Banks with Positive Net - Receive funds from RBI");
                System.out.println("Banks with Negative Net - Pay funds to RBI");
                System.out.println("---------------------------------------------------------");

                // ── FOOTER ───────────────────────────────────────────────
                // Format datetime like "07-Apr-2026 18:30:00"
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                String processedAt = String.format("%02d-%s-%d %02d:%02d:%02d",
                        now.getDayOfMonth(),
                        now.getMonth().getDisplayName(
                            java.time.format.TextStyle.SHORT,
                            java.util.Locale.ENGLISH
                        ),
                        now.getYear(),
                        now.getHour(),
                        now.getMinute(),
                        now.getSecond()
                );

                System.out.println("Status       : " + (finalNet == 0 ? "SUCCESS" : "MISMATCH"));
                System.out.println("Processed At : " + processedAt);
                System.out.println("=========================================================\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────

    // Formats number like 1,50,000.00
    private String formatAmount(double amount) {
        return String.format("%,.2f", amount);
    }

    // Returns "N/A" if value is null
    private String safe(String val) {
        return val == null ? "N/A" : val;
    }

	@Override
	public List<SettlementBatch> getAllBatches() {

	    List<SettlementBatch> list = new ArrayList<>();

	    String sql = "SELECT * FROM settlement_batch ORDER BY batch_date DESC";

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {

	            SettlementBatch batch = new SettlementBatch();

	            batch.setBatchId(rs.getString("batch_id"));

	            batch.setBatchDate(
	                rs.getTimestamp("batch_date").toLocalDateTime().toLocalDate()
	            );

	            batch.setBatchStatus(
	                BatchStatus.valueOf(rs.getString("batch_status"))
	            );

	            batch.setRunBy(rs.getString("run_by"));

	            if (rs.getTimestamp("run_at") != null) {
	                batch.setRunAt(rs.getTimestamp("run_at").toLocalDateTime());
	            }

	            list.add(batch);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	
	public static void updateBatchStatus(String batchId, String status) {

	    String sql = "UPDATE settlement_batch SET batch_status = ? WHERE batch_id = ?";

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setString(1, status);
	        ps.setString(2, batchId);

	        ps.executeUpdate();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	
}
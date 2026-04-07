package com.iispl.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.exception.DatabaseInsertException;

/**
 * TransactionDaoImpl — Compatible with immutable IncomingTransaction.
 *
 * Responsibilities:
 *   ✔ Save a new IncomingTransaction to DB (getters only — no mutation)
 *   ✔ Detect DUPLICATE via UNIQUE constraint (source_system_id + source_ref)
 *   ✔ Return false on duplicate  → caller logs and skips (no DB entry)
 *   ✔ Return true  on success    → caller logs the stage
 *   ✔ Throw DatabaseInsertException on any real/unexpected DB error
 *
 * Scenario handling:
 *
 *   CUSTOMER VALIDATION FAILED:
 *     → processingStatus = FAILED, errorMessage set by ValidationService
 *     → save() inserts the row → returns true
 *     → Record visible in report with FAILED status + validation remark
 *
 *   DUPLICATE TRANSACTION:
 *     → UNIQUE constraint fires → returns false (no insert, no exception)
 *     → Existing DB record is untouched (txnStatus and processingStatus unchanged)
 *     → Caller prints [DUPLICATE SKIPPED] log
 *
 *   SAME BANK:
 *     → processingStatus = FAILED, errorMessage = "Same bank transaction not allowed"
 *     → save() inserts the row → returns true
 *     → Record visible in report with FAILED status + same bank remark
 */
public class TransactionDaoImpl implements TransactionDao {

    private static final String INSERT_SQL =
            "INSERT INTO incoming_transaction ("
          + "source_system_id, source_ref, raw_payload, normalized_payload, "
          + "txn_type, amount, gross_amount, fee_amount, currency, value_date, "
          + "txn_status, processing_status, sender_ifsc, receiver_ifsc, "
          + "sender_bank_name, receiver_bank_name, sender_bic, receiver_bic, "
          + "partner_name, merchant_id, channel_code, checksum, error_message"
          + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    @Override
    public boolean save(IncomingTransaction txn) {

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {

            // All READ-ONLY access — no setters called on txn
            ps.setLong(1,        resolveSourceSystemId(txn.getChannelCode()));
            ps.setString(2,      txn.getSourceRef());
            ps.setString(3,      txn.getRawPayload());
            ps.setString(4,      txn.getNormalizedPayload());
            ps.setString(5,      txn.getTxnType().name());
            ps.setBigDecimal(6,  txn.getAmount());
            ps.setBigDecimal(7,  txn.getGrossAmount());
            ps.setBigDecimal(8,  txn.getFeeAmount());
            ps.setString(9,      txn.getCurrency());
            ps.setTimestamp(10,  java.sql.Timestamp.valueOf(txn.getValueDate()));

            // txnStatus  → source truth, stored exactly as received
            ps.setString(11,     txn.getTxnStatus().name());

            // processingStatus → internal decision (FAILED / QUEUED / RECEIVED)
            ps.setString(12,     txn.getProcessingStatus().name());

            ps.setString(13,     txn.getSenderIfsc());
            ps.setString(14,     txn.getReceiverIfsc());
            ps.setString(15,     txn.getSenderBankName());
            ps.setString(16,     txn.getReceiverBankName());
            ps.setString(17,     txn.getSenderBic());
            ps.setString(18,     txn.getReceiverBic());
            ps.setString(19,     txn.getPartnerName());
            ps.setString(20,     txn.getMerchantId());
            ps.setString(21,     txn.getChannelCode());
            ps.setString(22,     txn.getChecksum());

            // errorMessage → null for valid, set for FAILED scenarios
            ps.setString(23,     txn.getErrorMessage());

            ps.executeUpdate();
            return true; // ✅ Successfully inserted

        } catch (Exception e) {

            // ── DUPLICATE DETECTION ────────────────────────────────────────
            // DB UNIQUE constraint on (source_system_id, source_ref) fires.
            // This is NOT an error — it is expected business behaviour.
            // Existing record is left completely unchanged (txnStatus, processingStatus).
            // Caller (TransactionServiceImpl.insert) will log [DUPLICATE SKIPPED].
            if (isDuplicate(e)) {
                System.out.printf(
                    "[DUPLICATE][%-18s][%-7s] REF=%-22s | DB constraint hit → NOT INSERTED%n",
                    Thread.currentThread().getName(),
                    txn.getChannelCode(),
                    txn.getSourceRef()
                );
                return false; // ✅ Signal duplicate — do NOT throw
            }

            // ── REAL / UNEXPECTED DB ERROR ────────────────────────────────
            throw new DatabaseInsertException(
                "DB insert failed for REF=" + txn.getSourceRef(), e, txn
            );
        }
    }

    @Override
    public List<IncomingTransaction> findAll() {

        List<IncomingTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM incoming_transaction ORDER BY id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public List<IncomingTransaction> findSuccessfulTransactions() {

        List<IncomingTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM incoming_transaction "
                   + "WHERE txn_status = 'SUCCESS' AND processing_status = 'QUEUED'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ── Row Mapper — builds NEW immutable object from DB ──────────────────────

    private IncomingTransaction mapRow(ResultSet rs) throws Exception {
        return new IncomingTransaction.Builder()
                .incomingTxnId(rs.getLong("id"))
                .sourceRef(rs.getString("source_ref"))
                .channelCode(rs.getString("channel_code"))
                .senderIfsc(rs.getString("sender_ifsc"))
                .receiverIfsc(rs.getString("receiver_ifsc"))
                .senderBankName(rs.getString("sender_bank_name"))
                .receiverBankName(rs.getString("receiver_bank_name"))
                .amount(rs.getBigDecimal("amount"))
                .currency(rs.getString("currency"))
                .txnStatus(
                    com.iispl.enums.TransactionStatus.valueOf(rs.getString("txn_status"))
                )
                .processingStatus(
                    com.iispl.enums.ProcessingStatus.valueOf(rs.getString("processing_status"))
                )
                .txnType(
                    com.iispl.enums.TransactionType.valueOf(rs.getString("txn_type"))
                )
                .errorMessage(rs.getString("error_message"))
                .build();
    }

    // ── Duplicate Detection ────────────────────────────────────────────────────
    // Works for MySQL ("Duplicate entry"), PostgreSQL ("unique constraint"),
    // H2/HSQLDB, and any JDBC driver that surfaces the keyword.

    private boolean isDuplicate(Exception e) {
        if (e == null || e.getMessage() == null) return false;
        String msg = e.getMessage().toLowerCase();
        return msg.contains("duplicate") || msg.contains("unique");
    }

    // ── Source System ID Resolver ──────────────────────────────────────────────

    private Long resolveSourceSystemId(String channelCode) {
        if (channelCode == null) return 1L;
        switch (channelCode) {
            case "CBS":     return 1L;
            case "NEFT":    return 2L;
            case "UPI":     return 3L;
            case "RTGS":    return 4L;
            case "SWIFT":   return 5L;
            case "FINTECH": return 6L;
            default:        return 1L;
        }
    }
}


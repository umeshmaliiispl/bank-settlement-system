package com.iispl.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.exception.DatabaseInsertException;
import com.iispl.utility.DBConnection;

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
    public void checkConnection() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1")) {

            ps.executeQuery(); // just executes, no heavy data

        } catch (SQLException e) {
            throw new RuntimeException("DB connection failed", e);
        }
    }
    
    @Override
    public boolean save(IncomingTransaction txn) {

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {

            // All READ-ONLY access — no setters called on txn
        	preparedStatement.setLong(1,        resolveSourceSystemId(txn.getChannelCode()));
        	preparedStatement.setString(2,      txn.getSourceRef());
        	preparedStatement.setString(3,      txn.getRawPayload());
        	preparedStatement.setString(4,      txn.getNormalizedPayload());
        	preparedStatement.setString(5,      txn.getTxnType().name());
        	preparedStatement.setBigDecimal(6,  txn.getAmount());
        	preparedStatement.setBigDecimal(7,  txn.getGrossAmount());
        	preparedStatement.setBigDecimal(8,  txn.getFeeAmount());
        	preparedStatement.setString(9,      txn.getCurrency());
        	preparedStatement.setTimestamp(10,  java.sql.Timestamp.valueOf(txn.getValueDate()));

            // txnStatus  → source truth, stored exactly as received
        	preparedStatement.setString(11,     txn.getTxnStatus().name());

            // processingStatus → internal decision (FAILED / QUEUED / RECEIVED)
        	preparedStatement.setString(12,     txn.getProcessingStatus().name());

        	preparedStatement.setString(13,     txn.getSenderIfsc());
        	preparedStatement.setString(14,     txn.getReceiverIfsc());
        	preparedStatement.setString(15,     txn.getSenderBankName());
        	preparedStatement.setString(16,     txn.getReceiverBankName());
        	preparedStatement.setString(17,     txn.getSenderBic());
            preparedStatement.setString(18,     txn.getReceiverBic());
            preparedStatement.setString(19,     txn.getPartnerName());
            preparedStatement.setString(20,     txn.getMerchantId());
            preparedStatement.setString(21,     txn.getChannelCode());
            preparedStatement.setString(22,     txn.getChecksum());

            // errorMessage ->  null for valid, set for FAILED scenarios
            preparedStatement.setString(23,     txn.getErrorMessage());

            preparedStatement.executeUpdate();
            return true; // Successfully inserted

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
                return false; // Signal duplicate - do NOT throw
            }

            // REAL / UNEXPECTED DB ERROR 
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

    // Row Mapper builds NEW immutable object from DB 

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
                .valueDate(
                    rs.getTimestamp("value_date") != null
                        ? rs.getTimestamp("value_date").toLocalDateTime()
                        : null
                )
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

   

    private boolean isDuplicate(Exception e) {
        if (e == null || e.getMessage() == null) return false;
        String msg = e.getMessage().toLowerCase();
        return msg.contains("duplicate") || msg.contains("unique");
    }

    // Source System ID Resolver 

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




	/**
	 * @return true if inserted, false if duplicate skipped
	 */
	public boolean saveIfNotDuplicate(IncomingTransaction transaction) {

		try (Connection connection = DatabaseConfig.getConnection();
				PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {

			ps.setLong(1, resolveSourceSystemId(transaction.getChannelCode()));
			ps.setString(2, transaction.getSourceRef());
			ps.setString(3, transaction.getRawPayload());
			ps.setString(4, transaction.getNormalizedPayload());

			ps.setString(5, transaction.getTxnType().name());
			ps.setBigDecimal(6, transaction.getAmount());
			ps.setBigDecimal(7, transaction.getGrossAmount());
			ps.setBigDecimal(8, transaction.getFeeAmount());
			ps.setString(9, transaction.getCurrency());
			ps.setObject(10, transaction.getValueDate());

			ps.setString(11, transaction.getTxnStatus().name());
			ps.setString(12, transaction.getProcessingStatus().name());

			ps.setString(13, transaction.getSenderIfsc());
			ps.setString(14, transaction.getReceiverIfsc());

			ps.setString(15, transaction.getSenderBankName());
			ps.setString(16, transaction.getReceiverBankName());

			ps.setString(17, transaction.getSenderBic());
			ps.setString(18, transaction.getReceiverBic());

			ps.setString(19, transaction.getPartnerName());
			ps.setString(20, transaction.getMerchantId());

			ps.setString(21, transaction.getChannelCode());
			ps.setString(22, transaction.getChecksum());
			ps.setString(23, transaction.getErrorMessage());

			int rowsInserted = ps.executeUpdate();

			return rowsInserted > 0;

		} catch (Exception exception) {

			throw new DatabaseInsertException("Database insert failed for reference: " + transaction.getSourceRef(),
					exception, transaction);
		}
	}


    @Override
    public List<IncomingTransaction> getUnsettledTranasactions() {

        List<IncomingTransaction> list = new ArrayList<>();

        String sql = "SELECT it.* " +
                     "FROM incoming_transaction it " +
                     "LEFT JOIN settlement_record sr " +
                     "ON it.id = sr.incoming_txn_id " +
                     "WHERE sr.incoming_txn_id IS NULL " +
                     "AND it.txn_status = 'SUCCESS' " +
                     "AND it.processing_status = 'QUEUED'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                IncomingTransaction txn = new IncomingTransaction.Builder()
                        .incomingTxnId(rs.getLong("id"))
                        .sourceRef(rs.getString("source_ref"))
                        .channelCode(rs.getString("channel_code"))
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
                        .valueDate(
                            rs.getTimestamp("value_date") != null
                                ? rs.getTimestamp("value_date").toLocalDateTime()
                                : null
                        )
                        .build();

                list.add(txn);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }


}

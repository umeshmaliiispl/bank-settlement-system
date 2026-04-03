package com.iispl.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.exception.DatabaseInsertException;

public class TransactionDaoImpl implements TransactionDao {

	private static final String INSERT_SQL = "INSERT INTO incoming_transaction ("
			+ "source_system_id, source_ref, raw_payload, normalized_payload, "
			+ "txn_type, amount, gross_amount, fee_amount, currency, value_date, "
			+ "txn_status, processing_status, sender_ifsc, receiver_ifsc, "
			+ "sender_bank_name, receiver_bank_name, sender_bic, receiver_bic, "
			+ "partner_name, merchant_id, channel_code, checksum, error_message"
			+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
			+ "ON CONFLICT (source_system_id, source_ref) DO NOTHING"; // Prevent duplicate crash

	@Override
	public void save(IncomingTransaction transaction) {

		try (Connection connection = DatabaseConfig.getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {

			preparedStatement.setLong(1, resolveSourceSystemId(transaction.getChannelCode()));
			preparedStatement.setString(2, transaction.getSourceRef());
			preparedStatement.setString(3, transaction.getRawPayload());
			preparedStatement.setString(4, transaction.getNormalizedPayload());

			preparedStatement.setString(5, transaction.getTxnType().name());
			preparedStatement.setBigDecimal(6, transaction.getAmount());
			preparedStatement.setBigDecimal(7, transaction.getGrossAmount());
			preparedStatement.setBigDecimal(8, transaction.getFeeAmount());
			preparedStatement.setString(9, transaction.getCurrency());

			preparedStatement.setTimestamp(10, java.sql.Timestamp.valueOf(transaction.getValueDate()));

			preparedStatement.setString(11, transaction.getTxnStatus().name());
			preparedStatement.setString(12, transaction.getProcessingStatus().name());

			preparedStatement.setString(13, transaction.getSenderIfsc());
			preparedStatement.setString(14, transaction.getReceiverIfsc());

			preparedStatement.setString(15, transaction.getSenderBankName());
			preparedStatement.setString(16, transaction.getReceiverBankName());

			preparedStatement.setString(17, transaction.getSenderBic());
			preparedStatement.setString(18, transaction.getReceiverBic());

			preparedStatement.setString(19, transaction.getPartnerName());
			preparedStatement.setString(20, transaction.getMerchantId());

			preparedStatement.setString(21, transaction.getChannelCode());
			preparedStatement.setString(22, transaction.getChecksum());
			preparedStatement.setString(23, transaction.getErrorMessage());

			preparedStatement.executeUpdate();

		} catch (Exception exception) {

			throw new DatabaseInsertException("Database insert failed for reference: " + transaction.getSourceRef(),
					exception, transaction);
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

	public List<IncomingTransaction> findAll() {

		List<IncomingTransaction> list = new ArrayList<>();

		String sql = "SELECT * FROM incoming_transaction ORDER BY id DESC";

		try (Connection conn = DatabaseConfig.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				IncomingTransaction txn = new IncomingTransaction();

				txn.setIncomingTxnId(rs.getLong("id"));
				txn.setSourceRef(rs.getString("source_ref"));
				txn.setChannelCode(rs.getString("channel_code"));

				txn.setSenderIfsc(rs.getString("sender_ifsc"));
				txn.setReceiverIfsc(rs.getString("receiver_ifsc"));

				txn.setSenderBankName(rs.getString("sender_bank_name"));
				txn.setReceiverBankName(rs.getString("receiver_bank_name"));

				txn.setAmount(rs.getBigDecimal("amount"));
				txn.setCurrency(rs.getString("currency"));

				txn.setTxnStatus(com.iispl.enums.TransactionStatus.valueOf(rs.getString("txn_status")));

				txn.setProcessingStatus(com.iispl.enums.ProcessingStatus.valueOf(rs.getString("processing_status")));

				txn.setTxnType(com.iispl.enums.TransactionType.valueOf(rs.getString("txn_type")));

				txn.setErrorMessage(rs.getString("error_message"));

				list.add(txn);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public List<IncomingTransaction> findSuccessfulTransactions() {

		List<IncomingTransaction> list = new ArrayList<>();

		String sql = "SELECT * FROM incoming_transaction WHERE txn_status = 'SUCCESS'";

		try (Connection conn = DatabaseConfig.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				IncomingTransaction txn = new IncomingTransaction();

				txn.setIncomingTxnId(rs.getLong("id"));
				txn.setSourceRef(rs.getString("source_ref"));
				txn.setSenderBankName(rs.getString("sender_bank_name"));
				txn.setReceiverBankName(rs.getString("receiver_bank_name"));
				txn.setChannelCode(rs.getString("channel_code"));

				txn.setAmount(rs.getBigDecimal("amount"));
				txn.setCurrency(rs.getString("currency"));

				txn.setTxnStatus(com.iispl.enums.TransactionStatus.valueOf(rs.getString("txn_status")));

				txn.setProcessingStatus(com.iispl.enums.ProcessingStatus.valueOf(rs.getString("processing_status")));

				txn.setTxnType(com.iispl.enums.TransactionType.valueOf(rs.getString("txn_type")));

				list.add(txn);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	private Long resolveSourceSystemId(String channelCode) {
		switch (channelCode) {
		case "CBS":
			return 1L;
		case "NEFT":
			return 2L;
		case "UPI":
			return 3L;
		case "RTGS":
			return 4L;
		case "SWIFT":
			return 5L;
		case "FINTECH":
			return 6L;
		default:
			return 1L;
		}
	}
}
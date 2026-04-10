
package com.iispl.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementRecord;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.SettlementStatus;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

public class SettlementRecordDAOImpl implements SettlementRecordDAO {

	@Override
	public synchronized void save(SettlementRecord record) {

	    String sql = "INSERT INTO settlement_record " +
	                 "(batch_id, incoming_txn_id, settled_date, settled_status) " +
	                 "VALUES (?, ?, NOW(), ?)";

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setString(1, record.getBatchId());
	        ps.setLong(2, record.getIncomingTxnId());
	        ps.setString(3, record.getSettledStatus().name());

	        ps.executeUpdate();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	@Override
	public List<SettlementRecord> findByBatchId(String batchId) {

	    List<SettlementRecord> list = new ArrayList<>();

	    String sql = "SELECT sr.*, it.* FROM settlement_record sr " +
	                 "JOIN incoming_transaction it ON sr.incoming_txn_id = it.id " +
	                 "WHERE sr.batch_id = ?";

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setString(1, batchId);
	        ResultSet rs = ps.executeQuery();

	        while (rs.next()) {

	            SettlementRecord record = new SettlementRecord();

	            record.setBatchId(rs.getString("batch_id"));
	            record.setIncomingTxnId(rs.getLong("incoming_txn_id"));

	            if (rs.getTimestamp("settled_date") != null) {
	                record.setSettledDate(
	                    rs.getTimestamp("settled_date").toLocalDateTime()
	                );
	            }

	            record.setSettledStatus(
	                SettlementStatus.valueOf(rs.getString("settled_status"))
	            );

	            IncomingTransaction txn = new IncomingTransaction.Builder()
	                    .incomingTxnId(rs.getLong("incoming_txn_id")) // 🔥 FIX (important)
	                    .sourceRef(rs.getString("source_ref"))
	                    .channelCode(rs.getString("channel_code"))

	                    .txnType(rs.getString("txn_type") != null ? 
	                             TransactionType.valueOf(rs.getString("txn_type")) : null)

	                    .txnStatus(rs.getString("txn_status") != null ? 
	                               TransactionStatus.valueOf(rs.getString("txn_status")) : null)

	                    .processingStatus(rs.getString("processing_status") != null ? 
	                                      ProcessingStatus.valueOf(rs.getString("processing_status")) : null)

	                    .amount(rs.getBigDecimal("amount"))
	                    .grossAmount(rs.getBigDecimal("gross_amount"))
	                    .feeAmount(rs.getBigDecimal("fee_amount"))
	                    .currency(rs.getString("currency"))

	                    .senderIfsc(rs.getString("sender_ifsc"))
	                    .receiverIfsc(rs.getString("receiver_ifsc"))
	                    .senderBankName(rs.getString("sender_bank_name"))
	                    .receiverBankName(rs.getString("receiver_bank_name"))

	                    .senderBic(rs.getString("sender_bic"))
	                    .receiverBic(rs.getString("receiver_bic"))

	                    .partnerName(rs.getString("partner_name"))
	                    .merchantId(rs.getString("merchant_id"))

	                    .checksum(rs.getString("checksum"))
	                    .errorMessage(rs.getString("error_message"))

	                    .priority(rs.getInt("priority"))

	                    .valueDate(rs.getTimestamp("value_date") != null ?
	                            rs.getTimestamp("value_date").toLocalDateTime() : null)

	                    .rawPayload(rs.getString("raw_payload"))
	                    .normalizedPayload(rs.getString("normalized_payload"))

	                    .build();

	            record.setSettledAmount(txn.getAmount());

	            // If you still want list (your current design)
	            record.getSettlementBatchRecordTransactions().add(txn);

	            list.add(record);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
}

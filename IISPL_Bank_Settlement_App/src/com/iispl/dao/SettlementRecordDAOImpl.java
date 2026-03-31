package com.iispl.dao;

import java.sql.*;
import java.util.*;
import com.iispl.config.DatabaseConfig;
import com.iispl.entity.SettlementRecord;

public class SettlementRecordDAOImpl implements SettlementRecordDAO {

	@Override
	public synchronized void save(SettlementRecord record) {

		String sql = "INSERT INTO settlement_record (batch_id, incoming_txn_id, settled_amount, settled_status) VALUES (?, ?, ?, ?)";

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, record.getBatchId());
			ps.setLong(2, record.getIncomingTxnId());
			ps.setDouble(3, record.getSettledAmount());
			ps.setString(4, record.getSettledStatus().name());

			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<SettlementRecord> findByBatchId(String batchId) {
		return new ArrayList<>();
	}
}
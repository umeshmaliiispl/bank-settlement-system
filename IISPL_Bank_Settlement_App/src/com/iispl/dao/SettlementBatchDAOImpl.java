package com.iispl.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.SettlementBatch;

public class SettlementBatchDAOImpl implements SettlementBatchDAO {

	@Override
	public void save(SettlementBatch batch) {

		String sql = "INSERT INTO settlement_batch (batch_id, batch_date, batch_status, total_transactions, total_amount, run_by, run_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, batch.getBatchId());
			ps.setDate(2, Date.valueOf(batch.getBatchDate()));
			ps.setString(3, batch.getBatchStatus().name());
			ps.setInt(4, batch.getTotalTransactions());
			ps.setBigDecimal	(5, batch.getTotalAmount());
			ps.setString(6, batch.getRunBy());
			ps.setTimestamp(7, Timestamp.valueOf(batch.getRunAt()));

			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public SettlementBatch findByBatchId(String batchId) {
		return null; // optional
	}
}
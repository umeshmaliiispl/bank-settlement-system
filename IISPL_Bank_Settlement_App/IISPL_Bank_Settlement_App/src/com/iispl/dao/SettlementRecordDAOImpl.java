package com.iispl.dao;

import java.sql.*;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.SettlementRecord;

public class SettlementRecordDAOImpl implements SettlementRecordDAO {

    @Override
    public void save(SettlementRecord record) {

        String sql = "INSERT INTO settlement_record (batch_id, incoming_txn_id, settled_amount, settled_status, settled_date) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, record.getBatchId());
            ps.setLong(2, record.getIncomingTxnId());
            ps.setBigDecimal(3, record.getSettledAmount());
            ps.setString(4, record.getSettledStatus().name());
            ps.setTimestamp(5, Timestamp.valueOf(record.getSettledDate()));

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
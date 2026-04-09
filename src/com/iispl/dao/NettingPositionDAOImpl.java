package com.iispl.dao;

import com.iispl.entity.NettingPosition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class NettingPositionDAOImpl implements NettingPositionDAO {

    private Connection conn;

    public NettingPositionDAOImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void saveAll(List<NettingPosition> list) {

        String sql = "INSERT INTO netting_position (" +
                "bank_name, position_date, total_credit, total_debit, net_amount, direction) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (bank_name, position_date) DO UPDATE SET " +
                "total_credit = EXCLUDED.total_credit, " +
                "total_debit = EXCLUDED.total_debit, " +
                "net_amount = EXCLUDED.net_amount, " +
                "direction = EXCLUDED.direction";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            for (NettingPosition np : list) {

                ps.setString(1, np.getSenderBank());
                ps.setDate(2, java.sql.Date.valueOf(np.getPositionDate()));

                ps.setBigDecimal(3, np.getTotalDepositAmount());
                ps.setBigDecimal(4, np.getTotalWithdrawAmount());
                ps.setBigDecimal(5, np.getNetAmount());

                ps.setString(6, np.getDirection().name());

                ps.addBatch();
            }

            ps.executeBatch();

            System.out.println("✅ Netting saved/updated successfully in DB!");

        } catch (Exception e) {
            e.printStackTrace(); // keep for debugging
        }
    }
}
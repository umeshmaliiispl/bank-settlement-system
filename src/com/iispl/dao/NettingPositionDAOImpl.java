package com.iispl.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.NettingPosition;

public class NettingPositionDAOImpl implements NettingPositionDAO {

	// No-arg constructor (used by SettlementServiceImpl internally)
	public NettingPositionDAOImpl() {
	}

	// Connection constructor (used by NettingServiceImpl via MainApp)
	public NettingPositionDAOImpl(Connection conn) {
		// Connection accepted for API compatibility; pool is used per-operation.
	}

	@Override
	public void saveAll(List<NettingPosition> list) {

		String sql = "INSERT INTO netting_position ("
				+ "bank_name, position_date, total_credit, total_debit, net_amount, direction) "
				+ "VALUES (?, ?, ?, ?, ?, ?) " + "ON CONFLICT (bank_name, position_date) DO UPDATE SET "
				+ "total_credit = EXCLUDED.total_credit, " + "total_debit = EXCLUDED.total_debit, "
				+ "net_amount = EXCLUDED.net_amount, " + "direction = EXCLUDED.direction";

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			for (NettingPosition np : list) {

				// EXACT MATCH WITH DB
				ps.setString(1, np.getSenderBank()); // bank_name
				ps.setDate(2, java.sql.Date.valueOf(np.getPositionDate())); // position_date
				ps.setBigDecimal(3, np.getTotalDepositAmount()); // total_credit
				ps.setBigDecimal(4, np.getTotalWithdrawAmount()); // total_debit
				ps.setBigDecimal(5, np.getNetAmount()); // net_amount
				ps.setString(6, np.getDirection().name()); // direction

				ps.addBatch();
			}

			ps.executeBatch();

			System.out.println("✅ Netting saved/updated successfully in DB!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
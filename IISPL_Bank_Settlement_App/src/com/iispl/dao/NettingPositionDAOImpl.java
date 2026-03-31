package com.iispl.dao;

import java.sql.*;
import com.iispl.config.DatabaseConfig;
import com.iispl.entity.NettingPosition;

public class NettingPositionDAOImpl implements NettingPositionDAO {

	@Override
	public void save(NettingPosition pos) {

		String sql = "INSERT INTO netting_position (counterparty_bank_id, currency, gross_debit_amount, gross_credit_amount, net_amount, direction) VALUES (?, ?, ?, ?, ?, ?)";

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setLong(1, pos.getCounterpartyBankId());
			ps.setString(2, pos.getCurrency());
			ps.setDouble(3, pos.getGrossDebitAmount());
			ps.setDouble(4, pos.getGrossCreditAmount());
			ps.setDouble(5, pos.getNetAmount());
			ps.setString(6, pos.getDirection().name());

			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
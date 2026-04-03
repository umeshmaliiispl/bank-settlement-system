package com.iispl.dao;

import java.sql.*;
import com.iispl.config.DatabaseConfig;
import com.iispl.entity.NettingPosition;

public class NettingPositionDAOImpl implements NettingPositionDAO {

	@Override
	public void save(NettingPosition pos) {

		String sql = "INSERT INTO netting_position " +
		        "(bank_name, currency, gross_debit_amount, gross_credit_amount, net_amount, direction, position_date, created_by) " +
		        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, ?) " +
		        "ON CONFLICT (bank_name, currency, position_date) DO UPDATE SET " +
		        "gross_debit_amount = netting_position.gross_debit_amount + EXCLUDED.gross_debit_amount, " +
		        "gross_credit_amount = netting_position.gross_credit_amount + EXCLUDED.gross_credit_amount, " +
		        "net_amount = netting_position.net_amount + EXCLUDED.net_amount, " +
		        "direction = EXCLUDED.direction, " +
		        "updated_at = NOW();";
		
		
		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, pos.getBankName());
			ps.setString(2, pos.getCurrency());
			ps.setDouble(3, pos.getGrossDebitAmount());
			ps.setDouble(4, pos.getGrossCreditAmount());
			ps.setDouble(5, pos.getNetAmount());
			ps.setString(6, pos.getDirection().name());
			ps.setString(7, "SYSTEM");  

			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
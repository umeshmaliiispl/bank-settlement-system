package com.iispl.dao;

import java.sql.*;
import com.iispl.config.DatabaseConfig;
import com.iispl.entity.SettlementInstruction;

public class SettlementInstructionDAOImpl implements SettlementInstructionDAO {

	@Override
	public void save(SettlementInstruction ins) {

		String sql = "INSERT INTO settlement_instruction (instruction_id, transaction_id, channel, instruction_status) VALUES (?, ?, ?, ?)";

		try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, ins.getInstructionId());
			ps.setLong(2, ins.getTransactionId());
			ps.setString(3, ins.getChannel().name());
			ps.setString(4, ins.getInstructionStatus().name());

			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
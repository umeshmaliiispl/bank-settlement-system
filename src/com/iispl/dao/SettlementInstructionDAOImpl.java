package com.iispl.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.SettlementInstruction;

public class SettlementInstructionDAOImpl implements SettlementInstructionDAO {

	@Override
	public void save(SettlementInstruction ins) {

	    String sql = "INSERT INTO settlement_instruction " +
	            "(instruction_id, from_bank, to_bank, amount, channel, instruction_status, value_date, created_by) " +
	            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	    try (Connection conn = DatabaseConfig.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	    	LocalDate valueDate = ins.getValueDate();

	    	if (valueDate == null) {
	    	    valueDate = LocalDate.now();
	    	}

	    	
	        ps.setString(1, ins.getInstructionId());
	        ps.setString(2, ins.getFromBank());
	        ps.setString(3, ins.getToBank());
	        ps.setDouble(4, ins.getAmount());
	        ps.setString(5, ins.getChannel().name());
	        ps.setString(6, ins.getInstructionStatus().name());
	    	ps.setDate(7, java.sql.Date.valueOf(valueDate));
	        ps.setString(8, "SYSTEM");

	        ps.executeUpdate();

	        System.out.println("✔ INSTRUCTION SAVED: " + ins.getInstructionId());

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
}


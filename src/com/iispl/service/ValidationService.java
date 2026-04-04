package com.iispl.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.ProcessingStatus;

public class ValidationService {

	public void validate(IncomingTransaction txn) {

		try (Connection conn = DatabaseConfig.getConnection()) {

			// Validate Sender
			validateAccountAndCustomer(conn, txn.getSenderAccount(), txn, "SENDER");

			// Validate Receiver
			validateAccountAndCustomer(conn, txn.getReceiverAccount(), txn, "RECEIVER");

			// If all checks pass , correcet Then
			txn.setProcessingStatus(ProcessingStatus.VALIDATED);

		} catch (Exception e) {
			txn.setProcessingStatus(ProcessingStatus.FAILED);
			txn.setErrorMessage(e.getMessage());
		}
	}

	private void validateAccountAndCustomer(Connection conn, String accountNumber, IncomingTransaction txn, String type)
			throws Exception {

		// 1. ACCOUNT LOOKUP
		String accSql = "SELECT customer_id, account_status FROM account WHERE account_number = ?";

		String customerId;
		String accountStatus;

		try (PreparedStatement ps = conn.prepareStatement(accSql)) {
			ps.setString(1, accountNumber);

			ResultSet rs = ps.executeQuery();

			if (!rs.next()) {
				throw new Exception(type + " ACCOUNT NOT FOUND: " + accountNumber);
			}

			customerId = rs.getString("customer_id");
			accountStatus = rs.getString("account_status");
		}

		// 2. ACCOUNT STATUS CHECK
		if (!"ACTIVE".equalsIgnoreCase(accountStatus)) {
			throw new Exception(type + " ACCOUNT BLOCKED: " + accountNumber);
		}

		// 3. CUSTOMER LOOKUP
		String custSql = "SELECT kyc_status, customer_status FROM customer WHERE customer_id = ?";

		String kycStatus;
		String customerStatus;

		try (PreparedStatement ps = conn.prepareStatement(custSql)) {
			ps.setString(1, customerId);

			ResultSet rs = ps.executeQuery();

			if (!rs.next()) {
				throw new Exception(type + " CUSTOMER NOT FOUND: " + customerId);
			}

			kycStatus = rs.getString("kyc_status");
			customerStatus = rs.getString("customer_status");
		}

		// 4. CUSTOMER STATUS CHECK
		if (!"ACTIVE".equalsIgnoreCase(customerStatus)) {
			throw new Exception(type + " CUSTOMER INACTIVE: " + customerId);
		}

		// 5. KYC CHECK (FLAG ONLY)
		if (!"VERIFIED".equalsIgnoreCase(kycStatus)) {

			// DO NOT FAIL — just flag
			txn.setProcessingStatus(ProcessingStatus.FLAGGED);
			txn.setErrorMessage(type + " CUSTOMER KYC NOT VERIFIED: " + customerId);
		}
	}
}
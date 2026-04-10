package com.iispl.dao;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CustomerDAOImpl JDBC Implementation of CustomerDAO Interface Uses HikariCP
 * Connection Pool via DatabaseConfig
 *
 * Table : customer Columns: id BIGSERIAL PRIMARY KEY customer_id VARCHAR(20)
 * UNIQUE full_name VARCHAR(100) kyc_status VARCHAR(20) customer_status
 * VARCHAR(20) created_at TIMESTAMP updated_at TIMESTAMP created_by VARCHAR(50)
 * version INT
 */
public class CustomerDAOImpl implements CustomerDAO {

	// =========================================================
	// SQL CONSTANTS
	// =========================================================

	private static final String SQL_INSERT = "INSERT INTO customer "
			+ "(customer_id, full_name, kyc_status, customer_status, "
			+ " created_at, updated_at, created_by, version) " + "VALUES (?, ?, ?, ?, NOW(), NOW(), ?, 0)";

	private static final String SQL_SELECT_ALL = "SELECT id, customer_id, full_name, kyc_status, "
			+ "customer_status, created_at, updated_at, created_by, version " + "FROM customer " + "ORDER BY id";

	private static final String SQL_SELECT_ALL_ACTIVE = "SELECT id, customer_id, full_name, kyc_status, "
			+ "customer_status, created_at, updated_at, created_by, version " + "FROM customer "
			+ "WHERE UPPER(customer_status) = 'ACTIVE' " + "ORDER BY id";

	private static final String SQL_SELECT_ALL_VERIFIED = "SELECT id, customer_id, full_name, kyc_status, "
			+ "customer_status, created_at, updated_at, created_by, version " + "FROM customer "
			+ "WHERE UPPER(kyc_status) = 'VERIFIED' " + "ORDER BY id";

	private static final String SQL_SELECT_BY_ID = "SELECT id, customer_id, full_name, kyc_status, "
			+ "customer_status, created_at, updated_at, created_by, version " + "FROM customer " + "WHERE id = ?";

	private static final String SQL_SELECT_BY_CUSTOMER_ID = "SELECT id, customer_id, full_name, kyc_status, "
			+ "customer_status, created_at, updated_at, created_by, version " + "FROM customer "
			+ "WHERE customer_id = ?";

	private static final String SQL_SELECT_BY_FULL_NAME = "SELECT id, customer_id, full_name, kyc_status, "
			+ "customer_status, created_at, updated_at, created_by, version " + "FROM customer "
			+ "WHERE LOWER(full_name) LIKE LOWER(?) " + "ORDER BY full_name";

	private static final String SQL_UPDATE = "UPDATE customer SET " + "full_name        = ?, "
			+ "kyc_status       = ?, " + "customer_status  = ?, " + "updated_at       = NOW(), "
			+ "version          = version + 1 " + "WHERE customer_id = ?";

	private static final String SQL_DELETE = "DELETE FROM customer " + "WHERE customer_id = ?";

	// =========================================================
	// READ — GET ALL
	// =========================================================

	/**
	 * Retrieves ALL customers from the database.
	 *
	 * @return List of all Customer objects
	 */
	@Override
	public List<Customer> getAllCustomers() {

		System.out.println("==============================");
		System.out.println("  GET ALL CUSTOMERS           ");

		List<Customer> customers = new ArrayList<>();

		try (Connection connection = DatabaseConfig.getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(SQL_SELECT_ALL)) {

			while (rs.next()) {
				customers.add(mapRow(rs));
			}

			System.out.println(" Total Customers Fetched: " + customers.size());

		} catch (Exception e) {
			System.err.println(" Error Fetching All Customers: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to fetch all customers.", e);
		}

		return customers;
	}

	// =========================================================
	// READ — GET ALL ACTIVE CUSTOMERS
	// =========================================================

	/**
	 * Retrieves all Customers with customer_status = 'ACTIVE'.
	 *
	 * @return List of active Customer objects
	 */
	@Override
	public List<Customer> getAllActiveCustomers() {

		System.out.println("==============================");
		System.out.println("  GET ALL ACTIVE CUSTOMERS    ");

		List<Customer> customers = new ArrayList<>();

		try (Connection connection = DatabaseConfig.getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(SQL_SELECT_ALL_ACTIVE)) {

			while (rs.next()) {
				Customer customer = mapRow(rs);
				customers.add(customer);

				System.out.println(" Active -> " + customer.getCustomerId() + " | " + customer.getFullName()
						+ " | KYC: " + customer.getKycStatus());
			}

		} catch (Exception e) {
			System.err.println("Error Fetching Active Customers: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to fetch active customers.", e);
		}

		return customers;
	}

	// READ — GET ALL VERIFIED CUSTOMERS

	/**
	 * Retrieves all Customers with kyc_status = 'VERIFIED'.
	 *
	 * @return List of KYC-verified Customer objects
	 */
	@Override
	public List<Customer> getAllVerifiedCustomers() {

		System.out.println("==============================");
		System.out.println("  GET ALL VERIFIED CUSTOMERS  ");

		List<Customer> customers = new ArrayList<>();

		try (Connection connection = DatabaseConfig.getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(SQL_SELECT_ALL_VERIFIED)) {

			while (rs.next()) {
				Customer customer = mapRow(rs);
				customers.add(customer);

				System.out.println("   Verified -> " + customer.getCustomerId() + " | " + customer.getFullName()
						+ " | Status: " + customer.getCustomerStatus());
			}

		} catch (Exception e) {
			System.err.println(" Error Fetching Verified Customers: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to fetch verified customers.", e);
		}

		return customers;
	}

	// READ — GET BY CUSTOMER_ID
	/**
	 * Finds a Customer by their business Customer ID (e.g., CID1001).
	 *
	 * @param customerId business key e.g., "CID1001"
	 * @return Optional<Customer>
	 */
	@Override
	public Optional<Customer> getCustomerByCustomerId(String customerId) {

		System.out.println("========================================");
		System.out.println("  GET CUSTOMER BY CUSTOMER_ID          ");
		System.out.println("  Customer ID : " + customerId);

		try (Connection connection = DatabaseConfig.getConnection();
				PreparedStatement ps = connection.prepareStatement(SQL_SELECT_BY_CUSTOMER_ID)) {

			ps.setString(1, customerId);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Customer customer = mapRow(rs);
					System.out.println("Customer Found!");
					System.out.println("   Customer ID     : " + customer.getCustomerId());
					System.out.println("   Full Name       : " + customer.getFullName());
					System.out.println("   KYC Status      : " + customer.getKycStatus());
					System.out.println("   Customer Status : " + customer.getCustomerStatus());
					System.out.println("   Created By      : " + customer.getCreatedBy());
					return Optional.of(customer);
				}
			}

		} catch (Exception e) {
			System.err.println("Error Fetching Customer By Customer ID: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to fetch customer by customerId: " + customerId, e);
		}

		System.out.println(" No Customer Found with Customer ID: " + customerId);
		return Optional.empty();
	}

	// READ — GET BY FULL NAME

	/**
	 * Finds Customers whose full name contains the given keyword. Case-insensitive
	 * partial search.
	 *
	 * Examples: getCustomersByFullName("Rahul") -> finds "Rahul Sharma"
	 * getCustomersByFullName("sharma") -> finds anyone with "sharma"
	 *
	 * @param fullName search keyword (partial or full)
	 * @return List of matching Customer objects
	 */
	@Override
	public List<Customer> getCustomersByFullName(String fullName) {

		System.out.println("========================================");
		System.out.println("  GET CUSTOMERS BY FULL NAME           ");
		System.out.println("  Search : " + fullName);
		System.out.println("========================================");

		List<Customer> customers = new ArrayList<>();

		try (Connection connection = DatabaseConfig.getConnection();
				PreparedStatement ps = connection.prepareStatement(SQL_SELECT_BY_FULL_NAME)) {

			// Wrap with % for partial/contains match
			ps.setString(1, "%" + fullName.trim() + "%");

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Customer customer = mapRow(rs);
					customers.add(customer);

					System.out.println(" Found -> " + customer.getCustomerId() + " | " + customer.getFullName() + " | "
							+ customer.getKycStatus() + " | " + customer.getCustomerStatus());
				}
			}

		} catch (Exception e) {
			System.err.println(" Error Fetching Customers By Full Name: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to fetch customers by name: " + fullName, e);
		}

		return customers;
	}

	// HELPER — Map ResultSet Row -> Customer Object

	/**
	 * Maps a single ResultSet row to a Customer entity.
	 *
	 * @param rs ResultSet pointing to current row
	 * @return Customer object populated from DB row
	 * @throws java.sql.SQLException if column mapping fails
	 */
	private Customer mapRow(ResultSet rs) throws java.sql.SQLException {

		Customer customer = new Customer();

		customer.setCustomerId(rs.getString("customer_id"));
		customer.setFullName(rs.getString("full_name"));
		customer.setKycStatus(rs.getString("kyc_status"));
		customer.setCustomerStatus(rs.getString("customer_status"));
		customer.setCreatedBy(rs.getString("created_by"));

		return customer;
	}
}
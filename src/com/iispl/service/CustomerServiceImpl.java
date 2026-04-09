package com.iispl.service;

import com.iispl.dao.CustomerDAO;
import com.iispl.dao.CustomerDAOImpl;
import com.iispl.entity.Customer;

import java.util.List;
import java.util.Optional;

/**
 * CustomerServiceImpl Business Logic Implementation for Customer Operations.
 * Sits between Controller and DAO layer. All database work is delegated to
 * CustomerDAO.
 */
public class CustomerServiceImpl implements CustomerService {

	/*
	 * CustomerDAO reference. We use the interface type so we can swap
	 * implementations easily.
	 */
	private final CustomerDAO customerDao; 

	/*
	 * Default Constructor. Creates a real CustomerDAOImpl for actual database use.
	 */
	public CustomerServiceImpl() {
		this.customerDao = new CustomerDAOImpl();
	}

	/*
	 * Constructor with parameter. Used when we want to pass a custom or mock DAO
	 * for testing.
	 */
	public CustomerServiceImpl(CustomerDAO customerDAO) {
		this.customerDao = customerDAO;
	}

	// ===========================================================
	// READ - Get All Customers
	// ===========================================================

	/*
	 * Fetches all customers from the database. No filters. Returns the complete
	 * list. Delegates to DAO directly — no extra filtering in Java.
	 */
	@Override
	public List<Customer> getAllCustomers() {

		System.out.println("==============================");
		System.out.println("  SERVICE - GET ALL CUSTOMERS ");
		System.out.println("==============================");

		List<Customer> customers = customerDao.getAllCustomers();

		if (customers.isEmpty()) {
			System.out.println("No Customers found in the database.");
		} else {
			System.out.println("Total Customers Found: " + customers.size());
		}

		return customers;
	}

	// ===========================================================
	// READ - Get Active Customers
	// ===========================================================

	/*
	 * Fetches only ACTIVE customers. Delegates to DAO which uses SQL WHERE
	 * customer_status = 'ACTIVE'. More efficient than fetching all and filtering in
	 * Java.
	 */
	@Override
	public List<Customer> getActiveCustomers() {

		System.out.println("==============================");
		System.out.println("  SERVICE - GET ACTIVE CUSTOMERS");
		System.out.println("==============================");

		List<Customer> customers = customerDao.getAllActiveCustomers();

		if (customers.isEmpty()) {
			System.out.println("No Active Customers found.");
		} else {
			System.out.println("Total Active Customers: " + customers.size());
		}

		return customers;
	}

	// ===========================================================
	// READ - Get Verified Customers
	// ===========================================================

	/*
	 * Fetches only KYC VERIFIED customers. Delegates to DAO which uses SQL WHERE
	 * kyc_status = 'VERIFIED'. More efficient than fetching all and filtering in
	 * Java.
	 */
	@Override
	public List<Customer> getVerifiedCustomers() {

		System.out.println("==============================");
		System.out.println("  SERVICE - GET VERIFIED CUSTOMERS");
		System.out.println("==============================");

		List<Customer> customers = customerDao.getAllVerifiedCustomers();

		if (customers.isEmpty()) {
			System.out.println("No KYC Verified Customers found.");
		} else {
			System.out.println("Total KYC Verified Customers: " + customers.size());
		}

		return customers;
	}

	// ===========================================================
	// READ - Get Customer by Customer ID
	// ===========================================================

	/*
	 * Finds a customer using their business Customer ID (e.g., CID1001).
	 */
	@Override
	public Optional<Customer> getCustomerByCustomerId(String customerId) {

		System.out.println("==========================================");
		System.out.println("  SERVICE - GET CUSTOMER BY CUSTOMER ID  ");
		System.out.println("  Customer ID: " + customerId);
		System.out.println("==========================================");

		// Customer ID must not be blank
		if (customerId == null || customerId.trim().isEmpty()) {
			System.out.println("Validation Failed: Customer ID must not be null or blank.");
			return Optional.empty();
		}

		Optional<Customer> customer = customerDao.getCustomerByCustomerId(customerId.trim());

		return customer;
	}

	// ===========================================================
	// READ - Get Customers by Full Name
	// ===========================================================

	/*
	 * Searches customers by name. Case-insensitive partial match. Minimum 2
	 * characters required.
	 */
	@Override
	public List<Customer> getCustomersByFullName(String fullName) {

		System.out.println("==============================");
		System.out.println("  SERVICE - GET BY FULL NAME  ");
		System.out.println("  Search: " + fullName);
		System.out.println("==============================");

		// Name keyword must not be blank
		if (fullName == null || fullName.trim().isEmpty()) {
			System.out.println("Validation Failed: Full Name must not be null or blank.");
			return List.of();
		}

		// Minimum 2 characters needed for meaningful search
		if (fullName.trim().length() < 2) {
			System.out.println("Validation Failed: Please enter at least 2 characters to search.");
			return List.of();
		}

		List<Customer> customers = customerDao.getCustomersByFullName(fullName.trim());

		return customers;
	}

	// ===========================================================
	// PRIVATE HELPERS - Validation Methods

	/*
	 * Validates KYC Status value. Accepted values: VERIFIED, PENDING
	 */
	private boolean isValidKycStatus(String kycStatus) {
		return kycStatus != null && (kycStatus.equalsIgnoreCase("VERIFIED") || kycStatus.equalsIgnoreCase("PENDING"));
	}

	/*
	 * Validates Customer Status value. Accepted values: ACTIVE, INACTIVE
	 */
	private boolean isValidCustomerStatus(String customerStatus) {
		return customerStatus != null
				&& (customerStatus.equalsIgnoreCase("ACTIVE") || customerStatus.equalsIgnoreCase("INACTIVE"));
	}
}
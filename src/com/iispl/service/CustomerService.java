package com.iispl.service;

import com.iispl.entity.Customer;

import java.util.List;
import java.util.Optional;

/**
 * CustomerService Interface Defines all Business Logic Operations for Customer
 * Entity
 *
 * Operations: 
 * READ : getAllCustomers, getActiveCustomers, getVerifiedCustomers,
 * getCustomerById, getCustomerByCustomerId, getCustomersByFullName
 */
public interface CustomerService {

	/**
	 * Get all Customers
	 *
	 * @return List of all Customer objects
	 */
	List<Customer> getAllCustomers();

	/**
	 * Get all Active Customers (customerStatus = ACTIVE)
	 *
	 * @return List of active Customer objects
	 */
	List<Customer> getActiveCustomers();

	/**
	 * Get all KYC Verified Customers (kycStatus = VERIFIED)
	 *
	 * @return List of verified Customer objects
	 */
	List<Customer> getVerifiedCustomers();

	/**
	 * Get Customer by auto-generated primary key
	 *
	 * @param id bigserial primary key as String
	 * @return Optional<Customer>
	 */
//    Optional<Customer> getCustomerById(String id);

	/**
	 * Get Customer by business Customer ID (e.g., CID1001)
	 *
	 * @param customerId business key
	 * @return Optional<Customer>
	 */
	Optional<Customer> getCustomerByCustomerId(String customerId);

	/**
	 * Get Customers by Full Name Case-insensitive partial search
	 *
	 * @param fullName search keyword (min 2 characters)
	 * @return List of matching Customer objects
	 */
	List<Customer> getCustomersByFullName(String fullName);
}
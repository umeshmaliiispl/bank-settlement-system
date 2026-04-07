package com.iispl.entity;

import java.util.List;

/**
 * Customer Entity (Master Data)
 */
public class Customer extends BaseEntity {

	private String customerId; // CID
	private String fullName;

	private String kycStatus; // VERIFIED / PENDING
	private String customerStatus; // ACTIVE / INACTIVE

	// One-to-Many relationship
	private List<Account> accounts;

	public Customer() {
		super();
	}

	public Customer(String createdBy, String customerId, String fullName, String kycStatus, String customerStatus) {
		super(createdBy);
		this.customerId = customerId;
		this.fullName = fullName;
		this.kycStatus = kycStatus;
		this.customerStatus = customerStatus;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getKycStatus() {
		return kycStatus;
	}

	public void setKycStatus(String kycStatus) {
		this.kycStatus = kycStatus;
	}

	public String getCustomerStatus() {
		return customerStatus;
	}

	public void setCustomerStatus(String customerStatus) {
		this.customerStatus = customerStatus;
	}

	public List<Account> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
}
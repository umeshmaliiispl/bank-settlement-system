package com.iispl.entity;

import java.math.BigDecimal;

import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

/**
 * Account Entity (Used for lookup + settlement)
 */

public class Account extends BaseEntity {

	private String accountNumber; 
	private String ifscCode;
	private String bankName;

	private String customerId; // FK → Customer.customerId

	private AccountType accountType;
	private BigDecimal balance;
	private String currency;

	private AccountStatus accountStatus;

	public Account() {
		super();
	}

	public Account(String createdBy, String accountNumber, String ifscCode, String bankName, String customerId,
			AccountType accountType, BigDecimal balance, String currency, AccountStatus accountStatus) {

		super(createdBy);
		this.accountNumber = accountNumber;
		this.ifscCode = ifscCode;
		this.bankName = bankName;
		this.customerId = customerId;
		this.accountType = accountType;
		this.balance = balance;
		this.currency = currency;
		this.accountStatus = accountStatus;
	}

	public synchronized void credit(BigDecimal amount) {
		this.balance = this.balance.add(amount);
		markUpdated();
	}

	public synchronized void debit(BigDecimal amount) {
		if (this.balance.compareTo(amount) < 0) {
			throw new RuntimeException("Insufficient Balance");
		}
		this.balance = this.balance.subtract(amount);
		markUpdated();
	}


	public String getAccountNumber() {
		return accountNumber;
	}

	public String getCustomerId() {
		return customerId;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}
}
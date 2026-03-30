package com.iispl.entity;

import java.math.BigDecimal;

public class Account {

    private Long id;
    private String accountNumber;
    private String accountName;
    private BigDecimal balance;

    public Account(Long id, String accountNumber, String accountName, BigDecimal balance) {
		super();
		this.id = id;
		this.accountNumber = accountNumber;
		this.accountName = accountName;
		this.balance = balance;
	}

	public Account() {}

    public Long getId() { 
    	return id; 
    	}
    public void setId(Long id) {
    	this.id = id; 
    	}

    public String getAccountNumber() {
    	return accountNumber;
    	}
    public void setAccountNumber(String accountNumber) { 
    	this.accountNumber = accountNumber; 
    	}

    public String getAccountName() {
    	return accountName;
    	}
    public void setAccountName(String accountName) { 
    	this.accountName = accountName; 
    	}

    public BigDecimal getBalance() {
    	return balance; 
    	}
    public void setBalance(BigDecimal balance) {
    	this.balance = balance;
    	}
}
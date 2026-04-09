package com.iispl.entity;

import java.math.BigDecimal;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

/**
 * Account Entity
 *
 * Represents a bank account linked to a Customer.
 * Supports credit/debit operations with balance validation.
 * Extends BaseEntity → inherits id, audit fields, version control.
 */
public class Account extends BaseEntity {

    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String customerId;        // FK → Customer.customerId
    private AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private AccountStatus accountStatus;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public Account() {
        super();
    }

    public Account(String createdBy,
                   String accountNumber,
                   String ifscCode,
                   String bankName,
                   String customerId,
                   AccountType accountType,
                   BigDecimal balance,
                   String currency,
                   AccountStatus accountStatus) {

        super(createdBy);
        this.accountNumber = accountNumber;
        this.ifscCode      = ifscCode;
        this.bankName      = bankName;
        this.customerId    = customerId;
        this.accountType   = accountType;
        this.balance       = balance;
        this.currency      = currency;
        this.accountStatus = accountStatus;
    }

    // ── Business Methods ──────────────────────────────────────────────────────────

    /**
     * Credits the given amount to this account.
     * Thread-safe via synchronized.
     *
     * @param amount must be positive
     */
    public synchronized void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Credit amount must be positive. Received: " + amount
            );
        }
        this.balance = this.balance.add(amount);
        markUpdated();
    }

    /**
     * Debits the given amount from this account.
     * Thread-safe via synchronized.
     *
     * @param amount must be positive and <= current balance
     */
    public synchronized void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Debit amount must be positive. Received: " + amount
            );
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new RuntimeException(
                "Insufficient balance. Available: " + this.balance + ", Requested: " + amount
            );
        }
        this.balance = this.balance.subtract(amount);
        markUpdated();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    // ── toString ──────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return super.toString()
            + " | Account{"
            + "accountNumber='"  + accountNumber  + '\''
            + ", bankName='"     + bankName        + '\''
            + ", ifscCode='"     + ifscCode        + '\''
            + ", customerId='"   + customerId      + '\''
            + ", accountType="   + accountType
            + ", balance="       + balance
            + ", currency='"     + currency        + '\''
            + ", accountStatus=" + accountStatus
            + '}';
    }
}
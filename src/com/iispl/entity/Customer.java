package com.iispl.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Customer Entity (Master Data)
 *
 * Represents a bank customer with KYC and status information.
 * Maintains a one-to-many relationship with Account entities.
 * Extends BaseEntity → inherits id, audit fields, version control.
 */
public class Customer extends BaseEntity {

    private String customerId;       // Business key e.g., CID-001
    private String fullName;
    private String kycStatus;        // VERIFIED / PENDING / REJECTED
    private String customerStatus;   // ACTIVE / INACTIVE / SUSPENDED

    // One-to-Many: one customer → many accounts
    private List<Account> accounts;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public Customer() {
        super();
        this.accounts = new ArrayList<>();
    }

    public Customer(String createdBy,
                    String customerId,
                    String fullName,
                    String kycStatus,
                    String customerStatus) {

        super(createdBy);
        this.customerId     = customerId;
        this.fullName       = fullName;
        this.kycStatus      = kycStatus;
        this.customerStatus = customerStatus;
        this.accounts       = new ArrayList<>();
    }

    // ── Business Methods ──────────────────────────────────────────────────────────

    /**
     * Links an Account to this Customer.
     * Sets customerId on account for bidirectional consistency.
     *
     * @param account the Account to link — must not be null
     */
    public void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }
        account.setCustomerId(this.customerId);
        this.accounts.add(account);
        markUpdated();
    }

    /**
     * Removes an Account from this Customer by accountNumber.
     *
     * @param accountNumber the account to remove
     * @return true if removed, false if not found
     */
    public boolean removeAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return false;
        }
        boolean removed = accounts.removeIf(
            a -> accountNumber.equals(a.getAccountNumber())
        );
        if (removed) {
            markUpdated();
        }
        return removed;
    }

    /**
     * Checks if this Customer is active.
     *
     * @return true if customerStatus is ACTIVE
     */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.customerStatus);
    }

    /**
     * Checks if this Customer is KYC verified.
     *
     * @return true if kycStatus is VERIFIED
     */
    public boolean isKycVerified() {
        return "VERIFIED".equalsIgnoreCase(this.kycStatus);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

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

    /**
     * Returns an unmodifiable view of accounts.
     * Prevents external mutation of internal list.
     */
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts != null ? accounts : new ArrayList<>();
    }

    // ── toString ──────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return super.toString()
            + " | Customer{"
            + "customerId='"       + customerId     + '\''
            + ", fullName='"       + fullName        + '\''
            + ", kycStatus='"      + kycStatus       + '\''
            + ", customerStatus='" + customerStatus  + '\''
            + ", accountCount="    + accounts.size()
            + '}';
    }
}
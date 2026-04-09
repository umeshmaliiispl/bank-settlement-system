package com.iispl.service;

import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AccountService Interface
 * Defines all Business Logic Operations for Account Entity
 *
 * Operations:
 * ── WRITE  : createAccount, updateAccount, deleteAccount,
 *             credit, debit
 * ── READ   : getAllAccounts, getAccountById,
 *             getAccountByAccountNumber, getAccountsByBankName,
 *             getAccountsByAccountType, getAccountsByAccountStatus,
 *             getAccountsByCustomerId
 */
public interface AccountService {

    // ==================== WRITE ====================

    /**
     * Create a new Account
     * Business Rules:
     *   - Account Number must not already exist
     *   - Balance must not be negative
     *   - Customer ID must be present
     *
     * @param account Account object to create
     */
    void createAccount(Account account);

    /**
     * Update Account details
     * Business Rules:
     *   - Account must exist before updating
     *
     * @param account Account object with updated values
     */
    void updateAccount(Account account);

    /**
     * Delete Account by primary key ID
     * Business Rules:
     *   - Account must exist before deleting
     *
     * @param id primary key of Account
     */
    void deleteAccount(Long id);

    /**
     * Credit amount to Account balance
     * Business Rules:
     *   - Amount must be positive
     *   - Account must exist and be ACTIVE
     *
     * @param id     primary key of Account
     * @param amount amount to credit (must be positive)
     */
    void credit(Long id, BigDecimal amount);

    /**
     * Debit amount from Account balance
     * Business Rules:
     *   - Amount must be positive
     *   - Account must exist and be ACTIVE
     *   - Balance must be sufficient
     *
     * @param id     primary key of Account
     * @param amount amount to debit (must be positive)
     */
    void debit(Long id, BigDecimal amount);

    // ==================== READ ====================

    /**
     * Get all Accounts
     *
     * @return List of all Account objects
     */
    List<Account> getAllAccounts();

    /**
     * Get Account by primary key ID
     *
     * @param id primary key
     * @return Optional<Account>
     */
    Optional<Account> getAccountById(Long id);

    /**
     * Get Account by unique Account Number
     *
     * @param accountNumber unique account number
     * @return Optional<Account>
     */
    Optional<Account> getAccountByAccountNumber(String accountNumber);

    /**
     * Get Accounts by Bank Name
     * Case-insensitive partial search
     *
     * @param bankName bank name keyword
     * @return List of matching Account objects
     */
    List<Account> getAccountsByBankName(String bankName);

    /**
     * Get Accounts by Account Type
     *
     * @param accountType e.g., SAVINGS, CURRENT
     * @return List of matching Account objects
     */
    List<Account> getAccountsByAccountType(AccountType accountType);

    /**
     * Get Accounts by Account Status
     *
     * @param accountStatus e.g., ACTIVE, FROZEN
     * @return List of matching Account objects
     */
    List<Account> getAccountsByAccountStatus(AccountStatus accountStatus);

    /**
     * Get all Accounts linked to a specific Customer
     *
     * @param customerId e.g., CID1001
     * @return List of Account objects for that customer
     */
    List<Account> getAccountsByCustomerId(String customerId);
}
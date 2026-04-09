package com.iispl.service;

import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AccountService Interface Defines all Business Logic Operations for Account
 * Entity
 *
 * Operations: ── READ : getAllAccounts, getAccountById,
 * getAccountByAccountNumber, getAccountsByBankName, getAccountsByAccountType,
 * getAccountsByAccountStatus, getAccountsByCustomerId
 */
public interface AccountService {

	// ==================== READ ====================

	/**
	 * Get all Accounts
	 *
	 * @return List of all Account objects
	 */
	List<Account> getAllAccounts();

	/**
	 * Get Account by unique Account Number
	 *
	 * @param accountNumber unique account number
	 * @return Optional<Account>
	 */
	Optional<Account> getAccountByAccountNumber(String accountNumber);

	/**
	 * Get Accounts by Bank Name Case-insensitive partial search
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
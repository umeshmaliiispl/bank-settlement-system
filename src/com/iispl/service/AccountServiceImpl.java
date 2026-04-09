package com.iispl.service;

import com.iispl.dao.AccountDao;
import com.iispl.dao.AccountDaoImpl;
import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

import java.util.List;
import java.util.Optional;

/*
 * AccountServiceImpl
 * Business Logic Implementation for Account Operations.
 * Sits between Main and DAO layer.
 * All database work is delegated to AccountDao.
 * All print statements removed — Main class handles display.
 */
public class AccountServiceImpl implements AccountService {

    /*
     * AccountDao reference.
     * Interface type used so implementation can be swapped easily.
     */
    private final AccountDao accountDao;

    /*
     * Default Constructor.
     * Creates a real AccountDaoImpl for actual database use.
     */
    public AccountServiceImpl() {
        this.accountDao = new AccountDaoImpl();
    }

    /*
     * Constructor with DAO parameter.
     * Used when passing a custom or mock DAO for testing.
     */
    public AccountServiceImpl(AccountDao accountDao) {
        this.accountDao = accountDao;
    }


    // ===========================================================
    // READ - Get All Accounts
    // ===========================================================

    /*
     * Fetches all accounts from the database.
     * No filters applied. Returns complete list.
     * Delegates directly to DAO.
     *
     * @return List of all Account objects
     */
    @Override
    public List<Account> getAllAccounts() {
        return accountDao.findAll();
    }


    // ===========================================================
    // READ - Get Account by Account Number
    // ===========================================================

    /*
     * Finds an account using the unique account number.
     * Validates input before calling DAO.
     *
     * @param accountNumber unique account number
     * @return Optional<Account>
     */
    @Override
    public Optional<Account> getAccountByAccountNumber(String accountNumber) {

        /*
         * Account Number must not be null or blank.
         * Throws IllegalArgumentException if invalid.
         */
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Account Number must not be null or blank."
            );
        }

        return accountDao.findByAccountNumber(accountNumber.trim());
    }


    // ===========================================================
    // READ - Get Accounts by Bank Name
    // ===========================================================

    /*
     * Searches accounts by bank name.
     * Case-insensitive partial match.
     * Minimum 2 characters required for search.
     *
     * @param bankName bank name keyword to search
     * @return List of matching Account objects
     */
    @Override
    public List<Account> getAccountsByBankName(String bankName) {

        /*
         * Bank Name must not be null or blank.
         */
        if (bankName == null || bankName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Bank Name must not be null or blank."
            );
        }

        /*
         * At least 2 characters needed for meaningful search.
         */
        if (bankName.trim().length() < 2) {
            throw new IllegalArgumentException(
                "Please enter at least 2 characters to search."
            );
        }

        return accountDao.findByBankName(bankName.trim());
    }


    // ===========================================================
    // READ - Get Accounts by Account Type
    // ===========================================================

    /*
     * Finds all accounts of a specific AccountType.
     * Example: SAVINGS, CURRENT, FIXED_DEPOSIT, FOREX
     *
     * @param accountType the AccountType enum value
     * @return List of matching Account objects
     */
    @Override
    public List<Account> getAccountsByAccountType(AccountType accountType) {

        /*
         * Account Type must not be null.
         */
        if (accountType == null) {
            throw new IllegalArgumentException(
                "Account Type must not be null."
            );
        }

        return accountDao.findByAccountType(accountType);
    }


    // ===========================================================
    // READ - Get Accounts by Account Status
    // ===========================================================

    /*
     * Finds all accounts with a specific AccountStatus.
     * Example: ACTIVE, INACTIVE, FROZEN, CLOSED, BLOCKED
     *
     * @param accountStatus the AccountStatus enum value
     * @return List of matching Account objects
     */
    @Override
    public List<Account> getAccountsByAccountStatus(AccountStatus accountStatus) {

        /*
         * Account Status must not be null.
         */
        if (accountStatus == null) {
            throw new IllegalArgumentException(
                "Account Status must not be null."
            );
        }

        return accountDao.findByAccountStatus(accountStatus);
    }


    // ===========================================================
    // READ - Get Accounts by Customer ID
    // ===========================================================

    /*
     * Finds all accounts linked to a specific customer.
     * One customer can have multiple accounts.
     *
     * @param customerId business key e.g. CID1001
     * @return List of Account objects for that customer
     */
    @Override
    public List<Account> getAccountsByCustomerId(String customerId) {

        /*
         * Customer ID must not be null or blank.
         */
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Customer ID must not be null or blank."
            );
        }

        return accountDao.findByCustomerId(customerId.trim());
    }
}
package com.iispl.service;

import com.iispl.dao.AccountDao;
import com.iispl.dao.AccountDaoImpl;
import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AccountServiceImpl
 * Business Logic Implementation for Account Operations.
 * Sits between Controller and DAO layer.
 * All database work is delegated to AccountDao.
 */
public class AccountServiceImpl implements AccountService {

    /*
     * AccountDao reference.
     * We use the interface type so we can swap implementations easily.
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
     * Constructor with parameter.
     * Used when we want to pass a custom or mock DAO for testing.
     */
    public AccountServiceImpl(AccountDao accountDao) {
        this.accountDao = accountDao;
    }


    // ===========================================================
    // WRITE - Create Account
    // ===========================================================

    /*
     * Registers a new Account into the system.
     *
     * Checks before saving:
     * 1. Account object must not be null
     * 2. Account Number must not be null or blank
     * 3. Customer ID must not be null or blank
     * 4. Balance must not be null or negative
     * 5. Account Type must not be null
     * 6. Account Status must not be null
     * 7. Account Number must not already exist
     */
    @Override
    public void createAccount(Account account) {

        System.out.println("==============================");
        System.out.println("  SERVICE - CREATE ACCOUNT    ");
        System.out.println("==============================");

        // Check 1: Account object must not be null
        if (account == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account object must not be null."
            );
        }

        // Check 2: Account Number must not be blank
        if (account.getAccountNumber() == null
                || account.getAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Validation Failed: Account Number must not be null or blank."
            );
        }

        // Check 3: Customer ID must not be blank
        if (account.getCustomerId() == null
                || account.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Validation Failed: Customer ID must not be null or blank."
            );
        }

        // Check 4: Balance must not be null or negative
        if (account.getBalance() == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Balance must not be null."
            );
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "Validation Failed: Balance must not be negative. Given: "
                    + account.getBalance()
            );
        }

        // Check 5: Account Type must not be null
        if (account.getAccountType() == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account Type must not be null."
            );
        }

        // Check 6: Account Status must not be null
        if (account.getAccountStatus() == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account Status must not be null."
            );
        }

        // Check 7: Account Number must not already exist
        Optional<Account> existing =
            accountDao.findByAccountNumber(account.getAccountNumber().trim());

        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                "Validation Failed: Account Number already exists - "
                    + account.getAccountNumber()
            );
        }

        // All checks passed. Clean up data before saving.
        account.setAccountNumber(account.getAccountNumber().trim());
        account.setCustomerId(account.getCustomerId().trim());

        System.out.println("Account Details Ready for Creation:");
        System.out.println("   Account Number : " + account.getAccountNumber());
        System.out.println("   Customer ID    : " + account.getCustomerId());
        System.out.println("   Bank Name      : " + account.getBankName());
        System.out.println("   Account Type   : " + account.getAccountType());
        System.out.println("   Balance        : " + account.getBalance());
        System.out.println("   Status         : " + account.getAccountStatus());

        // Delegate to DAO
        accountDao.save(account);

        System.out.println("✅ Account Created Successfully: " + account.getAccountNumber());
    }


    // ===========================================================
    // WRITE - Update Account
    // ===========================================================

    /*
     * Updates an existing Account record.
     *
     * Checks before updating:
     * 1. Account object must not be null
     * 2. Account ID must be present (needed for WHERE clause)
     * 3. Account must exist in the database
     */
    @Override
    public void updateAccount(Account account) {

        System.out.println("==============================");
        System.out.println("  SERVICE - UPDATE ACCOUNT    ");
        System.out.println("==============================");

        // Check 1: Account object must not be null
        if (account == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account object must not be null."
            );
        }

        // Check 2: Account ID must be present
        if (account.getId() == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account ID is required for update."
            );
        }

        // Check 3: Account must exist in database
        Optional<Account> existing = accountDao.findById(account.getId());
        if (existing.isEmpty()) {
            throw new RuntimeException(
                "Cannot Update: Account not found with ID - " + account.getId()
            );
        }

        System.out.println("Updating Account ID: " + account.getId());

        // Delegate to DAO
        accountDao.update(account);

        System.out.println("✅ Account Updated Successfully: " + account.getId());
    }


    // ===========================================================
    // WRITE - Delete Account
    // ===========================================================

    /*
     * Permanently removes an Account from the database.
     *
     * Checks before deleting:
     * 1. ID must not be null
     * 2. Account must exist in the database
     */
    @Override
    public void deleteAccount(Long id) {

        System.out.println("==============================");
        System.out.println("  SERVICE - DELETE ACCOUNT    ");
        System.out.println("  Account ID: " + id);
        System.out.println("==============================");

        // Check 1: ID must not be null
        if (id == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account ID must not be null."
            );
        }

        // Check 2: Account must exist
        Optional<Account> existing = accountDao.findById(id);
        if (existing.isEmpty()) {
            throw new RuntimeException(
                "Cannot Delete: Account not found with ID - " + id
            );
        }

        // Delegate to DAO
        accountDao.delete(id);

        System.out.println("✅ Account Deleted Successfully: " + id);
    }


    // ===========================================================
    // WRITE - Credit Amount
    // ===========================================================

    /*
     * Credits an amount to the account balance.
     *
     * Checks before crediting:
     * 1. ID must not be null
     * 2. Amount must be positive
     * 3. Account must exist
     * 4. Account must be ACTIVE
     */
    @Override
    public void credit(Long id, BigDecimal amount) {

        System.out.println("==============================");
        System.out.println("  SERVICE - CREDIT ACCOUNT    ");
        System.out.println("  Account ID : " + id);
        System.out.println("  Amount     : " + amount);
        System.out.println("==============================");

        // Check 1: ID must not be null
        if (id == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account ID must not be null."
            );
        }

        // Check 2: Amount must be positive
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Validation Failed: Credit amount must be positive. Given: " + amount
            );
        }

        // Check 3: Account must exist
        Account account = accountDao.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Account not found with ID: " + id
            ));

        // Check 4: Account must be ACTIVE
        if (!AccountStatus.ACTIVE.equals(account.getAccountStatus())) {
            throw new RuntimeException(
                "Cannot Credit: Account is not ACTIVE. "
                    + "Current Status: " + account.getAccountStatus()
                    + " | Account: " + account.getAccountNumber()
            );
        }

        // Calculate new balance
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);

        System.out.println("   Old Balance : " + oldBalance);
        System.out.println("   Credit      : +" + amount);
        System.out.println("   New Balance : " + newBalance);

        // Delegate to DAO
        accountDao.updateBalance(id, newBalance);

        System.out.println("✅ Credit Successful for Account: "
            + account.getAccountNumber());
    }


    // ===========================================================
    // WRITE - Debit Amount
    // ===========================================================

    /*
     * Debits an amount from the account balance.
     *
     * Checks before debiting:
     * 1. ID must not be null
     * 2. Amount must be positive
     * 3. Account must exist
     * 4. Account must be ACTIVE
     * 5. Balance must be sufficient
     */
    @Override
    public void debit(Long id, BigDecimal amount) {

        System.out.println("==============================");
        System.out.println("  SERVICE - DEBIT ACCOUNT     ");
        System.out.println("  Account ID : " + id);
        System.out.println("  Amount     : " + amount);
        System.out.println("==============================");

        // Check 1: ID must not be null
        if (id == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account ID must not be null."
            );
        }

        // Check 2: Amount must be positive
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Validation Failed: Debit amount must be positive. Given: " + amount
            );
        }

        // Check 3: Account must exist
        Account account = accountDao.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Account not found with ID: " + id
            ));

        // Check 4: Account must be ACTIVE
        if (!AccountStatus.ACTIVE.equals(account.getAccountStatus())) {
            throw new RuntimeException(
                "Cannot Debit: Account is not ACTIVE. "
                    + "Current Status: " + account.getAccountStatus()
                    + " | Account: " + account.getAccountNumber()
            );
        }

        // Check 5: Balance must be sufficient
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                "Insufficient Balance. "
                    + "Available: " + account.getBalance()
                    + " | Requested: " + amount
                    + " | Account: " + account.getAccountNumber()
            );
        }

        // Calculate new balance
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.subtract(amount);

        System.out.println("   Old Balance : " + oldBalance);
        System.out.println("   Debit       : -" + amount);
        System.out.println("   New Balance : " + newBalance);

        // Delegate to DAO
        accountDao.updateBalance(id, newBalance);

        System.out.println("✅ Debit Successful for Account: "
            + account.getAccountNumber());
    }


    // ===========================================================
    // READ - Get All Accounts
    // ===========================================================

    /*
     * Fetches all accounts from the database.
     * No filters. Returns the complete list.
     */
    @Override
    public List<Account> getAllAccounts() {

        System.out.println("==============================");
        System.out.println("  SERVICE - GET ALL ACCOUNTS  ");
        System.out.println("==============================");

        List<Account> accounts = accountDao.findAll();

        if (accounts.isEmpty()) {
            System.out.println("⚠️  No Accounts found in the database.");
        } else {
            System.out.println("✅ Total Accounts Found: " + accounts.size());
        }

        return accounts;
    }


    // ===========================================================
    // READ - Get Account by ID
    // ===========================================================

    /*
     * Finds an account using the auto-generated database primary key.
     */
    @Override
    public Optional<Account> getAccountById(Long id) {

        System.out.println("==============================");
        System.out.println("  SERVICE - GET ACCOUNT BY ID ");
        System.out.println("  Account ID: " + id);
        System.out.println("==============================");

        // ID must not be null
        if (id == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account ID must not be null."
            );
        }

        Optional<Account> account = accountDao.findById(id);

        if (account.isPresent()) {
            System.out.println("✅ Account Found: " + account.get().getAccountNumber());
        } else {
            System.out.println("⚠️  No Account Found with ID: " + id);
        }

        return account;
    }


    // ===========================================================
    // READ - Get Account by Account Number
    // ===========================================================

    /*
     * Finds an account using the unique account number.
     */
    @Override
    public Optional<Account> getAccountByAccountNumber(String accountNumber) {

        System.out.println("==========================================");
        System.out.println("  SERVICE - GET ACCOUNT BY ACCOUNT NUMBER");
        System.out.println("  Account Number: " + accountNumber);
        System.out.println("==========================================");

        // Account Number must not be blank
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Validation Failed: Account Number must not be null or blank."
            );
        }

        Optional<Account> account =
            accountDao.findByAccountNumber(accountNumber.trim());

        if (account.isPresent()) {
            System.out.println("✅ Account Found: " + account.get().getAccountNumber());
            System.out.println("   Bank Name    : " + account.get().getBankName());
            System.out.println("   Type         : " + account.get().getAccountType());
            System.out.println("   Balance      : " + account.get().getBalance());
            System.out.println("   Status       : " + account.get().getAccountStatus());
        } else {
            System.out.println("⚠️  No Account Found with Account Number: " + accountNumber);
        }

        return account;
    }


    // ===========================================================
    // READ - Get Accounts by Bank Name
    // ===========================================================

    /*
     * Searches accounts by bank name.
     * Case-insensitive partial match.
     * Minimum 2 characters required for search.
     */
    @Override
    public List<Account> getAccountsByBankName(String bankName) {

        System.out.println("==============================");
        System.out.println("  SERVICE - GET BY BANK NAME  ");
        System.out.println("  Search: " + bankName);
        System.out.println("==============================");

        // Bank Name must not be blank
        if (bankName == null || bankName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Validation Failed: Bank Name must not be null or blank."
            );
        }

        // At least 2 characters needed for meaningful search
        if (bankName.trim().length() < 2) {
            throw new IllegalArgumentException(
                "Validation Failed: Please enter at least 2 characters to search."
            );
        }

        List<Account> accounts = accountDao.findByBankName(bankName.trim());

        if (accounts.isEmpty()) {
            System.out.println("⚠️  No Accounts Found for Bank: " + bankName);
        } else {
            System.out.println("✅ Total Accounts Found: " + accounts.size());
        }

        return accounts;
    }


    // ===========================================================
    // READ - Get Accounts by Account Type
    // ===========================================================

    /*
     * Finds all accounts of a specific type.
     * Example: SAVINGS, CURRENT, FIXED_DEPOSIT
     */
    @Override
    public List<Account> getAccountsByAccountType(AccountType accountType) {

        System.out.println("==============================");
        System.out.println("  SERVICE - GET BY ACCT TYPE  ");
        System.out.println("  Type: " + accountType);
        System.out.println("==============================");

        // Account Type must not be null
        if (accountType == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account Type must not be null."
            );
        }

        List<Account> accounts = accountDao.findByAccountType(accountType);

        if (accounts.isEmpty()) {
            System.out.println("⚠️  No Accounts Found for Type: " + accountType);
        } else {
            System.out.println("✅ Total Accounts Found: " + accounts.size());
        }

        return accounts;
    }


    // ===========================================================
    // READ - Get Accounts by Account Status
    // ===========================================================

    /*
     * Finds all accounts with a specific status.
     * Example: ACTIVE, INACTIVE, FROZEN, CLOSED
     */
    @Override
    public List<Account> getAccountsByAccountStatus(AccountStatus accountStatus) {

        System.out.println("==============================");
        System.out.println("  SERVICE - GET BY ACCT STATUS");
        System.out.println("  Status: " + accountStatus);
        System.out.println("==============================");

        // Account Status must not be null
        if (accountStatus == null) {
            throw new IllegalArgumentException(
                "Validation Failed: Account Status must not be null."
            );
        }

        List<Account> accounts = accountDao.findByAccountStatus(accountStatus);

        if (accounts.isEmpty()) {
            System.out.println("⚠️  No Accounts Found with Status: " + accountStatus);
        } else {
            System.out.println("✅ Total Accounts Found: " + accounts.size());
        }

        return accounts;
    }


    // ===========================================================
    // READ - Get Accounts by Customer ID
    // ===========================================================

    /*
     * Finds all accounts linked to a specific customer.
     * Example: getAccountsByCustomerId("CID1001")
     */
    @Override
    public List<Account> getAccountsByCustomerId(String customerId) {

        System.out.println("==============================");
        System.out.println("  SERVICE - GET BY CUSTOMER ID");
        System.out.println("  Customer ID: " + customerId);
        System.out.println("==============================");

        // Customer ID must not be blank
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Validation Failed: Customer ID must not be null or blank."
            );
        }

        List<Account> accounts =
            accountDao.findByCustomerId(customerId.trim());

        if (accounts.isEmpty()) {
            System.out.println("⚠️  No Accounts Found for Customer ID: " + customerId);
        } else {
            System.out.println("✅ Total Accounts Found for Customer ["
                + customerId + "]: " + accounts.size());
        }

        return accounts;
    }
}
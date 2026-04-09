package com.iispl.dao;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AccountDaoImpl
 * JDBC Implementation of AccountDao Interface
 * Uses HikariCP Connection Pool via DatabaseConfig
 *
 * Table  : account
 * Columns:
 *   id               BIGSERIAL        PRIMARY KEY
 *   account_number   VARCHAR(20)      UNIQUE
 *   ifsc_code        VARCHAR(20)
 *   bank_name        VARCHAR(100)
 *   customer_id      VARCHAR(20)      FK → customer.customer_id
 *   account_type     VARCHAR(30)
 *   balance          NUMERIC(15,2)
 *   currency         VARCHAR(10)
 *   account_status   VARCHAR(20)
 *   created_at       TIMESTAMP
 *   updated_at       TIMESTAMP
 *   created_by       VARCHAR(50)
 *   version          INT
 */
public class AccountDaoImpl implements AccountDao {

    // =========================================================
    //  SQL CONSTANTS
    // =========================================================

    private static final String SQL_INSERT =
        "INSERT INTO account " +
        "(account_number, ifsc_code, bank_name, customer_id, " +
        " account_type, balance, currency, account_status, " +
        " created_at, updated_at, created_by, version) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, 0)";

    private static final String SQL_SELECT_ALL =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account " +
        "ORDER BY id";

    private static final String SQL_SELECT_BY_ID =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account " +
        "WHERE id = ?";

    private static final String SQL_SELECT_BY_ACCOUNT_NUMBER =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account " +
        "WHERE account_number = ?";

    private static final String SQL_SELECT_BY_BANK_NAME =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account " +
        "WHERE LOWER(bank_name) LIKE LOWER(?) " +
        "ORDER BY bank_name";

    private static final String SQL_SELECT_BY_ACCOUNT_TYPE =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account " +
        "WHERE account_type = ? " +
        "ORDER BY id";

    private static final String SQL_SELECT_BY_ACCOUNT_STATUS =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account " +
        "WHERE account_status = ? " +
        "ORDER BY id";

    private static final String SQL_SELECT_BY_CUSTOMER_ID =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account " +
        "WHERE customer_id = ? " +
        "ORDER BY id";

    private static final String SQL_UPDATE =
        "UPDATE account SET " +
        "ifsc_code      = ?, " +
        "bank_name      = ?, " +
        "account_type   = ?, " +
        "balance        = ?, " +
        "currency       = ?, " +
        "account_status = ?, " +
        "updated_at     = NOW(), " +
        "version        = version + 1 " +
        "WHERE id = ?";

    private static final String SQL_UPDATE_BALANCE =
        "UPDATE account SET " +
        "balance    = ?, " +
        "updated_at = NOW(), " +
        "version    = version + 1 " +
        "WHERE id = ?";

    private static final String SQL_DELETE =
        "DELETE FROM account WHERE id = ?";


    // =========================================================
    //  WRITE — SAVE
    // =========================================================

    /**
     * Inserts a new Account into the database.
     *
     * @param account Account object to insert
     */
    @Override
    public void save(Account account) {

        System.out.println("==============================");
        System.out.println("  INSERT ACCOUNT              ");
        System.out.println("==============================");

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_INSERT)) {

            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getIfscCode());
            ps.setString(3, account.getBankName());
            ps.setString(4, account.getCustomerId());
            ps.setString(5, account.getAccountType().name());
            ps.setBigDecimal(6, account.getBalance());
            ps.setString(7, account.getCurrency());
            ps.setString(8, account.getAccountStatus().name());
            ps.setString(9, account.getCreatedBy());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Account Inserted Successfully!");
                System.out.println("   Account Number : " + account.getAccountNumber());
                System.out.println("   Customer ID    : " + account.getCustomerId());
                System.out.println("   Account Type   : " + account.getAccountType());
                System.out.println("   Balance        : " + account.getBalance());
            }

        } catch (Exception e) {
            System.err.println("❌ Error Inserting Account: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to insert account: " + account.getAccountNumber(), e);
        }
    }


    // =========================================================
    //  READ — FIND ALL
    // =========================================================

    /**
     * Retrieves ALL accounts from the database.
     *
     * @return List of all Account objects
     */
    @Override
    public List<Account> findAll() {

        System.out.println("==============================");
        System.out.println("  GET ALL ACCOUNTS            ");
        System.out.println("==============================");

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SQL_SELECT_ALL)) {

            while (rs.next()) {
                accountList.add(mapRow(rs));
            }

            System.out.println("✅ Total Accounts Fetched: " + accountList.size());

        } catch (Exception e) {
            System.err.println("❌ Error Fetching All Accounts: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch all accounts.", e);
        }

        return accountList;
    }


    // =========================================================
    //  READ — FIND BY ID
    // =========================================================

    /**
     * Finds an Account by its auto-generated primary key.
     *
     * @param accountId bigserial primary key
     * @return Optional<Account>
     */
    @Override
    public Optional<Account> findById(Long accountId) {

        System.out.println("==============================");
        System.out.println("  GET ACCOUNT BY ID: " + accountId);
        System.out.println("==============================");

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_SELECT_BY_ID)) {

            ps.setLong(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Account account = mapRow(rs);
                    System.out.println("✅ Account Found: " + account.getAccountNumber());
                    return Optional.of(account);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error Fetching Account By ID: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch account by id: " + accountId, e);
        }

        System.out.println("⚠️  No Account Found with ID: " + accountId);
        return Optional.empty();
    }


    // =========================================================
    //  READ — FIND BY ACCOUNT NUMBER
    // =========================================================

    /**
     * Finds an Account by its unique account number.
     *
     * @param accountNumber the unique account number
     * @return Optional<Account>
     */
    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {

        System.out.println("========================================");
        System.out.println("  GET ACCOUNT BY ACCOUNT NUMBER        ");
        System.out.println("  Account Number : " + accountNumber);
        System.out.println("========================================");

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_SELECT_BY_ACCOUNT_NUMBER)) {

            ps.setString(1, accountNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Account account = mapRow(rs);
                    System.out.println("✅ Account Found!");
                    System.out.println("   Account Number  : " + account.getAccountNumber());
                    System.out.println("   Bank Name       : " + account.getBankName());
                    System.out.println("   Account Type    : " + account.getAccountType());
                    System.out.println("   Balance         : " + account.getBalance());
                    System.out.println("   Account Status  : " + account.getAccountStatus());
                    return Optional.of(account);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error Fetching Account By Account Number: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch account by number: " + accountNumber, e);
        }

        System.out.println("⚠️  No Account Found with Account Number: " + accountNumber);
        return Optional.empty();
    }


    // =========================================================
    //  READ — FIND BY BANK NAME
    // =========================================================

    /**
     * Finds all Accounts belonging to a specific bank.
     * Case-insensitive partial match.
     *
     * Examples:
     *   findByBankName("SBI")   → finds all SBI accounts
     *   findByBankName("bank")  → finds all accounts with "bank" in name
     *
     * @param bankName bank name keyword to search
     * @return List of matching Account objects
     */
    @Override
    public List<Account> findByBankName(String bankName) {

        System.out.println("========================================");
        System.out.println("  GET ACCOUNTS BY BANK NAME            ");
        System.out.println("  Search : " + bankName);
        System.out.println("========================================");

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_SELECT_BY_BANK_NAME)) {

            // Wrap with % for partial/contains search
            ps.setString(1, "%" + bankName.trim() + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account account = mapRow(rs);
                    accountList.add(account);

                    System.out.println("   ✅ Found → "
                        + account.getAccountNumber()
                        + " | " + account.getBankName()
                        + " | " + account.getAccountType()
                        + " | " + account.getAccountStatus());
                }
            }

            System.out.println("✅ Total Accounts Found: " + accountList.size());

        } catch (Exception e) {
            System.err.println("❌ Error Fetching Accounts By Bank Name: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch accounts by bank name: " + bankName, e);
        }

        return accountList;
    }


    // =========================================================
    //  READ — FIND BY ACCOUNT TYPE
    // =========================================================

    /**
     * Finds all Accounts of a specific AccountType.
     *
     * Examples:
     *   findByAccountType(AccountType.SAVINGS)  → all savings accounts
     *   findByAccountType(AccountType.CURRENT)  → all current accounts
     *
     * @param accountType the AccountType enum value
     * @return List of matching Account objects
     */
    @Override
    public List<Account> findByAccountType(AccountType accountType) {

        System.out.println("========================================");
        System.out.println("  GET ACCOUNTS BY ACCOUNT TYPE         ");
        System.out.println("  Type : " + accountType);
        System.out.println("========================================");

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_SELECT_BY_ACCOUNT_TYPE)) {

            // Store enum as String in DB
            ps.setString(1, accountType.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account account = mapRow(rs);
                    accountList.add(account);

                    System.out.println("   ✅ Found → "
                        + account.getAccountNumber()
                        + " | " + account.getBankName()
                        + " | " + account.getAccountType()
                        + " | Balance: " + account.getBalance());
                }
            }

            System.out.println("✅ Total Accounts Found: " + accountList.size());

        } catch (Exception e) {
            System.err.println("❌ Error Fetching Accounts By Account Type: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch accounts by type: " + accountType, e);
        }

        return accountList;
    }


    // =========================================================
    //  READ — FIND BY ACCOUNT STATUS
    // =========================================================

    /**
     * Finds all Accounts with a specific AccountStatus.
     *
     * Examples:
     *   findByAccountStatus(AccountStatus.ACTIVE)  → all active accounts
     *   findByAccountStatus(AccountStatus.FROZEN)  → all frozen accounts
     *
     * @param accountStatus the AccountStatus enum value
     * @return List of matching Account objects
     */
    @Override
    public List<Account> findByAccountStatus(AccountStatus accountStatus) {

        System.out.println("========================================");
        System.out.println("  GET ACCOUNTS BY ACCOUNT STATUS       ");
        System.out.println("  Status : " + accountStatus);
        System.out.println("========================================");

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_SELECT_BY_ACCOUNT_STATUS)) {

            // Store enum as String in DB
            ps.setString(1, accountStatus.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account account = mapRow(rs);
                    accountList.add(account);

                    System.out.println("   ✅ Found → "
                        + account.getAccountNumber()
                        + " | " + account.getBankName()
                        + " | " + account.getAccountType()
                        + " | Status: " + account.getAccountStatus());
                }
            }

            System.out.println("✅ Total Accounts Found: " + accountList.size());

        } catch (Exception e) {
            System.err.println("❌ Error Fetching Accounts By Status: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch accounts by status: " + accountStatus, e);
        }

        return accountList;
    }


    // =========================================================
    //  READ — FIND BY CUSTOMER ID
    // =========================================================

    /**
     * Finds all Accounts linked to a specific Customer.
     *
     * Example:
     *   findByCustomerId("CID1001") → all accounts for customer CID1001
     *
     * @param customerId the customer's business key
     * @return List of Account objects belonging to that customer
     */
    @Override
    public List<Account> findByCustomerId(String customerId) {

        System.out.println("========================================");
        System.out.println("  GET ACCOUNTS BY CUSTOMER ID          ");
        System.out.println("  Customer ID : " + customerId);
        System.out.println("========================================");

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_SELECT_BY_CUSTOMER_ID)) {

            ps.setString(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Account account = mapRow(rs);
                    accountList.add(account);

                    System.out.println("   ✅ Found → "
                        + account.getAccountNumber()
                        + " | " + account.getBankName()
                        + " | " + account.getAccountType()
                        + " | " + account.getAccountStatus());
                }
            }

            System.out.println("✅ Total Accounts for Customer [" + customerId + "]: "
                + accountList.size());

        } catch (Exception e) {
            System.err.println("❌ Error Fetching Accounts By Customer ID: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch accounts for customer: " + customerId, e);
        }

        return accountList;
    }


    // =========================================================
    //  WRITE — UPDATE FULL
    // =========================================================

    /**
     * Updates all updatable fields of an Account.
     *
     * @param account Account object with updated values
     */
    @Override
    public void update(Account account) {

        System.out.println("==============================");
        System.out.println("  UPDATE ACCOUNT              ");
        System.out.println("  Account ID: " + account.getId());
        System.out.println("==============================");

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, account.getIfscCode());
            ps.setString(2, account.getBankName());
            ps.setString(3, account.getAccountType().name());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getCurrency());
            ps.setString(6, account.getAccountStatus().name());
            ps.setLong(7, account.getId());    // WHERE id = ?

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Account Updated Successfully: "
                    + account.getAccountNumber());
            } else {
                System.out.println("⚠️  No Account Found to Update with ID: "
                    + account.getId());
            }

        } catch (Exception e) {
            System.err.println("❌ Error Updating Account: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update account: " + account.getId(), e);
        }
    }


    // =========================================================
    //  WRITE — UPDATE BALANCE ONLY
    // =========================================================

    /**
     * Updates ONLY the balance of an Account.
     * Also bumps updated_at and version.
     *
     * @param accountId primary key of the account
     * @param balance   new balance value
     */
    @Override
    public void updateBalance(Long accountId, BigDecimal balance) {

        System.out.println("==============================");
        System.out.println("  UPDATE ACCOUNT BALANCE      ");
        System.out.println("  Account ID : " + accountId);
        System.out.println("  New Balance: " + balance);
        System.out.println("==============================");

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_UPDATE_BALANCE)) {

            ps.setBigDecimal(1, balance);
            ps.setLong(2, accountId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Balance Updated Successfully for Account ID: "
                    + accountId);
            } else {
                System.out.println("⚠️  No Account Found to Update Balance with ID: "
                    + accountId);
            }

        } catch (Exception e) {
            System.err.println("❌ Error Updating Account Balance: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update balance for account: " + accountId, e);
        }
    }


    // =========================================================
    //  WRITE — DELETE
    // =========================================================

    /**
     * Deletes an Account by its primary key.
     *
     * @param accountId bigserial primary key
     */
    @Override
    public void delete(Long accountId) {

        System.out.println("==============================");
        System.out.println("  DELETE ACCOUNT              ");
        System.out.println("  Account ID: " + accountId);
        System.out.println("==============================");

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_DELETE)) {

            ps.setLong(1, accountId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Account Deleted Successfully: " + accountId);
            } else {
                System.out.println("⚠️  No Account Found to Delete with ID: " + accountId);
            }

        } catch (Exception e) {
            System.err.println("❌ Error Deleting Account: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete account: " + accountId, e);
        }
    }


    // =========================================================
    //  HELPER — Map ResultSet Row → Account Object
    // =========================================================

    /**
     * Maps a single ResultSet row to an Account entity.
     *
     * @param rs ResultSet pointing to current row
     * @return Account object populated from DB row
     * @throws SQLException if column mapping fails
     */
    private Account mapRow(ResultSet rs) throws SQLException {

        Account account = new Account();

        account.setId(rs.getLong("id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setIfscCode(rs.getString("ifsc_code"));
        account.setBankName(rs.getString("bank_name"));
        account.setCustomerId(rs.getString("customer_id"));
        account.setAccountType(AccountType.valueOf(rs.getString("account_type")));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setCurrency(rs.getString("currency"));
        account.setAccountStatus(AccountStatus.valueOf(rs.getString("account_status")));
        account.setCreatedBy(rs.getString("created_by"));

        return account;
    }
}
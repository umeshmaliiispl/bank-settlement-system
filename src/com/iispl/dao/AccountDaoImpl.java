package com.iispl.dao;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * AccountDaoImpl
 * JDBC Implementation of AccountDao Interface.
 * Uses HikariCP Connection Pool via DatabaseConfig.
 * All print statements removed — Main class handles display.
 */
public class AccountDaoImpl implements AccountDao {

    // =========================================================
    // SQL CONSTANTS
    // =========================================================

    private static final String SQL_SELECT_ALL =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account ORDER BY id";

    private static final String SQL_SELECT_BY_ACCOUNT_NUMBER =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account WHERE account_number = ?";

    private static final String SQL_SELECT_BY_BANK_NAME =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account WHERE LOWER(bank_name) LIKE LOWER(?) " +
        "ORDER BY bank_name";

    private static final String SQL_SELECT_BY_ACCOUNT_TYPE =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account WHERE account_type = ? ORDER BY id";

    private static final String SQL_SELECT_BY_ACCOUNT_STATUS =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account WHERE account_status = ? ORDER BY id";

    private static final String SQL_SELECT_BY_CUSTOMER_ID =
        "SELECT id, account_number, ifsc_code, bank_name, customer_id, " +
        "account_type, balance, currency, account_status, " +
        "created_at, updated_at, created_by, version " +
        "FROM account WHERE customer_id = ? ORDER BY id";


    // =========================================================
    // READ — FIND ALL
    // =========================================================

    /*
     * Retrieves ALL accounts from the database.
     * No print statements here — Main class handles display.
     *
     * @return List of all Account objects
     */
    @Override
    public List<Account> findAll() {

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SQL_SELECT_ALL)) {

            while (rs.next()) {
                accountList.add(mapRow(rs));
            }

        } catch (Exception e) {
            System.err.println("DAO Error - findAll : " + e.getMessage());
            throw new RuntimeException("Failed to fetch all accounts.", e);
        }

        return accountList;
    }


    // =========================================================
    // READ — FIND BY ACCOUNT NUMBER
    // =========================================================

    /*
     * Finds an Account by its unique account number.
     * No print statements here — Main class handles display.
     *
     * @param accountNumber the unique account number
     * @return Optional<Account>
     */
    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                 SQL_SELECT_BY_ACCOUNT_NUMBER)) {

            ps.setString(1, accountNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (Exception e) {
            System.err.println("DAO Error - findByAccountNumber : "
                + e.getMessage());
            throw new RuntimeException(
                "Failed to fetch account by number: " + accountNumber, e);
        }

        return Optional.empty();
    }


    // =========================================================
    // READ — FIND BY BANK NAME
    // =========================================================

    /*
     * Finds all Accounts belonging to a specific bank.
     * Case-insensitive partial match.
     * No print statements here — Main class handles display.
     *
     * @param bankName bank name keyword to search
     * @return List of matching Account objects
     */
    @Override
    public List<Account> findByBankName(String bankName) {

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                 SQL_SELECT_BY_BANK_NAME)) {

            /*
             * Wrap with % for partial/contains search.
             * Example: "SBI" becomes "%SBI%"
             */
            ps.setString(1, "%" + bankName.trim() + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accountList.add(mapRow(rs));
                }
            }

        } catch (Exception e) {
            System.err.println("DAO Error - findByBankName : " + e.getMessage());
            throw new RuntimeException(
                "Failed to fetch accounts by bank name: " + bankName, e);
        }

        return accountList;
    }


    // =========================================================
    // READ — FIND BY ACCOUNT TYPE
    // =========================================================

    /*
     * Finds all Accounts of a specific AccountType.
     * No print statements here — Main class handles display.
     *
     * @param accountType the AccountType enum value
     * @return List of matching Account objects
     */
    @Override
    public List<Account> findByAccountType(AccountType accountType) {

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                 SQL_SELECT_BY_ACCOUNT_TYPE)) {

            /*
             * Convert enum to String for DB storage.
             * Example: AccountType.SAVINGS → "SAVINGS"
             */
            ps.setString(1, accountType.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accountList.add(mapRow(rs));
                }
            }

        } catch (Exception e) {
            System.err.println("DAO Error - findByAccountType : " + e.getMessage());
            throw new RuntimeException(
                "Failed to fetch accounts by type: " + accountType, e);
        }

        return accountList;
    }


    // =========================================================
    // READ — FIND BY ACCOUNT STATUS
    // =========================================================

    /*
     * Finds all Accounts with a specific AccountStatus.
     * No print statements here — Main class handles display.
     *
     * @param accountStatus the AccountStatus enum value
     * @return List of matching Account objects
     */
    @Override
    public List<Account> findByAccountStatus(AccountStatus accountStatus) {

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                 SQL_SELECT_BY_ACCOUNT_STATUS)) {

            /*
             * Convert enum to String for DB storage.
             * Example: AccountStatus.ACTIVE → "ACTIVE"
             */
            ps.setString(1, accountStatus.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accountList.add(mapRow(rs));
                }
            }

        } catch (Exception e) {
            System.err.println("DAO Error - findByAccountStatus : "
                + e.getMessage());
            throw new RuntimeException(
                "Failed to fetch accounts by status: " + accountStatus, e);
        }

        return accountList;
    }


    // =========================================================
    // READ — FIND BY CUSTOMER ID
    // =========================================================

    /*
     * Finds all Accounts linked to a specific Customer.
     * No print statements here — Main class handles display.
     *
     * @param customerId the customer's business key e.g. CID1001
     * @return List of Account objects belonging to that customer
     */
    @Override
    public List<Account> findByCustomerId(String customerId) {

        List<Account> accountList = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                 SQL_SELECT_BY_CUSTOMER_ID)) {

            ps.setString(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accountList.add(mapRow(rs));
                }
            }

        } catch (Exception e) {
            System.err.println("DAO Error - findByCustomerId : " + e.getMessage());
            throw new RuntimeException(
                "Failed to fetch accounts for customer: " + customerId, e);
        }

        return accountList;
    }


    // =========================================================
    // HELPER — Map ResultSet Row to Account Object
    // =========================================================

    /*
     * Maps a single ResultSet row to an Account entity.
     * Handles unknown enum values safely using try-catch.
     * If DB has unknown AccountType or AccountStatus,
     * sets field to null instead of crashing the app.
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

        /*
         * Safely parse AccountType enum from DB string value.
         * If DB contains an unknown value, set to null and log warning.
         * Prevents crash when new types exist in DB but not in enum.
         */
        String accountTypeStr = rs.getString("account_type");
        try {
            account.setAccountType(AccountType.valueOf(accountTypeStr));
        } catch (IllegalArgumentException e) {
            System.err.println("  ⚠  Unknown AccountType in DB : ["
                + accountTypeStr + "]"
                + " for Account : " + rs.getString("account_number")
                + " — Setting to null.");
            account.setAccountType(null);
        }

        account.setBalance(rs.getBigDecimal("balance"));
        account.setCurrency(rs.getString("currency"));

        /*
         * Safely parse AccountStatus enum from DB string value.
         * If DB contains an unknown value, set to null and log warning.
         * Prevents crash when new statuses exist in DB but not in enum.
         */
        String accountStatusStr = rs.getString("account_status");
        try {
            account.setAccountStatus(AccountStatus.valueOf(accountStatusStr));
        } catch (IllegalArgumentException e) {
            System.err.println("  ⚠  Unknown AccountStatus in DB : ["
                + accountStatusStr + "]"
                + " for Account : " + rs.getString("account_number")
                + " — Setting to null.");
            account.setAccountStatus(null);
        }

        account.setCreatedBy(rs.getString("created_by"));

        /*
         * Safely map created_at timestamp.
         * Returns null if the column is null in DB.
         */
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            account.setCreatedAt(createdAt.toLocalDateTime());
        }

        return account;
    }


}
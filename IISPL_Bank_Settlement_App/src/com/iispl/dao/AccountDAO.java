package com.iispl.dao;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.Account;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountDAO {

    // CREATE
    public void save(Account account) {
        String sql = "INSERT INTO account(account_number, account_name, balance) VALUES (?,?,?)";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, account.getAccountNumber());
            preparedStatement.setString(2, account.getAccountName());
            preparedStatement.setBigDecimal(3, account.getBalance());

            preparedStatement.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // READ BY ID
    public Optional<Account> findById(Long accountId) {
        String sql = "SELECT * FROM account WHERE id=?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, accountId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Account account = mapRow(resultSet);
                return Optional.of(account);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    // READ ALL
    public List<Account> findAll() {
        List<Account> accountList = new ArrayList<>();
        String sql = "SELECT * FROM account";

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                accountList.add(mapRow(resultSet));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return accountList;
    }

    // UPDATE FULL
    public void update(Account account) {
        String sql = "UPDATE account SET account_name=?, balance=? WHERE id=?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, account.getAccountName());
            preparedStatement.setBigDecimal(2, account.getBalance());
            preparedStatement.setLong(3, account.getId());

            preparedStatement.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // UPDATE BALANCE ONLY
    public void updateBalance(Long accountId, java.math.BigDecimal balance) {
        String sql = "UPDATE account SET balance=? WHERE id=?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setBigDecimal(1, balance);
            preparedStatement.setLong(2, accountId);

            preparedStatement.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // DELETE
    public void delete(Long accountId) {
        String sql = "DELETE FROM account WHERE id=?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, accountId);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // COMMON MAPPER
    private Account mapRow(ResultSet resultSet) throws SQLException {
        Account account = new Account();
        account.setId(resultSet.getLong("id"));
        account.setAccountNumber(resultSet.getString("account_number"));
        account.setAccountName(resultSet.getString("account_name"));
        account.setBalance(resultSet.getBigDecimal("balance"));
        return account;
    }
}
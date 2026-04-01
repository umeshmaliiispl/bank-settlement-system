package com.iispl.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.Transaction;

public class TransactionDaoImpl implements TransactionDao  {

    // CREATE
    public void save(Transaction transaction) {
 
    	String sql = "INSERT INTO transaction(account_id, type, amount) VALUES (?,?,?)";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, transaction.getAccountId());
            preparedStatement.setString(2, transaction.getType());
            preparedStatement.setBigDecimal(3, transaction.getAmount());

            preparedStatement.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // READ BY ID
    public Optional<Transaction> findById(Long transactionId) {
        String sql = "SELECT * FROM transaction WHERE id=?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, transactionId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Transaction transaction = mapRow(resultSet);
                return Optional.of(transaction);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    // READ ALL
    public List<Transaction> findAll() {
        List<Transaction> transactionList = new ArrayList<>();
        String sql = "SELECT * FROM transaction";

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
            	transactionList.add(mapRow(resultSet));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return transactionList;
    }

    // DELETE
    public void delete(Long transactionId) {
        String sql = "DELETE FROM transaction WHERE id=?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, transactionId);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // SIMPLE MAPPER (returns a basic Transaction)
    private Transaction mapRow(ResultSet resultSet) throws SQLException {
        Transaction transaction = new Transaction() {
            @Override
            public void execute(com.iispl.entity.Account account) {}
        };

        transaction.setId(resultSet.getLong("id"));
        transaction.setAccountId(resultSet.getLong("account_id"));
        transaction.setType(resultSet.getString("type"));
        transaction.setAmount(resultSet.getBigDecimal("amount"));

        return transaction;
    }
}
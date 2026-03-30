package com.iispl.dao;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionDAO {

    // CREATE
    public void save(Transaction txn) {
        String sql = "INSERT INTO transaction(account_id, type, amount) VALUES (?,?,?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, txn.getAccountId());
            ps.setString(2, txn.getType());
            ps.setBigDecimal(3, txn.getAmount());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // READ BY ID
    public Optional<Transaction> findById(Long id) {
        String sql = "SELECT * FROM transaction WHERE id=?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Transaction txn = mapRow(rs);
                return Optional.of(txn);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    // READ ALL
    public List<Transaction> findAll() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transaction";

        try (Connection con = DatabaseConfig.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    // DELETE
    public void delete(Long id) {
        String sql = "DELETE FROM transaction WHERE id=?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // SIMPLE MAPPER (NOTE: returns basic Transaction)
    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction txn = new Transaction() {
            @Override
            public void execute(com.iispl.entity.Account account) {}
        };

        txn.setId(rs.getLong("id"));
        txn.setAccountId(rs.getLong("account_id"));
        txn.setType(rs.getString("type"));
        txn.setAmount(rs.getBigDecimal("amount"));

        return txn;
    }
}
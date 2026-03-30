package com.iispl.dao;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.Account;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountDAO {

    // CREATE
    public void save(Account acc) {
        String sql = "INSERT INTO account(account_number, account_name, balance) VALUES (?,?,?)";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, acc.getAccountNumber());
            ps.setString(2, acc.getAccountName());
            ps.setBigDecimal(3, acc.getBalance());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // READ BY ID
    public Optional<Account> findById(Long id) {
        String sql = "SELECT * FROM account WHERE id=?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Account acc = mapRow(rs);
                return Optional.of(acc);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    // READ ALL
    public List<Account> findAll() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM account";

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

    // UPDATE FULL
    public void update(Account acc) {
        String sql = "UPDATE account SET account_name=?, balance=? WHERE id=?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, acc.getAccountName());
            ps.setBigDecimal(2, acc.getBalance());
            ps.setLong(3, acc.getId());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // UPDATE BALANCE ONLY
    public void updateBalance(Long id, java.math.BigDecimal balance) {
        String sql = "UPDATE account SET balance=? WHERE id=?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBigDecimal(1, balance);
            ps.setLong(2, id);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // DELETE
    public void delete(Long id) {
        String sql = "DELETE FROM account WHERE id=?";

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // COMMON MAPPER
    private Account mapRow(ResultSet rs) throws SQLException {
        Account acc = new Account();
        acc.setId(rs.getLong("id"));
        acc.setAccountNumber(rs.getString("account_number"));
        acc.setAccountName(rs.getString("account_name"));
        acc.setBalance(rs.getBigDecimal("balance"));
        return acc;
    }
}
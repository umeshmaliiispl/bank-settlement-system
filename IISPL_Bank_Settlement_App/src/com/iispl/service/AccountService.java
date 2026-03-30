package com.iispl.service;

import com.iispl.dao.AccountDAO;
import com.iispl.entity.Account;

import java.math.BigDecimal;
import java.util.List;

public class AccountService {

    private final AccountDAO dao = new AccountDAO();

    public void createAccount(Account acc) {
        dao.save(acc);
    }

    public Account getAccount(Long id) {
        return dao.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public List<Account> getAllAccounts() {
        return dao.findAll();
    }

    public void updateAccount(Account acc) {
        dao.update(acc);
    }

    public void deleteAccount(Long id) {
        dao.delete(id);
    }

    public void credit(Long id, BigDecimal amount) {
        Account acc = getAccount(id);
        acc.setBalance(acc.getBalance().add(amount));
        dao.updateBalance(id, acc.getBalance());
    }

    public void debit(Long id, BigDecimal amount) {
        Account acc = getAccount(id);

        if (acc.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient Balance");
        }

        acc.setBalance(acc.getBalance().subtract(amount));
        dao.updateBalance(id, acc.getBalance());
    }
}
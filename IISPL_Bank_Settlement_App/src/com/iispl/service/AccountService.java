package com.iispl.service;

import com.iispl.dao.AccountDAO;
import com.iispl.entity.Account;

import java.math.BigDecimal;
import java.util.List;

public class AccountService {

    private final AccountDAO dao = new AccountDAO();

    public void createAccount(Account account) {
        dao.save(account);
    }

    public Account getAccount(Long id) {
        return dao.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public List<Account> getAllAccounts() {
        return dao.findAll();
    }

    public void updateAccount(Account account) {
        dao.update(account);
    }

    public void deleteAccount(Long id) {
        dao.delete(id);
    }

    public void credit(Long id, BigDecimal amount) {
        Account account = getAccount(id);

        account.setBalance(account.getBalance().add(amount));

        dao.updateBalance(id, account.getBalance());
    }

    public void debit(Long id, BigDecimal amount) {
        Account account = getAccount(id);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient Balance");
        }

        account.setBalance(account.getBalance().subtract(amount));

        dao.updateBalance(id, account.getBalance());
    }
}
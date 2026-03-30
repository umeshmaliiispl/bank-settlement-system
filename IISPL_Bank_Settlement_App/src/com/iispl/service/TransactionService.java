package com.iispl.service;

import com.iispl.dao.TransactionDAO;
import com.iispl.entity.Account;
import com.iispl.entity.Transaction;

import java.util.List;

public class TransactionService {

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AccountService accountService = new AccountService();

    public void process(Transaction transaction) {

        // 1. Get account
        Account account = accountService.getAccount(transaction.getAccountId());

        // 2. Execute transaction logic
        transaction.execute(account);

        // 3. Update balance in DB
        accountService.updateAccount(account);

        // 4. Save transaction
        transactionDAO.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionDAO.findAll();
    }

    public void deleteTransaction(Long id) {
        transactionDAO.delete(id);
    }
}
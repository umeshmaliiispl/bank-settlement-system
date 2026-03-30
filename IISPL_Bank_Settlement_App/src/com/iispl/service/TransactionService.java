package com.iispl.service;

import com.iispl.dao.TransactionDAO;
import com.iispl.entity.Account;
import com.iispl.entity.Transaction;

import java.util.List;

public class TransactionService {

    private final TransactionDAO txnDAO = new TransactionDAO();
    private final AccountService accService = new AccountService();

    public void process(Transaction txn) {

        // 1. Get account
        Account acc = accService.getAccount(txn.getAccountId());

        // 2. Execute transaction logic
        txn.execute(acc);

        // 3. Update balance in DB
        accService.updateAccount(acc);

        // 4. Save transaction
        txnDAO.save(txn);
    }

    public List<Transaction> getAllTransactions() {
        return txnDAO.findAll();
    }

    public void deleteTransaction(Long id) {
        txnDAO.delete(id);
    }
}
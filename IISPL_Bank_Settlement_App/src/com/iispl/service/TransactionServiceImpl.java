package com.iispl.service;

import java.util.List;

import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;
import com.iispl.entity.Account;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.Transaction;

public class TransactionServiceImpl implements TransactionService {

    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final AccountService accountService = new AccountServiceImpl();

    public void process(Transaction transaction) {

        // 1. Get account
        Account account = accountService.getAccount(transaction.getAccountId());

        // 2. Execute transaction logic
        transaction.execute(account);

        // 3. Update balance in DB
        accountService.updateAccount(account);

        // 4. Save transaction
        transactionDao.save(transaction);
    }

    public List<IncomingTransaction> getAllTransactions() {
        return transactionDao.findAll();
    }

    public void deleteTransaction(Long id) {
    	transactionDao.delete(id);
    }
}
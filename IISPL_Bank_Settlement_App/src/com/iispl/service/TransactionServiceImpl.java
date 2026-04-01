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
    private final TransactionDao dao = new TransactionDaoImpl();

    public void save(IncomingTransaction txn) {
        dao.save(txn);
    }
}
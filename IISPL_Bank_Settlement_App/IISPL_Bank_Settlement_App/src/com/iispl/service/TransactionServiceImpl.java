package com.iispl.service;

import java.util.List;

import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;
import com.iispl.entity.IncomingTransaction;

public class TransactionServiceImpl implements TransactionService {

    private final TransactionDao dao = new TransactionDaoImpl();

    // ✅ SAVE TRANSACTION
    @Override
    public void save(IncomingTransaction txn) {
        dao.save(txn);
    }

    // ✅ GET ALL TRANSACTIONS (USED IN SETTLEMENT)
    @Override
    public List<IncomingTransaction> getAllTransactions() {
        return dao.findAll();
    }

    // ✅ GET BY ID
    @Override
    public IncomingTransaction getTransactionById(long id) {
        return dao.findById(id);
    }
}
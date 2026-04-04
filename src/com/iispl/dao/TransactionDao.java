package com.iispl.dao;


import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionDao {

    public void save(IncomingTransaction txn);
    public List<IncomingTransaction> findAll();
    public List<IncomingTransaction> findSuccessfulTransactions();
    
}

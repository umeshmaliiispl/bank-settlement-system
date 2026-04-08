package com.iispl.dao;

import com.iispl.entity.IncomingTransaction;
import java.util.List;

public interface TransactionDao {

    void save(IncomingTransaction txn);

    List<IncomingTransaction> findAll();

    IncomingTransaction findById(long id);

	List<IncomingTransaction> getAllTransactions();
}
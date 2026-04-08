package com.iispl.dao;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.Transaction;

import java.util.List;

public interface TransactionDao {

    void save(Transaction transaction);

    List<IncomingTransaction> findAll();

    IncomingTransaction findById(long id);

	List<IncomingTransaction> getAllTransactions();

	void delete(Long id);

	void save(IncomingTransaction txn);
}
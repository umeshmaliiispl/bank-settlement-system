package com.iispl.service;

import com.iispl.entity.IncomingTransaction;
import java.util.List;

public interface TransactionService {

    void save(IncomingTransaction txn);

    List<IncomingTransaction> getAllTransactions();

    IncomingTransaction getTransactionById(long id);
}
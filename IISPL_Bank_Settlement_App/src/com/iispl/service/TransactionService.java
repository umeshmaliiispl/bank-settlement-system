package com.iispl.service;


import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.Transaction;

import java.util.List;

public interface TransactionService 
{

    void process(Transaction transaction);

    List<IncomingTransaction> getAllTransactions();

    void deleteTransaction(Long id);
}
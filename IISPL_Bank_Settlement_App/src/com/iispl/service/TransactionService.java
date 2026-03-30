package com.iispl.service;


import com.iispl.entity.Transaction;

import java.util.List;

public interface TransactionService 
{

    void process(Transaction transaction);

    List<Transaction> getAllTransactions();

    void deleteTransaction(Long id);
}
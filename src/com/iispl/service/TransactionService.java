package com.iispl.service;


import com.iispl.entity.IncomingTransaction;

public interface TransactionService 
{
    public void save(IncomingTransaction txn);
    public void printAllTransactions();

}
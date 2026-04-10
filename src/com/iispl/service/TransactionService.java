package com.iispl.service;


import java.util.List;

import com.iispl.entity.IncomingTransaction;

public interface TransactionService 
{
    public void save(IncomingTransaction txn);
    public void printAllTransactions();
	List<IncomingTransaction> getAllTransactions();
    
    

}
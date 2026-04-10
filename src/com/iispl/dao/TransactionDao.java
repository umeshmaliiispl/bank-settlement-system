package com.iispl.dao;

import java.util.List;

import com.iispl.entity.IncomingTransaction;

public interface TransactionDao {

	
    public void checkConnection();
    
    
    /**
     * Saves transaction into DB
     *
     * @param txn Incoming transaction
     * @return true if inserted, false if duplicate
     */
	
	
    boolean save(IncomingTransaction txn);

    
    public void printBankWiseSettlement(String batchId);
    /**
     * Fetch all transactions
     */
    List<IncomingTransaction> findAll();

    /**
     * Fetch only settlement-ready transactions
     * (SUCCESS + QUEUED)
     */
    List<IncomingTransaction> findSuccessfulTransactions();
    public List<IncomingTransaction> getUnsettledTranasactions();

}
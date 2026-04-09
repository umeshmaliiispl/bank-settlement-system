package com.iispl.dao;

import com.iispl.entity.IncomingTransaction;
import java.util.List;

public interface TransactionDao {

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
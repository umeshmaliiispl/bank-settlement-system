package com.iispl.runner;

import com.iispl.dao.TransactionDaoImpl;
import com.iispl.entity.IncomingTransaction;
import com.iispl.service.NettingEngine;
import com.iispl.service.SettlementService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PipelineExecutor {

    public static void run() {

        TransactionDaoImpl transactionDao= new TransactionDaoImpl();
        NettingEngine nettingEngine = new NettingEngine();
        SettlementService settlementService = new SettlementService();

        List<IncomingTransaction> transactions =
        		transactionDao.findSuccessfulTransactions();

        ExecutorService executor = Executors.newFixedThreadPool(5);

        // NETTING (Parallel)
        for (IncomingTransaction transaction : transactions) {
            executor.submit(() -> nettingEngine.process(transaction));
        }

        executor.shutdown();

        while (!executor.isTerminated()) {}

        // SETTLEMENT
        settlementService.settle(nettingEngine);
    }
}
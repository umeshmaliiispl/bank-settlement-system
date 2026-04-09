package com.iispl.runner;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.ProcessingStatus;
import com.iispl.enums.TransactionStatus;
import com.iispl.utility.QueueManager;

public class SettlementProcessor implements Runnable {

	@Override
	public void run() {

	    while (!Thread.currentThread().isInterrupted()) {
	        try {
	            IncomingTransaction txn = QueueManager.QUEUE.poll(5, java.util.concurrent.TimeUnit.SECONDS);

	            if (txn == null) {
	                System.out.println("No more transactions → Consumer exiting...");
	                break; // ✅ EXIT THREAD
	            }

	            if (txn.getTxnStatus() == TransactionStatus.SUCCESS &&
	                txn.getProcessingStatus() == ProcessingStatus.QUEUED) {

	                processSettlement(txn);

	            } else {
	                System.out.printf(
	                    "[SKIPPED ][%s] REF=%s | TXN=%s | PROC=%s%n",
	                    txn.getChannelCode(),
	                    txn.getSourceRef(),
	                    txn.getTxnStatus(),
	                    txn.getProcessingStatus()
	                );
	            }

	        } catch (InterruptedException e) {
	            System.out.println("Consumer interrupted → exiting...");
	            break;
	        }
	    }
	}	
	
    private void processSettlement(IncomingTransaction txn) {

        //  FINAL SAFETY CHECK (DEFENSIVE PROGRAMMING)
        if (txn.getProcessingStatus() != ProcessingStatus.QUEUED) {
            return;
        }

        System.out.printf(
            "[SETTLED ][%s] REF=%s | AMOUNT=%s %s%n",
            txn.getChannelCode(),
            txn.getSourceRef(),
            txn.getAmount(),
            txn.getCurrency()
        );
    }
}

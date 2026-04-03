package com.iispl.runner;

import com.iispl.entity.IncomingTransaction;
import com.iispl.utility.QueueManager;

public class SettlementProcessor implements Runnable {

	@Override
	public void run() {
		while (true) {
			try {
				IncomingTransaction txn = QueueManager.QUEUE.take();

				// Only process valid transactions
				if ("SUCCESS".equals(txn.getTxnStatus().name()) && "QUEUED".equals(txn.getProcessingStatus().name())) {

					processSettlement(txn);
				}

			} catch (Exception e) {
				System.err.println("Consumer error: " + e.getMessage());
			}
		}
	}

	private void processSettlement(IncomingTransaction txn) {
		System.out.println(" SETTLED -> " + txn.getSourceRef() + " | Amount: " + txn.getAmount());
	}
}
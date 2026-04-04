package com.iispl.runner;

import com.iispl.adaptor.AdapterRegistry;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.service.TransactionService;
import com.iispl.utility.QueueManager;

public class IngestionWorker implements Runnable {

	private final String payload;
	private final SourceType sourceType;
	private final TransactionService service;

	public IngestionWorker(String payload, SourceType sourceType, TransactionService service) {
		this.payload = payload;
		this.sourceType = sourceType;
		this.service = service;
	}

	@Override
	public void run() {
		final String thread = Thread.currentThread().getName();

		try {
			// Step 1: Adapt transaction
			IncomingTransaction txn = AdapterRegistry.getInstance().adapt(sourceType, payload);

			// Step 2: Persist
			service.save(txn);

			// Step 3: Push to queue
			QueueManager.QUEUE.put(txn);

			// Step 4: Structured log (aligned with pipeline format)
			System.out.printf(
				    "[PRODUCER][%-18s][%-7s] REF=%-22s | AMT=%12s %-3s | STATUS=%-8s/%-10s%n",
				    thread,
				    safe(txn.getChannelCode()),
				    safe(txn.getSourceRef()),
				    formatAmount(txn.getAmount()),
				    safe(txn.getCurrency()),
				    safe(txn.getTxnStatus()),
				    safe(txn.getProcessingStatus())
				);

		} catch (Exception ex) {
			System.err.printf(
				    "[ERROR   ][%-18s][%-7s] %s%n",
				    thread,
				    sourceType,
				    ex.getMessage()
				);		}
	}

	/**
	 * Logs successful producer processing
	 */
	private void logProducer(IncomingTransaction txn, String threadName) {
		System.out.printf("[PRODUCER][%-15s][%-8s] REF=%-22s | AMT=%12s %-3s | STATUS=%-20s%n", threadName,
				safe(txn.getChannelCode()), safe(txn.getSourceRef()), formatAmount(txn.getAmount()),
				safe(txn.getCurrency()), txn.getTxnStatus() + "/" + txn.getProcessingStatus());
	}

	/**
	 * Logs errors during processing
	 */
	private void logError(Exception ex, String threadName) {
		System.err.printf("[PRODUCER][ERROR][%-15s][%s] %s%n", threadName, sourceType, ex.getMessage());
	}

	private String formatAmount(java.math.BigDecimal amt) {
		if (amt == null)
			return "0.00";
		return String.format("%,.2f", amt);
	}

	private String safe(Object val) {
		return val == null ? "N/A" : val.toString();
	}
}
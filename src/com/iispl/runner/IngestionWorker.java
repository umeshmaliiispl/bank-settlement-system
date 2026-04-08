package com.iispl.runner;

import com.iispl.adaptor.AdapterRegistry;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.service.TransactionService;
import com.iispl.utility.QueueManager;

/**
 * IngestionWorker — Immutable pipeline design.
 *
 * Adapters return immutable IncomingTransaction objects. This worker only reads
 * from them (getters) — no mutation.
 */
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
			// Step 1: Adapt → returns NEW immutable IncomingTransaction
			IncomingTransaction txn = AdapterRegistry.getInstance().adapt(sourceType, payload);

			// Step 2: Persist → service uses toBuilder() internally, no mutation here
			service.save(txn);

			// Step 3: Push to queue
			QueueManager.QUEUE.put(txn);

			// Step 4: Log (READ-ONLY access to immutable txn)
			System.out.printf("[PRODUCER][%-18s][%-7s] REF=%-22s | AMT=%12s %-3s | STATUS=%-8s/%-10s%n", thread,
					safe(txn.getChannelCode()), safe(txn.getSourceRef()), formatAmount(txn.getAmount()),
					safe(txn.getCurrency()), safe(txn.getTxnStatus()), safe(txn.getProcessingStatus()));

		} catch (Exception ex) {
			System.err.printf("[ERROR   ][%-18s][%-7s] %s%n", thread, sourceType, ex.getMessage());
		}
	}

	private static String formatAmount(java.math.BigDecimal amt) {
		return amt == null ? "0.00" : String.format("%,.2f", amt);
	}

	private static String safe(Object val) {
		return val == null ? "N/A" : val.toString();
	}
}

package com.iispl.intefaces;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

/**
 * TransactionAdapter — Strategy Pattern interface.
 *
 * Every source system has exactly ONE adapter that converts its proprietary
 * wire format into a canonical IncomingTransaction POJO.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * SOURCE SYSTEM WIRE FORMAT ADAPTER CLASS ─────────────────────
 * ────────────────────── ───────────────────── CBS (Core Banking) →
 * Pipe-delimited string → CbsAdapter RTGS (Gross Settle) → JSON / Message Queue
 * → RtgsAdapter SWIFT (Cross-border) → MT103 tagged message → SwiftAdapter NEFT
 * (Batch retail) → CSV flat file (NPCI) → NeftUpiAdapter UPI (Real-time) → CSV
 * / REST push → NeftUpiAdapter Fintech (Webhook) → JSON webhook POST →
 * FintechAdapter
 * ─────────────────────────────────────────────────────────────────────────
 *
 * ADDING A NEW SOURCE (e.g. ACH): Step 1 — Create AchAdapter implements
 * TransactionAdapter Step 2 — Add one line in AdapterRegistry constructor Step
 * 3 — Done. Zero changes to pipeline, settlement, or queue code.
 */
public interface TransactionAdapter {

	/**
	 * Parse the raw source payload and return a fully populated canonical
	 * IncomingTransaction ready for the BlockingQueue.
	 *
	 * @param rawPayload exact string received from the source system
	 * @return canonical IncomingTransaction
	 * @throws com.iispl.exception.IngestionException on parse/validation failure
	 */
	IncomingTransaction adapt(String rawPayload);

	/**
	 * The SourceType this adapter handles. Used by AdapterRegistry to build its
	 * routing map at startup.
	 */
	SourceType getSourceType();
}

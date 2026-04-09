package com.iispl.service;

import java.util.concurrent.ConcurrentHashMap;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NettingPosition;
import com.iispl.enums.NetDirection;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

/**
 * NettingEngine — Immutable pipeline design.
 *
 * IncomingTransaction is immutable — we only READ from it (getters).
 * NettingPosition is the mutable accumulator (internal state, not shared
 * externally).
 */
public class NettingEngine {

	private final ConcurrentHashMap<String, NettingPosition> positionMap = new ConcurrentHashMap<>();

	/**
	 * Processes a transaction and updates the bank netting position.
	 * IncomingTransaction is READ-ONLY here — no mutation.
	 */
	public void process(IncomingTransaction txn) {

		// Only process SUCCESS transactions
		if (txn.getTxnStatus() != TransactionStatus.SUCCESS)
			return;

		String bank = txn.getSenderBankName();
		if (bank == null || bank.isEmpty())
			return;

		positionMap.compute(bank, (key, position) -> {

			if (position == null) {
				position = new NettingPosition();
				position.setBankName(bank);
				position.setCurrency(txn.getCurrency());
			}

			// READ from immutable txn — no setters called on txn
			double txnAmount = txn.getAmount() != null ? txn.getAmount().doubleValue() : 0.0;

			if (txn.getTxnType() == TransactionType.DEBIT) {
				position.setGrossDebitAmount(position.getGrossDebitAmount() + txnAmount);
			} else {
				position.setGrossCreditAmount(position.getNetAmount() + txnAmount);
			}

			double net = position.getNetAmount() - position.getGrossDebitAmount();
			position.setNetAmount(net);

			if (net > 0)
				position.setDirection(NetDirection.NET_CREDIT);
			else if (net < 0)
				position.setDirection(NetDirection.NET_DEBIT);
			else
				position.setDirection(NetDirection.FLAT);

			return position;
		});
	}

	public ConcurrentHashMap<String, NettingPosition> getPositions() {
		return positionMap;
	}
}

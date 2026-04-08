package com.iispl.enums;

/**
 * TransactionType — the nature of the financial movement.
 *
 * CREDIT — Money credited to beneficiary account 
 * DEBIT — Money debited from sender account 
 * REVERSAL — Reversal / refund of a previously settled transaction 
 * SWAP — FX swap between two currencies FEE — Bank/partner service charge 
 * INTRABANK — Within same bank (no external settlement needed)
 * 
 */

public enum TransactionType {
	CREDIT, DEBIT, REVERSAL, SWAP, FEE, INTRABANK
}

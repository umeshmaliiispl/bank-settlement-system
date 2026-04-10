package com.iispl.service;

import java.util.concurrent.ConcurrentHashMap;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NettingPosition;
import com.iispl.enums.TransactionStatus;
import com.iispl.enums.TransactionType;

/**
 * NettingEngine — Immutable pipeline design.
 */
public class NettingEngine {

    private final ConcurrentHashMap<String, NettingPosition> positionMap =
            new ConcurrentHashMap<>();

    /**
     * Processes transaction for netting.
     */
    public void process(IncomingTransaction txn) {

        // Only SUCCESS transactions allowed
        if (txn.getTxnStatus() != TransactionStatus.SUCCESS) {
            return;
        }

        String bank = txn.getSenderBankName();

        if (bank == null || bank.isEmpty()) {
            return;
        }

        positionMap.compute(bank, (key, position) -> {

            if (position == null) {
                position = new NettingPosition();
                position.setSenderBank(bank);          //  FIXED
                position.setCurrency(txn.getCurrency());
            }

            // amount
            if (txn.getAmount() == null) {
                return position;
            }

            // Business logic
            if (txn.getTxnType() == TransactionType.DEBIT) {
                position.addDebit(txn.getAmount());
            } else {
                position.addCredit(txn.getAmount());
            }

            // calculate final net
            position.calculateNet();

            return position;
        });
    }

    public ConcurrentHashMap<String, NettingPosition> getPositions() {
        return positionMap;
    }
}


package com.iispl.service;

import java.util.concurrent.ConcurrentHashMap;

import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NettingPosition;
import com.iispl.enums.NetDirection;
import com.iispl.enums.TransactionType;

public class NettingEngine {

    private final ConcurrentHashMap<String, NettingPosition> positionMap =
            new ConcurrentHashMap<>();

    public void process(IncomingTransaction txn) {

        // ONLY SUCCESS
        if (txn.getTxnStatus() != com.iispl.enums.TransactionStatus.SUCCESS)
            return;

        String bank = txn.getSenderBankName();

        positionMap.compute(bank, (key, pos) -> {

            if (pos == null) {
                pos = new NettingPosition();
                pos.setBankName(bank);
                pos.setCurrency(txn.getCurrency());
            }

            if (txn.getTxnType() == TransactionType.DEBIT) {
                pos.setGrossDebitAmount(
                    pos.getGrossDebitAmount() + txn.getAmount().doubleValue()
                );
            } else {
                pos.setGrossCreditAmount(
                    pos.getGrossCreditAmount() + txn.getAmount().doubleValue()
                );
            }

            double net = pos.getGrossCreditAmount() - pos.getGrossDebitAmount();
            pos.setNetAmount(net);

            if (net > 0) pos.setDirection(NetDirection.NET_CREDIT);
            else if (net < 0) pos.setDirection(NetDirection.NET_DEBIT);
            else pos.setDirection(NetDirection.FLAT);

            return pos;
        });
    }

    public ConcurrentHashMap<String, NettingPosition> getPositions() {
        return positionMap;
    }
}
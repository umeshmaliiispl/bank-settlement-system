package com.iispl.service;

import java.time.LocalDate;
import java.util.*;

import com.iispl.dao.NettingPositionDAO;
import com.iispl.dao.NettingPositionDAOImpl;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NettingPosition;
import com.iispl.enums.NetDirection;

public class NettingPositionServiceImpl implements NettingPositionService {

    private final NettingPositionDAO dao = new NettingPositionDAOImpl();

    @Override
    public void calculateAndStore(List<IncomingTransaction> txns) {

        // ✅ Key should be BANK ID (not name)
        Map<Long, NettingPosition> map = new HashMap<>();

        for (IncomingTransaction txn : txns) {

            long bankId = txn.getReceiverBankId(); // ✅ FIXED

            map.putIfAbsent(bankId, new NettingPosition(
                    bankId,                          // ✅ correct
                    txn.getCurrency(),
                    0, 0, 0,
                    NetDirection.FLAT,
                    LocalDate.now()
            ));

            NettingPosition pos = map.get(bankId);

            // ✅ CREDIT → money coming IN
            if (txn.getTxnType().name().equals("CREDIT")) {
                pos.setGrossCreditAmount(
                        pos.getGrossCreditAmount() + txn.getAmount().doubleValue()
                );
            }
            // ✅ DEBIT → money going OUT
            else {
                pos.setGrossDebitAmount(
                        pos.getGrossDebitAmount() + txn.getAmount().doubleValue()
                );
            }

            // ✅ NET CALCULATION
            double net = pos.getGrossCreditAmount() - pos.getGrossDebitAmount();
            pos.setNetAmount(net);

            // ✅ DIRECTION
            if (net > 0) {
                pos.setDirection(NetDirection.NET_CREDIT);
            } else if (net < 0) {
                pos.setDirection(NetDirection.NET_DEBIT);
            } else {
                pos.setDirection(NetDirection.FLAT);
            }
        }

        // ✅ SAVE ALL POSITIONS
        for (NettingPosition p : map.values()) {
            dao.save(p);
        }
    }
}
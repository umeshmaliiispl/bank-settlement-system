package com.iispl.service;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;

import com.iispl.dao.NettingPositionDAO;
import com.iispl.dao.NettingPositionDAOImpl;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NettingPosition;

public class NettingPositionServiceImpl implements NettingPositionService {

    private final NettingPositionDAO dao;

    // Constructor Injection (FIXED)
    public NettingPositionServiceImpl(Connection conn) {
        this.dao = new NettingPositionDAOImpl(conn);
    }

    @Override
    public void calculateAndStore(List<IncomingTransaction> txns) {

        // ✅ Group by bank
        Map<String, NettingPosition> map = new HashMap<>();

        for (IncomingTransaction txn : txns) {

            String bankName = txn.getReceiverBankName();

            // Create if not exists
            map.putIfAbsent(bankName, new NettingPosition());

            NettingPosition pos = map.get(bankName);

            // Set base fields
            pos.setSenderBank(bankName);  // using as bank_name
            pos.setCurrency(txn.getCurrency());
            pos.setPositionDate(LocalDate.now());

            // CREDIT / DEBIT logic
            if (txn.getTxnType().name().equals("CREDIT")) {
                pos.addCredit(txn.getAmount());
            } else {
                pos.addDebit(txn.getAmount());
            }

            // Calculate net
            pos.calculateNet();
        }

        //  Save all in batch
        dao.saveAll(new ArrayList<>(map.values()));
    }
}
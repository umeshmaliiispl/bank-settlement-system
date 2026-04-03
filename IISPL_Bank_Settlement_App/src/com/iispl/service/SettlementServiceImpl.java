package com.iispl.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.iispl.dao.*;
import com.iispl.entity.*;
import com.iispl.enums.*;

public class SettlementServiceImpl implements SettlementService {

    private final SettlementBatchDAO batchDAO = new SettlementBatchDAOImpl();
    private final SettlementRecordDAO recordDAO = new SettlementRecordDAOImpl();
    private final TransactionDao txnDAO = new TransactionDaoImpl();
    private final NettingPositionService nettingService = new NettingPositionServiceImpl();
    private final SettlementInstructionDAO instructionDAO = new SettlementInstructionDAOImpl();

    @Override
    public void processSettlement() {

        List<IncomingTransaction> txns = txnDAO.getAllTransactions();

        if (txns.isEmpty()) {
            System.out.println("❌ No Transactions → No Batch Created");
            return;
        }

        // ✅ CHECK TODAY BATCH
        SettlementBatch batch = batchDAO.findTodayBatch();

        if (batch == null) {
            batch = new SettlementBatch("BCH" + System.currentTimeMillis(), "SYSTEM");
            batchDAO.save(batch);
        }

        // ✅ PROCESS EACH TXN
        for (IncomingTransaction txn : txns) {

            SettlementRecord record = new SettlementRecord(
                    batch.getBatchId(),
                    txn.getId(),
                    txn.getAmount(),
                    SettlementStatus.SETTLED
            );

            recordDAO.save(record);
            batch.addRecord(record);

            // ✅ CREATE INSTRUCTION
            SettlementInstruction ins = new SettlementInstruction(
                    "INS" + System.nanoTime(),
                    txn.getId(),
                    ChannelType.valueOf(txn.getChannelCode()),
                    5,
                    txn.getValueDate(),
                    txn.getSenderBankName(),
                    txn.getReceiverBankName(),
                    InstructionStatus.SENT
            );

            instructionDAO.save(ins);
        }

        // ✅ NETTING
        nettingService.calculateAndStore(txns);

        System.out.println("✅ Settlement Completed for Batch: " + batch.getBatchId());
    }
}
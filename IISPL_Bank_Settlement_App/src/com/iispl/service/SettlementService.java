package com.iispl.service;

import com.iispl.dao.*;
import com.iispl.entity.*;
import com.iispl.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class SettlementService {

    private final SettlementInstructionDAO instructionDAO = new SettlementInstructionDAOImpl();
    private final NettingPositionDAO nettingDAO = new NettingPositionDAOImpl();

    public void settle(NettingEngine engine) {

        String batchId = generateBatchId();

        int totalTxn = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // ─────────────────────────────────────────────
        // PRINT HEADER
        // ─────────────────────────────────────────────
        System.out.println("\n==============================================================");
        System.out.println("                 SETTLEMENT INSTRUCTIONS REPORT");
        System.out.println("==============================================================");

        System.out.printf(
                "%-40s %-30s %-30s %-12s %-10s %-12s\n",
                "Instruction ID",
                "From Bank",
                "To Bank",
                "Amount",
                "Channel",
                "Value Date"
        );

        System.out.println("--------------------------------------------------------------");

        // ─────────────────────────────────────────────
        // PROCESS NETTING POSITIONS
        // ─────────────────────────────────────────────
        for (NettingPosition pos : engine.getPositions().values()) {

            // Save netting first
            nettingDAO.save(pos);

            SettlementInstruction ins = new SettlementInstruction();

            ins.setInstructionId("INS-" + UUID.randomUUID());

            // Branch generation
            String fromBranch = generateBranch(pos.getBankName());
            String toBranch = generateBranch("IISPL Bank");

            ins.setFromBank(pos.getBankName() + " (" + fromBranch + ")");
            ins.setToBank("IISPL Bank (" + toBranch + ")");

            ins.setAmount(Math.abs(pos.getNetAmount()));

            ins.setChannel(ChannelType.RTGS);
            ins.setInstructionStatus(InstructionStatus.INITIATED);

            // ✅ FIX for NPE
            ins.setValueDate(LocalDate.now());

            // Save instruction
            instructionDAO.save(ins);

            // ─────────────────────────────────────────────
            // PRINT ROW
            // ─────────────────────────────────────────────
            System.out.printf(
                    "%-40s %-30s %-30s %-12.2f %-10s %-12s\n",
                    ins.getInstructionId(),
                    ins.getFromBank(),
                    ins.getToBank(),
                    ins.getAmount(),
                    ins.getChannel(),
                    ins.getValueDate()
            );

            totalTxn++;
            totalAmount = totalAmount.add(BigDecimal.valueOf(ins.getAmount()));
        }

        // ─────────────────────────────────────────────
        // FINAL SUMMARY
        // ─────────────────────────────────────────────
        System.out.println("==============================================================");
        System.out.println("Batch ID   : " + batchId);
        System.out.println("Total Txn  : " + totalTxn);
        System.out.println("Total Amt  : " + totalAmount);
        System.out.println("==============================================================");

        System.out.println("✅ Settlement Completed: " + batchId);
    }

    // ─────────────────────────────────────────────
    // BATCH ID GENERATOR
    // ─────────────────────────────────────────────
    private String generateBatchId() {
        return "BATCH-" +
                LocalDate.now().toString().replace("-", "") +
                "-" +
                UUID.randomUUID().toString().substring(0, 6);
    }

    // ─────────────────────────────────────────────
    // BRANCH GENERATOR
    // ─────────────────────────────────────────────
    private String generateBranch(String bankName) {

        String code = bankName.replaceAll("[^A-Za-z]", "")
                .substring(0, Math.min(4, bankName.length()))
                .toUpperCase();

        int branchNumber = (int) (Math.random() * 900) + 100;

        return code + "-BR-" + branchNumber;
    }
}
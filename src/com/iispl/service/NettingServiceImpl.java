package com.iispl.service;

import com.iispl.dao.NettingPositionDAO;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.NettingPosition;
import com.iispl.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class NettingServiceImpl implements NettingService {

    private NettingPositionDAO dao;
    private List<NettingPosition> positions = new ArrayList<>();

    public NettingServiceImpl(NettingPositionDAO dao) {
        this.dao = dao;
    }

    @Override
    public void processNetting(List<IncomingTransaction> transactions) {

        Map<String, NettingPosition> bankMap = new HashMap<>();

        for (IncomingTransaction txn : transactions) {

            if (txn.getTxnStatus() != TransactionStatus.SUCCESS) continue;
            if (txn.getAmount() == null) continue;

            BigDecimal amount = txn.getAmount();

            String senderBank = cleanBankName(txn.getSenderBankName());
            String receiverBank = cleanBankName(txn.getReceiverBankName());

            // ✅ SENDER → DEBIT
            if (senderBank != null) {
                NettingPosition sender = bankMap.computeIfAbsent(senderBank, b -> {
                    NettingPosition n = new NettingPosition();
                    n.setSenderBank(b);
                    n.setPositionDate(LocalDate.now()); // ✅ date stored
                    return n;
                });

                sender.addDebit(amount);
            }

            // ✅ RECEIVER → CREDIT
            if (receiverBank != null) {
                NettingPosition receiver = bankMap.computeIfAbsent(receiverBank, b -> {
                    NettingPosition n = new NettingPosition();
                    n.setSenderBank(b);
                    n.setPositionDate(LocalDate.now());
                    return n;
                });

                receiver.addCredit(amount);
            }
        }

        List<NettingPosition> result = new ArrayList<>(bankMap.values());

        for (NettingPosition np : result) {
            np.calculateNet();
        }

        // ✅ CORRECT CALL
        dao.saveAll(result);

        this.positions = result;
    }

    private String cleanBankName(String bank) {
        if (bank == null || bank.trim().isEmpty() || bank.equalsIgnoreCase("UNKNOWN-UNKN")) {
            return null;
        }
        return bank.trim();
    }

    @Override
    public void printNettingReport() {

        if (positions.isEmpty()) {
            System.out.println("No data");
            return;
        }

        System.out.println("\n========================================================================================================================");
        System.out.println("                              BANK-WISE NETTING SUMMARY");
        System.out.println("========================================================================================================================");

        System.out.printf("%-25s %-20s %-20s %-20s %-15s%n",
                "BANK", "TOTAL CREDIT", "TOTAL DEBIT", "NET AMOUNT", "DIRECTION");

        System.out.println("------------------------------------------------------------------------------------------------------------------------");

        for (NettingPosition np : positions) {
            System.out.printf("%-25s %-20s %-20s %-20s %-15s%n",
                    np.getSenderBank(),
                    format(np.getTotalDepositAmount()),
                    format(np.getTotalWithdrawAmount()),
                    format(np.getNetAmount()),
                    np.getDirection());
        }

        System.out.println("========================================================================================================================");
    }

    private String format(BigDecimal val) {
        return val == null ? "0.00" : String.format("%,.2f", val);
    }
}
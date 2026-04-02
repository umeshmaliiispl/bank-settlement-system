package com.iispl.utility;

import com.iispl.adaptor.AdapterRegistry;
import com.iispl.dao.TransactionDaoImpl;   // ✅ YOUR DAO
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainApp {

    private static final AdapterRegistry registry = AdapterRegistry.getInstance();

    // ✅ DB DAO
    private static final TransactionDaoImpl dao = new TransactionDaoImpl();

    private static final List<IncomingTransaction> allTxns =
            new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("========== IISPL INGESTION PIPELINE ==========");

        processFile("cbs_transactions.txt", SourceType.CBS);
        processFile("neft_transactions.txt", SourceType.NEFT);
        processFile("upi_transactions.txt", SourceType.UPI);

        listAllIncomingTransactions();

        System.out.println("\n========== INGESTION COMPLETE ==========");
    }

    // ─────────────────────────────────────────────
    // PRODUCER (PARSE + SAVE TO DB)
    // ─────────────────────────────────────────────
    private static void processFile(String fileName, SourceType type) {

        System.out.println("\n--- Processing " + type + " ---");

        try (BufferedReader br = new BufferedReader(
                new FileReader("src/resources/" + fileName))) {

            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                line = line.replace("\t", "|").replace("\"", "");

                try {
                    IncomingTransaction txn = registry.adapt(type, line);

                    // ✅ SAVE TO DB
                    dao.save(txn);

                    // ✅ STORE IN MEMORY (for report)
                    //allTxns.add(txn);

                    System.out.println("✔ INGESTED + SAVED: " + txn.toAuditString());

                } catch (Exception e) {
                    System.err.println("[ERROR][" + type + "] " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ File read error: " + fileName + " → " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // FINAL REPORT
    // ─────────────────────────────────────────────
    private static void listAllIncomingTransactions() {
    	
    	List<IncomingTransaction> transactions=TransactionDaoImpl.getAllTransactions();

        if (transactions.isEmpty()) {
            System.out.println("\n⚠ No transactions available.");
            return;
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss a");

        System.out.println(
            "\n======================================================================================================================================");
        System.out.println(
            "                                          ALL INCOMING TRANSACTIONS (MULTI-CHANNEL)");
        System.out.println(
            "======================================================================================================================================");

        System.out.printf(
            "%-20s %-28s %-28s %-12s %12s %-12s %-20s\n",
            "Ref No",
            "Sender Bank",
            "Receiver Bank",
            "Channel",
            "Amount",
            "Txn Status",
            "Txn Time"
        );

        System.out.println(
            "--------------------------------------------------------------------------------------------------------------------------------------");

        for (IncomingTransaction txn : transactions) {

            System.out.printf(
                "%-20s %-28s %-28s %-12s %12s %-12s %-20s\n",
                txn.getSourceRef(),
                txn.getSenderBankName(),
                txn.getReceiverBankName(),
                txn.getChannelCode(),
                txn.getAmount().toPlainString(),
                txn.getTxnStatus(),
                txn.getIngestTimestamp().format(formatter)
            );
        }

        System.out.println(
            "======================================================================================================================================");
    }
}
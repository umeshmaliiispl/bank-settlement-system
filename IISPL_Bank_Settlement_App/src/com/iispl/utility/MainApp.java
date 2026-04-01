package com.iispl.utility;

import com.iispl.adaptor.AdapterRegistry;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainApp {

    private static final AdapterRegistry registry = AdapterRegistry.getInstance();

    private static final List<IncomingTransaction> allTxns =
            new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("========== IISPL INGESTION PIPELINE ==========");

        // ✅ ONLY INGESTION
        processFile("cbs_transactions.txt", SourceType.CBS);
        processFile("neft_transactions.txt", SourceType.NEFT);
        processFile("upi_transactions.txt", SourceType.UPI);

        // ✅ REPORT (FINAL OUTPUT FOR DB TEAM)
        listAllIncomingTransactions();

        System.out.println("\n========== INGESTION COMPLETE ==========");
    }

    // ─────────────────────────────────────────────
    // PRODUCER (ONLY PARSE + STORE)
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

                    // ✅ STORE ONLY (no queue, no settlement)
                    allTxns.add(txn);

                    System.out.println("✔ INGESTED: " + txn.toAuditString());

                } catch (Exception e) {
                    System.err.println("[ERROR][" + type + "] " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ File read error: " + fileName + " → " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // FINAL REPORT (DB TEAM WILL USE THIS DATA)
    // ─────────────────────────────────────────────
    private static void listAllIncomingTransactions() {

        if (allTxns.isEmpty()) {
            System.out.println("\n⚠ No transactions available.");
            return;
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

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

        for (IncomingTransaction txn : allTxns) {

            System.out.printf(
                "%-20s %-28s %-28s %-12s %12s %-12s %-20s\n",
                txn.getSourceRef(),
                txn.getSenderBankName(),
                txn.getReceiverBankName(),
                txn.getChannelCode(),
                txn.getAmount().toPlainString(),
                txn.getTxnStatus(),   // ✅ IMPORTANT (SUCCESS / FAILED / PENDING)
                txn.getIngestTimestamp().format(formatter)
            );
        }

        System.out.println(
            "======================================================================================================================================");
    }
}

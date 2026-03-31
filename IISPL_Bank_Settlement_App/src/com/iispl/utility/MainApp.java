package com.iispl.utility;

import com.iispl.adaptor.AdapterRegistry;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainApp {

    private static final AdapterRegistry registry = AdapterRegistry.getInstance();

    // 🔥 QUEUE (Producer → Consumer)
    private static final BlockingQueue<IncomingTransaction> queue =
            new LinkedBlockingQueue<>();

    // 🔥 STORE ALL TRANSACTIONS (for report)
    private static final List<IncomingTransaction> allTxns =
            new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("========== IISPL INGESTION PIPELINE ==========");

        // STEP 1: PRODUCER
        processFile("cbs_transactions.txt", SourceType.CBS);
        processFile("neft_transactions.txt", SourceType.NEFT);
        processFile("upi_transactions.txt", SourceType.UPI);

        // STEP 2: REPORT (🔥 YOUR REQUIREMENT)
        listAllIncomingTransactions();

        // STEP 3: CONSUMER
        startSettlementEngine();

        System.out.println("\n========== PIPELINE COMPLETE ==========");
    }

    // ─────────────────────────────────────────────
    // PRODUCER
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

                    // ✅ store for report
                    allTxns.add(txn);

                    // ✅ push to queue
                    queue.put(txn);

                    System.out.println("→ QUEUED: " + txn.toAuditString());

                } catch (Exception e) {
                    System.err.println("[ERROR][" + type + "] " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ File read error: " + fileName + " → " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 🔥 REPORT (LIKE YOUR OLD TRANSACTION REPORT)
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
            "%-20s %-28s %-28s %-12s %12s %-10s %-20s\n",
            "Ref No",
            "Sender Bank",
            "Receiver Bank",
            "Channel",
            "Amount",
            "Status",
            "Txn Time"
        );

        System.out.println(
            "--------------------------------------------------------------------------------------------------------------------------------------");

        for (IncomingTransaction txn : allTxns) {

            System.out.printf(
                "%-20s %-28s %-28s %-12s %12s %-10s %-20s\n",
                txn.getSourceRef(),
                txn.getSenderBankName(),
                txn.getReceiverBankName(),
                txn.getChannelCode(),
                txn.getAmount().toPlainString(),
                txn.getProcessingStatus(),
                txn.getIngestTimestamp().format(formatter)
            );
        }

        System.out.println(
            "======================================================================================================================================");
    }

    // ─────────────────────────────────────────────
    // CONSUMER (SETTLEMENT)
    // ─────────────────────────────────────────────
    private static void startSettlementEngine() {

        System.out.println("\n========== SETTLEMENT ENGINE START ==========");

        while (!queue.isEmpty()) {

            try {
                IncomingTransaction txn = queue.take();

                if (!txn.isQueueable()) {
                    System.out.println("⚠ SKIPPED: " + txn.toAuditString());
                    continue;
                }

                System.out.println("💰 SETTLING → " + txn.toAuditString());

                // future:
                // txn.setProcessingStatus(ProcessingStatus.PROCESSED);

            } catch (Exception e) {
                System.err.println("Settlement error → " + e.getMessage());
            }
        }

        System.out.println("========== SETTLEMENT COMPLETE ==========");
    }
}
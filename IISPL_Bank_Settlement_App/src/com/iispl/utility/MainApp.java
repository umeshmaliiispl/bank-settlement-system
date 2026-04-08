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
                    allTxns.add(txn);

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

        List<IncomingTransaction> txns = dao.findAll();

        if (txns.isEmpty()) {
            System.out.println("\n⚠ No transactions available.");
            return;
        }

        System.out.println("\n=============================================================================================================================================================================================");
        System.out.println("                                      					ALL INCOMING TRANSACTIONS (COMPLETE DB VIEW)");
        System.out.println("=============================================================================================================================================================================================");

        System.out.printf(
            "%-4s %-20s %-8s %-12s %-5s %-10s %-10s %-12s %-20s %-20s %-20s %-8s %-10s %-10s %-10s\n",
            "ID", "REF", "TYPE", "AMOUNT", "CUR",
            "TXN_STATUS", "PROC_ST", "VAL_DATE",
            "INGEST_TIME", "CREATED_AT", "UPDATED_AT",
            "VER", "CHANNEL", "PRIORITY", "ERROR"
        );

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        for (IncomingTransaction t : txns) {

            String ingest = t.getIngestTimestamp() != null ? t.getIngestTimestamp().format(fmt) : "N/A";
            String created = t.getCreatedAt() != null ? t.getCreatedAt().format(fmt) : "N/A";
            String updated = t.getUpdatedAt() != null ? t.getUpdatedAt().format(fmt) : "N/A";

            System.out.printf(
                "%-4d %-20s %-8s %-12.2f %-5s %-10s %-10s %-12s %-20s %-20s %-20s %-8d %-10s %-10d %-10s\n",
                t.getId(),
                t.getSourceRef(),
                t.getTxnType(),
                t.getAmount().doubleValue(),
                t.getCurrency(),
                t.getTxnStatus(),
                t.getProcessingStatus(),
                t.getValueDate(),
                ingest,
                created,
                updated,
                t.getVersion(),
                t.getChannelCode(),
                t.getPriority(),
                (t.getErrorMessage() != null ? "YES" : "NO")
            );
        }

        System.out.println("=============================================================================================================================================================================================");
    }
}
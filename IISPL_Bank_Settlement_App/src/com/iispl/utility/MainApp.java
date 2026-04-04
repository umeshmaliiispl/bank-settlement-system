package com.iispl.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.iispl.adaptor.AdapterRegistry;
import com.iispl.config.AppInitializer;
import com.iispl.config.ExecutorConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.runner.IngestionWorker;
import com.iispl.runner.SettlementProcessor;
import com.iispl.service.TransactionService;
import com.iispl.service.TransactionServiceImpl;

public class MainApp {

	private static final TransactionService transactionService = new TransactionServiceImpl();
	private static final AdapterRegistry adapterRegistry = AdapterRegistry.getInstance();
	private static final List<IncomingTransaction> auditList = new ArrayList<>();

	public static void main(String[] args) {

		System.out.println("========== MULTI-THREADED INGESTION PIPELINE ==========");

		
		// Step 1: Initialize system DB
        AppInitializer.init();
        
        
		// 1. START CONSUMERS
		for (int i = 0; i < 5; i++) {
			ExecutorConfig.CONSUMER_POOL.submit(new SettlementProcessor());
		}

		// 2. START PRODUCERS
		processFile("cbs_transactions.txt", SourceType.CBS);
		processFile("neft_transactions.txt", SourceType.NEFT);
		processFile("upi_transactions.txt", SourceType.UPI);
		processFile("rtgs_transactions.txt", SourceType.RTGS);
		processSwiftFile("swift_transactions.txt", SourceType.SWIFT);
		processFile("fintech_transactions.txt", SourceType.FINTECH);

		
	


        // Step 2: Start pipeline
		System.out.println("========== INGESTION STARTED ==========");

		// WAIT FOR PRODUCERS TO FINISH (CORRECT WAY)
		ExecutorConfig.PRODUCER_POOL.shutdown();
		try {
			ExecutorConfig.PRODUCER_POOL.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// NOW PRINT FINAL REPORT (FROM DB)
		transactionService.printAllTransactions();
	}

	// ─────────────────────────────────────────────
	// FILE PROCESSING -> PRODUCER THREADS
	 
	private static void processFile(String fileName, SourceType sourceType) {

		System.out.println("\n--- Processing " + sourceType + " ---");

		try (BufferedReader reader = new BufferedReader(new FileReader("src/resources/" + fileName))) {

			String line;

			while ((line = reader.readLine()) != null) {

				if (line.trim().isEmpty())
					continue;

				// SUBMIT EACH TRANSACTION TO THREAD POOL
				ExecutorConfig.PRODUCER_POOL.submit(new IngestionWorker(line.trim(), sourceType, transactionService));
			}

		} catch (Exception e) {
			System.err.println(" File error [" + fileName + "] ->  " + e.getMessage());
		}
	}

	// ─────────────────────────────────────────────
	// SWIFT MULTI-LINE HANDLING
	// ─────────────────────────────────────────────
	private static void processSwiftFile(String fileName, SourceType sourceType) {

		System.out.println("\n--- Processing " + sourceType + " ---");

		try (BufferedReader reader = new BufferedReader(new FileReader("src/resources/" + fileName))) {

			StringBuilder block = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {

				if (line.trim().isEmpty()) {

					if (block.length() > 0) {

						ExecutorConfig.PRODUCER_POOL
								.submit(new IngestionWorker(block.toString(), sourceType, transactionService));

						block.setLength(0);
					}

				} else {
					block.append(line).append("\n");
				}
			}

			if (block.length() > 0) {
				ExecutorConfig.PRODUCER_POOL
						.submit(new IngestionWorker(block.toString(), sourceType, transactionService));
			}

		} catch (Exception e) {
			System.err.println("SWIFT error → " + e.getMessage());
		}
	}

	private static void processTransaction(String payload, SourceType sourceType) {

		try {
			IncomingTransaction txn = adapterRegistry.adapt(sourceType, payload);

			// VALIDATION + SAVE
			transactionService.save(txn);

			auditList.add(txn);

			System.out.println("PROCESSED: " + txn.toAuditString());

		} catch (Exception e) {
			System.err.println("[ERROR][" + sourceType + "] " + e.getMessage());
		}
	}

	private static void printSettlementReport(List<IncomingTransaction> list) {

		if (list.isEmpty()) {
			System.out.println("\n No transactions available.");
			return;
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

                    // ✅ STORE IN MEMORY (for report)
                    //allTxns.add(txn);

		System.out.printf("%-18s %-8s %-8s %-18s %-18s %12s %-6s %-10s %-14s %-20s\n", "Ref ID", "Channel", "Type",
				"Sender Bank", "Receiver Bank", "Amount", "Currency", "TxnStatus", "Processing Status", "Txn Time");

		System.out.println(
				"----------------------------------------------------------------------------------------------");

		int queued = 0, failed = 0, flagged = 0;

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

			System.out.printf("%-18s %-8s %-8s %-18s %-18s %12.2f %-6s %-10s %-14s %-20s\n", safe(txn.getSourceRef()),
					safe(txn.getChannelCode()), safe(txn.getTxnType()), safe(txn.getSenderBankName()),
					safe(txn.getReceiverBankName()), txn.getAmount() != null ? txn.getAmount().doubleValue() : 0.0,
					safe(txn.getCurrency()), safe(txn.getTxnStatus()), processingStatus,
					txn.getValueDate() != null ? txn.getValueDate().format(formatter) : "N/A");
		}

        System.out.printf(
            "%-20s %-28s %-28s %-12s %14s %-14s %-20s\n",
            "Ref No",
            "Sender Bank",
            "Receiver Bank",
            "Channel",
            "Amount",
            "Txn Status",
            "Txn Time"
        );

		System.out.println("TOTAL READY (QUEUED): " + queued);
		System.out.println("TOTAL FAILED: " + failed);
		System.out.println("TOTAL FLAGGED: " + flagged);

        for (IncomingTransaction txn : transactions) {

            System.out.printf(
                "%-20s %-28s %-28s %-12s %14s %-14s %-20s\n",
                txn.getSourceRef(),
                txn.getSenderBankName(),
                txn.getReceiverBankName(),
                txn.getChannelCode(),
                txn.getAmount().toPlainString(),
                txn.getTxnStatus(),
                txn.getIngestTimestamp().format(formatter)
            );
        }

}
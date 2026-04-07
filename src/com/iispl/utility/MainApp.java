package com.iispl.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private static final TransactionService transactionService = new TransactionServiceImpl();
    private static final AdapterRegistry adapterRegistry = AdapterRegistry.getInstance();
    private static final List<IncomingTransaction> auditList = new ArrayList<>();

    public static void main(String[] args) {

        log.info("========== MULTI-THREADED INGESTION PIPELINE ==========");

        // Step 1: Initialize system DB
        AppInitializer.init();
        log.info("Database initialized successfully");

        // 1. START CONSUMERS
        log.info("Starting Settlement Consumers...");
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

        log.info("========== INGESTION STARTED ==========");

        // WAIT FOR PRODUCERS
        ExecutorConfig.PRODUCER_POOL.shutdown();
        try {
            ExecutorConfig.PRODUCER_POOL.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Producer pool interrupted", e);
            Thread.currentThread().interrupt();
        }

        // FINAL REPORT
        log.info("========== PRINTING FINAL REPORT ==========");
        transactionService.printAllTransactions();
    }

    // FILE PROCESSING
    private static void processFile(String fileName, SourceType sourceType) {

        log.info("Processing file: {} | Source: {}", fileName, sourceType);

        try (BufferedReader reader = new BufferedReader(
                new FileReader("src/resources/" + fileName))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty())
                    continue;

                ExecutorConfig.PRODUCER_POOL.submit(
                        new IngestionWorker(line.trim(), sourceType, transactionService)
                );
            }

        } catch (Exception e) {
            log.error("File error [{}] → {}", fileName, e.getMessage(), e);
        }
    }

    // SWIFT FILE PROCESSING
    private static void processSwiftFile(String fileName, SourceType sourceType) {

        log.info("Processing SWIFT file: {}", fileName);

        try (BufferedReader reader = new BufferedReader(
                new FileReader("src/resources/" + fileName))) {

            StringBuilder block = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {

                    if (block.length() > 0) {
                        ExecutorConfig.PRODUCER_POOL.submit(
                                new IngestionWorker(block.toString(), sourceType, transactionService)
                        );
                        block.setLength(0);
                    }

                } else {
                    block.append(line).append("\n");
                }
            }

            if (block.length() > 0) {
                ExecutorConfig.PRODUCER_POOL.submit(
                        new IngestionWorker(block.toString(), sourceType, transactionService)
                );
            }

        } catch (Exception e) {
            log.error("SWIFT processing error -> {}", e.getMessage(), e);
        }
    }
}
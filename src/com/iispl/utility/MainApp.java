

package com.iispl.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iispl.adaptor.AdapterRegistry;
import com.iispl.config.AppInitializer;
import com.iispl.config.DatabaseConfig;
import com.iispl.config.ExecutorConfig;
import com.iispl.entity.Customer;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.runner.IngestionWorker;
import com.iispl.runner.SettlementProcessor;
import com.iispl.service.CustomerService;
import com.iispl.service.TransactionService;
import com.iispl.service.TransactionServiceImpl;
import com.iispl.service.CustomerServiceImpl;

public class MainApp {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    // =========================================================
    //  Service Instances
    // =========================================================
    private static final TransactionService transactionService  = new TransactionServiceImpl();
    private static final CustomerService    customerService     = new CustomerServiceImpl();
    private static final AdapterRegistry    adapterRegistry     = AdapterRegistry.getInstance();
    private static final List<IncomingTransaction> auditList    = new ArrayList<>();

    // =========================================================
    //  MAIN
    // =========================================================
    public static void main(String[] args) {

        log.info("========== MULTI-THREADED INGESTION PIPELINE ==========");

        // ✅ Step 1: Initialize system DB
        AppInitializer.init();
        log.info("Database initialized successfully");

        try {

            // =========================================================
            // ✅ CUSTOMER OPERATIONS
            // =========================================================
            log.info("\n");
            log.info("╔══════════════════════════════════════════╗");
            log.info("║       CUSTOMER SERVICE OPERATIONS        ║");
            log.info("╚══════════════════════════════════════════╝");

            // ----------------------------------------------------------
            // 1️⃣  REGISTER NEW CUSTOMER
            // ----------------------------------------------------------
            log.info("---------- 1. REGISTER CUSTOMER ----------");

            Customer newCustomer = new Customer(
                "SYSTEM",       // createdBy
                "CID9999",      // customerId
                "Test User",    // fullName
                "PENDING",      // kycStatus
                "ACTIVE"        // customerStatus
            );

//            boolean isRegistered = customerService.registerCustomer(newCustomer);
//            if (isRegistered) {
//                log.info("✅ Customer Registered → CID: {} | Name: {}",
//                    newCustomer.getCustomerId(),
//                    newCustomer.getFullName());
//            } else {
//                log.warn("⚠️  Customer Registration Failed → CID: {}",
//                    newCustomer.getCustomerId());
//            }

            // ----------------------------------------------------------
            // 2️⃣  GET ALL CUSTOMERS
            // ----------------------------------------------------------
            log.info("---------- 2. GET ALL CUSTOMERS ----------");

            List<Customer> allCustomers = customerService.getAllCustomers();

            if (allCustomers.isEmpty()) {
                log.warn("⚠️  No Customers Found in Database");
            } else {
                log.info("✅ Total Customers: {}", allCustomers.size());
                log.info("--------------------------------------------");

                allCustomers.forEach(customer ->
                    log.info("  CID: {} | Name: {} | KYC: {} | Status: {}",
                        customer.getCustomerId(),
                        customer.getFullName(),
                        customer.getKycStatus(),
                        customer.getCustomerStatus()
                    )
                );

                log.info("--------------------------------------------");
            }

            // ----------------------------------------------------------
            // 3️⃣  GET CUSTOMER BY CUSTOMER ID (CID)
            // ----------------------------------------------------------
            log.info("---------- 3. GET CUSTOMER BY CUSTOMER ID ----------");

            String searchCustomerId = "CID1001";

            Optional<Customer> customerById =
                customerService.getCustomerByCustomerId(searchCustomerId);

            customerById.ifPresentOrElse(
                customer -> {
                    log.info("✅ Customer Found!");
                    log.info("   CID     : {}", customer.getCustomerId());
                    log.info("   Name    : {}", customer.getFullName());
                    log.info("   KYC     : {}", customer.getKycStatus());
                    log.info("   Status  : {}", customer.getCustomerStatus());
                },
                () -> log.warn("⚠️  Customer Not Found → CID: {}", searchCustomerId)
            );

            // ----------------------------------------------------------
            // 4️⃣  GET CUSTOMERS BY FULL NAME
            // ----------------------------------------------------------
            log.info("---------- 4. GET CUSTOMERS BY FULL NAME ----------");

            String searchName = "Rahul";

            List<Customer> customersByName =
                customerService.getCustomersByFullName(searchName);

            if (customersByName.isEmpty()) {
                log.warn("⚠️  No Customers Found with Name: {}", searchName);
            } else {
                log.info("✅ Customers Found with Name '{}': {}", searchName, customersByName.size());
                customersByName.forEach(customer ->
                    log.info("   CID: {} | Name: {} | KYC: {} | Status: {}",
                        customer.getCustomerId(),
                        customer.getFullName(),
                        customer.getKycStatus(),
                        customer.getCustomerStatus()
                    )
                );
            }

            // ----------------------------------------------------------
            // 5️⃣  GET ACTIVE CUSTOMERS
            // ----------------------------------------------------------
            log.info("---------- 5. GET ACTIVE CUSTOMERS ----------");

            List<Customer> activeCustomers = customerService.getActiveCustomers();

            if (activeCustomers.isEmpty()) {
                log.warn("⚠️  No Active Customers Found");
            } else {
                log.info("✅ Total Active Customers: {}", activeCustomers.size());
                activeCustomers.forEach(customer ->
                    log.info("   CID: {} | Name: {} | Status: {}",
                        customer.getCustomerId(),
                        customer.getFullName(),
                        customer.getCustomerStatus()
                    )
                );
            }

            // ----------------------------------------------------------
            // 6️⃣  GET KYC VERIFIED CUSTOMERS
            // ----------------------------------------------------------
            log.info("---------- 6. GET KYC VERIFIED CUSTOMERS ----------");

            List<Customer> verifiedCustomers = customerService.getVerifiedCustomers();

            if (verifiedCustomers.isEmpty()) {
                log.warn("⚠️  No KYC Verified Customers Found");
            } else {
                log.info("✅ Total KYC Verified Customers: {}", verifiedCustomers.size());
                verifiedCustomers.forEach(customer ->
                    log.info("   CID: {} | Name: {} | KYC: {}",
                        customer.getCustomerId(),
                        customer.getFullName(),
                        customer.getKycStatus()
                    )
                );
            }

            // ----------------------------------------------------------
            // 7️⃣  UPDATE CUSTOMER DETAILS
            // ----------------------------------------------------------
            log.info("---------- 7. UPDATE CUSTOMER ----------");

            Customer updateCustomer = new Customer();
            updateCustomer.setCustomerId("CID9999");
            updateCustomer.setFullName("Test User Updated");
            updateCustomer.setKycStatus("VERIFIED");
            updateCustomer.setCustomerStatus("ACTIVE");

            boolean isUpdated = customerService.updateCustomer(updateCustomer);

            if (isUpdated) {
                log.info("✅ Customer Updated → CID: {} | New Name: {} | KYC: {}",
                    updateCustomer.getCustomerId(),
                    updateCustomer.getFullName(),
                    updateCustomer.getKycStatus());
            } else {
                log.warn("⚠️  Customer Update Failed → CID: {}",
                    updateCustomer.getCustomerId());
            }

            // ----------------------------------------------------------
            // 8️⃣  UPDATE KYC STATUS
            // ----------------------------------------------------------
            log.info("---------- 8. UPDATE KYC STATUS ----------");

            boolean isKycUpdated = customerService.updateKycStatus("CID9999", "VERIFIED");

            if (isKycUpdated) {
                log.info("✅ KYC Status Updated → CID: CID9999 | Status: VERIFIED");
            } else {
                log.warn("⚠️  KYC Status Update Failed → CID: CID9999");
            }

            // ----------------------------------------------------------
            // 9️⃣  DEACTIVATE CUSTOMER
            // ----------------------------------------------------------
            log.info("---------- 9. DEACTIVATE CUSTOMER ----------");

            boolean isDeactivated = customerService.deactivateCustomer("CID9999");

            if (isDeactivated) {
                log.info("✅ Customer Deactivated → CID: CID9999");
            } else {
                log.warn("⚠️  Customer Deactivation Failed → CID: CID9999");
            }

            // ----------------------------------------------------------
            // 🔟  ACTIVATE CUSTOMER
            // ----------------------------------------------------------
            log.info("---------- 10. ACTIVATE CUSTOMER ----------");

            boolean isActivated = customerService.activateCustomer("CID9999");

            if (isActivated) {
                log.info("✅ Customer Activated → CID: CID9999");
            } else {
                log.warn("⚠️  Customer Activation Failed → CID: CID9999");
            }

            // ----------------------------------------------------------
            // 1️⃣1️⃣  REMOVE CUSTOMER
            // ----------------------------------------------------------
            log.info("---------- 11. REMOVE CUSTOMER ----------");

            boolean isRemoved = customerService.removeCustomer("CID9999");

            if (isRemoved) {
                log.info("✅ Customer Removed Successfully → CID: CID9999");
            } else {
                log.warn("⚠️  Customer Removal Failed → CID: CID9999");
            }

            log.info("╔══════════════════════════════════════════╗");
            log.info("║   CUSTOMER OPERATIONS COMPLETED ✅       ║");
            log.info("╚══════════════════════════════════════════╝");

            // =========================================================
            // ✅ TRANSACTION PIPELINE OPERATIONS
            // =========================================================
            log.info("\n");
            log.info("╔══════════════════════════════════════════╗");
            log.info("║       TRANSACTION PIPELINE START         ║");
            log.info("╚══════════════════════════════════════════╝");

            // 1. START CONSUMERS
            log.info("Starting Settlement Consumers...");
            for (int i = 0; i < 5; i++) {
                ExecutorConfig.CONSUMER_POOL.submit(new SettlementProcessor());
            }

            // 2. START PRODUCERS
            processFile("cbs_transactions.txt",     SourceType.CBS);
            processFile("neft_transactions.txt",    SourceType.NEFT);
            processFile("upi_transactions.txt",     SourceType.UPI);
            processFile("rtgs_transactions.txt",    SourceType.RTGS);
            processSwiftFile("swift_transactions.txt", SourceType.SWIFT);
            processFile("fintech_transactions.txt", SourceType.FINTECH);

            log.info("========== INGESTION STARTED ==========");

            // WAIT FOR PRODUCERS
            ExecutorConfig.PRODUCER_POOL.shutdown();
            try {
                ExecutorConfig.PRODUCER_POOL.awaitTermination(
                    30, java.util.concurrent.TimeUnit.SECONDS
                );
            } catch (InterruptedException e) {
                log.error("Producer pool interrupted", e);
                Thread.currentThread().interrupt();
            }

            // FINAL REPORT
            log.info("========== PRINTING FINAL REPORT ==========");
            transactionService.printAllTransactions();

        } finally {
            // ✅ Shutdown HikariCP pool gracefully
            DatabaseConfig.shutdown();
            log.info("✅ Database Connection Pool Closed");
        }
    }


    // =========================================================
    //  FILE PROCESSING
    // =========================================================

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


    // =========================================================
    //  SWIFT FILE PROCESSING
    // =========================================================

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





//package com.iispl.utility;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.iispl.adaptor.AdapterRegistry;
//import com.iispl.config.AppInitializer;
//import com.iispl.config.ExecutorConfig;
//import com.iispl.entity.IncomingTransaction;
//import com.iispl.enums.SourceType;
//import com.iispl.runner.IngestionWorker;
//import com.iispl.runner.SettlementProcessor;
//import com.iispl.service.TransactionService;
//import com.iispl.service.TransactionServiceImpl;
//
//public class MainApp {
//
//    private static final Logger log = LoggerFactory.getLogger(MainApp.class);
//
//    private static final TransactionService transactionService = new TransactionServiceImpl();
//    private static final AdapterRegistry adapterRegistry = AdapterRegistry.getInstance();
//    private static final List<IncomingTransaction> auditList = new ArrayList<>();
//
//    public static void main(String[] args) {
//
//        log.info("========== MULTI-THREADED INGESTION PIPELINE ==========");
//
//        // Step 1: Initialize system DB
//        AppInitializer.init();
//        log.info("Database initialized successfully");
//
//        // 1. START CONSUMERS
//        log.info("Starting Settlement Consumers...");
//        for (int i = 0; i < 5; i++) {
//            ExecutorConfig.CONSUMER_POOL.submit(new SettlementProcessor());
//        }
//
//        // 2. START PRODUCERS
//        processFile("cbs_transactions.txt", SourceType.CBS);
//        processFile("neft_transactions.txt", SourceType.NEFT);
//        processFile("upi_transactions.txt", SourceType.UPI);
//        processFile("rtgs_transactions.txt", SourceType.RTGS);
//        processSwiftFile("swift_transactions.txt", SourceType.SWIFT);
//        processFile("fintech_transactions.txt", SourceType.FINTECH);
//
//        log.info("========== INGESTION STARTED ==========");
//
//        // WAIT FOR PRODUCERS
//        ExecutorConfig.PRODUCER_POOL.shutdown();
//        try {
//            ExecutorConfig.PRODUCER_POOL.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            log.error("Producer pool interrupted", e);
//            Thread.currentThread().interrupt();
//        }
//
//        // FINAL REPORT
//        log.info("========== PRINTING FINAL REPORT ==========");
//        transactionService.printAllTransactions();
//    }
//
//    // FILE PROCESSING
//    private static void processFile(String fileName, SourceType sourceType) {
//
//        log.info("Processing file: {} | Source: {}", fileName, sourceType);
//
//        try (BufferedReader reader = new BufferedReader(
//                new FileReader("src/resources/" + fileName))) {
//
//            String line;
//
//            while ((line = reader.readLine()) != null) {
//
//                if (line.trim().isEmpty())
//                    continue;
//
//                ExecutorConfig.PRODUCER_POOL.submit(
//                        new IngestionWorker(line.trim(), sourceType, transactionService)
//                );
//            }
//
//        } catch (Exception e) {
//            log.error("File error [{}] → {}", fileName, e.getMessage(), e);
//        }
//    }
//
//    // SWIFT FILE PROCESSING
//    private static void processSwiftFile(String fileName, SourceType sourceType) {
//
//        log.info("Processing SWIFT file: {}", fileName);
//
//        try (BufferedReader reader = new BufferedReader(
//                new FileReader("src/resources/" + fileName))) {
//
//            StringBuilder block = new StringBuilder();
//            String line;
//
//            while ((line = reader.readLine()) != null) {
//
//                if (line.trim().isEmpty()) {
//
//                    if (block.length() > 0) {
//                        ExecutorConfig.PRODUCER_POOL.submit(
//                                new IngestionWorker(block.toString(), sourceType, transactionService)
//                        );
//                        block.setLength(0);
//                    }
//
//                } else {
//                    block.append(line).append("\n");
//                }
//            }
//
//            if (block.length() > 0) {
//                ExecutorConfig.PRODUCER_POOL.submit(
//                        new IngestionWorker(block.toString(), sourceType, transactionService)
//                );
//            }
//
//        } catch (Exception e) {
//            log.error("SWIFT processing error -> {}", e.getMessage(), e);
//        }
//    }
//}

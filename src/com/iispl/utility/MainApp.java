

package com.iispl.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iispl.adaptor.AdapterRegistry;
import com.iispl.config.AppInitializer;
import com.iispl.config.DatabaseConfig;
import com.iispl.config.ExecutorConfig;
import com.iispl.entity.Customer;
import com.iispl.entity.IncomingTransaction;
import com.iispl.entity.SettlementBatch;
import com.iispl.enums.SourceType;
import com.iispl.runner.IngestionWorker;
import com.iispl.runner.SettlementProcessor;
import com.iispl.service.SettlementService;
import com.iispl.service.TransactionService;
import com.iispl.service.TransactionServiceImpl;
import com.iispl.service.CustomerServiceImpl;

// ✅ ONLY THESE IMPORTS ADDED
import com.iispl.dao.NettingPositionDAOImpl;
import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;
import com.iispl.service.NettingService;
import com.iispl.service.NettingServiceImpl;

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
    	
    	

    	Scanner scanner=new Scanner(System.in);
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

        // WAIT FOR PRODUCERS
        ExecutorConfig.PRODUCER_POOL.shutdown();
        try {
            ExecutorConfig.PRODUCER_POOL.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Producer pool interrupted", e);
            Thread.currentThread().interrupt();
        }
        
     // WAIT FOR CONSUMERS (VERY IMPORTANT)
        ExecutorConfig.CONSUMER_POOL.shutdown();

        try {
            ExecutorConfig.CONSUMER_POOL.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // important
            System.out.println("Consumer pool interrupted");
        }

        // FINAL REPORT
        log.info("========== PRINTING FINAL REPORT ==========");
        transactionService.printAllTransactions();


        // =========================================================
        // ✅ NETTING POSITION (ADDED ONLY)
        // =========================================================
        try {
            NettingService nettingService =
                    new NettingServiceImpl(new NettingPositionDAOImpl(DBConnection.getConnection()));

            TransactionDao dao = new TransactionDaoImpl();
            nettingService.processNetting(dao.findSuccessfulTransactions());
            nettingService.printNettingReport();

        } catch (Exception e) {
            log.error("Netting error", e);
        }

        
        char mychoice;
        do {
	        System.out.println("\n2.Create Settlement Batch \n3.Get All Batch Summary \n4.Get Batch Wise Summary \n5.Send Batches to NPCI \n6.View NPCI Sent Batches  ");
	        int choice=scanner.nextInt();
	        scanner.nextLine();
	        switch(choice)
	        {
		        case 2:
		        	System.out.println("Welcome to Settlement");
		            SettlementService.createSettlementBatch();
		            break;
	        
		        case 3:
		        	SettlementService service = new SettlementService();
		            List<SettlementBatch> batches = service.getAllBatchesWithRecords();
		            service.printBatchSummary(batches);
		            break;
		            
		        case 4:

		            SettlementService service1 = new SettlementService();

		            // STEP 1: Get all batches with records
		            List<SettlementBatch> batches1 =
		                    service1.getAllBatchesWithRecords();

		            // STEP 2: Print list with index
		            service1.printBatchListWithIndex(batches1);

		            // STEP 3: Ask user to choose
		            System.out.print("Enter batch number: ");
		            int batchChoice = Integer.parseInt(scanner.nextLine());

		            // STEP 4: Validate input
		            if (batchChoice < 1 || batchChoice > batches1.size()) {
		                System.out.println("Invalid selection!");
		                break;
		            }

		            // STEP 5: Get selected batch
		            SettlementBatch selectedBatch = batches1.get(batchChoice - 1);

		            // STEP 6: Print single batch
		            service1.printSingleBatchSummary(selectedBatch);

		            break;
		            
		        case 5:
		        	SettlementService settlementService = new SettlementService();
		        	List<SettlementBatch> allBatches = settlementService.getAllBatchesWithRecords();
		        	SettlementService.sendBatchToNpc(allBatches);
		        	break;
		        	
		        case 6:
		        	SettlementService.viewXmlByIndex();
		            break;
		        default:
		        	System.out.println("Enter right choice");
		        	break;
	        }
	        System.out.println("Do you want to continue(y/n)");
	        mychoice=scanner.next().charAt(0);
        }
        while(mychoice=='y' || mychoice=='Y');
        System.out.println("Thank you...");
        
        

        
        
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


// package com.iispl.utility;

// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.util.ArrayList;
// import java.util.List;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.iispl.adaptor.AdapterRegistry;
// import com.iispl.config.AppInitializer;
// import com.iispl.config.ExecutorConfig;
// import com.iispl.entity.IncomingTransaction;
// import com.iispl.enums.SourceType;
// import com.iispl.runner.IngestionWorker;
// import com.iispl.runner.SettlementProcessor;
// import com.iispl.service.TransactionService;
// import com.iispl.service.TransactionServiceImpl;

// public class MainApp {

//     private static final Logger log = LoggerFactory.getLogger(MainApp.class);

//     private static final TransactionService transactionService = new TransactionServiceImpl();
//     private static final AdapterRegistry adapterRegistry = AdapterRegistry.getInstance();
//     private static final List<IncomingTransaction> auditList = new ArrayList<>();

//     public static void main(String[] args) {

//         log.info("========== MULTI-THREADED INGESTION PIPELINE ==========");

//         // Step 1: Initialize system DB
//         AppInitializer.init();
//         log.info("Database initialized successfully");

//         // 1. START CONSUMERS
//         log.info("Starting Settlement Consumers...");
//         for (int i = 0; i < 5; i++) {
//             ExecutorConfig.CONSUMER_POOL.submit(new SettlementProcessor());
//         }

//         // 2. START PRODUCERS
//         processFile("cbs_transactions.txt", SourceType.CBS);
//         processFile("neft_transactions.txt", SourceType.NEFT);
//         processFile("upi_transactions.txt", SourceType.UPI);
//         processFile("rtgs_transactions.txt", SourceType.RTGS);
//         processSwiftFile("swift_transactions.txt", SourceType.SWIFT);
//         processFile("fintech_transactions.txt", SourceType.FINTECH);

//         log.info("========== INGESTION STARTED ==========");

//         // WAIT FOR PRODUCERS
//         ExecutorConfig.PRODUCER_POOL.shutdown();
//         try {
//             ExecutorConfig.PRODUCER_POOL.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
//         } catch (InterruptedException e) {
//             log.error("Producer pool interrupted", e);
//             Thread.currentThread().interrupt();
//         }

//         // FINAL REPORT
//         log.info("========== PRINTING FINAL REPORT ==========");
//         transactionService.printAllTransactions();
//     }

//     // FILE PROCESSING
//     private static void processFile(String fileName, SourceType sourceType) {

//         log.info("Processing file: {} | Source: {}", fileName, sourceType);

//         try (BufferedReader reader = new BufferedReader(
//                 new FileReader("src/resources/" + fileName))) {

//             String line;

//             while ((line = reader.readLine()) != null) {

//                 if (line.trim().isEmpty())
//                     continue;

//                 ExecutorConfig.PRODUCER_POOL.submit(
//                         new IngestionWorker(line.trim(), sourceType, transactionService)
//                 );
//             }

//         } catch (Exception e) {
//             log.error("File error [{}] → {}", fileName, e.getMessage(), e);
//         }
//     }

//     // SWIFT FILE PROCESSING
//     private static void processSwiftFile(String fileName, SourceType sourceType) {

//         log.info("Processing SWIFT file: {}", fileName);

//         try (BufferedReader reader = new BufferedReader(
//                 new FileReader("src/resources/" + fileName))) {

//             StringBuilder block = new StringBuilder();
//             String line;

//             while ((line = reader.readLine()) != null) {

//                 if (line.trim().isEmpty()) {

//                     if (block.length() > 0) {
//                         ExecutorConfig.PRODUCER_POOL.submit(
//                                 new IngestionWorker(block.toString(), sourceType, transactionService)
//                         );
//                         block.setLength(0);
//                     }

//                 } else {
//                     block.append(line).append("\n");
//                 }
//             }

//             if (block.length() > 0) {
//                 ExecutorConfig.PRODUCER_POOL.submit(
//                         new IngestionWorker(block.toString(), sourceType, transactionService)
//                 );
//             }

//         } catch (Exception e) {
//             log.error("SWIFT processing error -> {}", e.getMessage(), e);
//         }
//     }
// }


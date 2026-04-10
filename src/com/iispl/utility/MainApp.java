

package com.iispl.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iispl.config.AppInitializer;
import com.iispl.config.DatabaseConfig;
import com.iispl.config.ExecutorConfig;
import com.iispl.dao.AccountDaoImpl;
import com.iispl.dao.CustomerDAOImpl;
import com.iispl.dao.NettingPositionDAOImpl;
import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;
import com.iispl.entity.Account;
import com.iispl.entity.Customer;
import com.iispl.entity.SettlementBatch;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;
import com.iispl.enums.SourceType;
import com.iispl.runner.IngestionWorker;
import com.iispl.runner.SettlementProcessor;
import com.iispl.service.AccountService;
import com.iispl.service.AccountServiceImpl;
import com.iispl.service.CustomerService;
import com.iispl.service.CustomerServiceImpl;
import com.iispl.service.NettingService;
import com.iispl.service.NettingServiceImpl;
import com.iispl.service.SettlementService;
import com.iispl.service.SettlementServiceImpl;
import com.iispl.service.TransactionService;
import com.iispl.service.TransactionServiceImpl;
import com.iispl.service.CustomerServiceImpl;

//ONLY THESE IMPORTS ADDED
import com.iispl.dao.NettingPositionDAOImpl;
import com.iispl.dao.TransactionDao;
import com.iispl.dao.TransactionDaoImpl;
import com.iispl.service.NettingService;
import com.iispl.service.NettingServiceImpl;

public class MainApp {

	private static final Logger log = LoggerFactory.getLogger(MainApp.class);

	private static final TransactionService transactionService = new TransactionServiceImpl();

	private static final CustomerService customerService = new CustomerServiceImpl(new CustomerDAOImpl());

	private static final AccountService accountService = new AccountServiceImpl(new AccountDaoImpl());

	// ================= SAFE INPUT METHOD =================
	private static int readInt(Scanner scanner) {

		while (true) {

			System.out.print("Enter your choice: ");

			try {
				int value = Integer.parseInt(scanner.nextLine().trim());
				return value;

			} catch (NumberFormatException e) {
				System.out.println("Invalid input! Please enter a number.");
			}
		}
	}

	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);

		AppInitializer.init();

		char continueChoice;

		do {
			System.out.println("\n==================================================");
			System.out.println("        IISPL BANK SETTLEMENT SYSTEM");
			System.out.println("==================================================");
			System.out.println("1. Run Ingestion Pipeline");
			System.out.println("2. Run Netting Operations");
			System.out.println("3. Settlement Batch Menu");
			System.out.println("4. Customer & Account Details");
			System.out.println("0. Exit");
			System.out.println("==================================================");

			int choice = readInt(scanner);

			switch (choice) {

			case 1:
				runIngestionPipeline();
				break;

			case 2:
				runNettingOperations();
				break;

			case 3:
				runSettlementMenu(scanner);
				break;

			case 4:
				runCustomerMenu(scanner);
				break;

			case 0:
				System.out.println("Exiting...");
				scanner.close();
				return;

			default:
				System.out.println("Invalid choice!");
			}

			System.out.print("\nContinue? (y/n): ");
			continueChoice = scanner.nextLine().trim().toLowerCase().charAt(0);

		} while (continueChoice == 'y');

		scanner.close();
	}

	private static void runIngestionPipeline() {

		// Reset shutdown flag
		QueueManager.PRODUCERS_DONE.set(false);

		// Create FRESH pools every time — no RejectedExecutionException
		ExecutorService producerPool = ExecutorConfig.newProducerPool();
		ExecutorService consumerPool = ExecutorConfig.newConsumerPool();

		int consumerCount = 5;

		// Start consumers
		for (int i = 0; i < consumerCount; i++) {
			consumerPool.submit(new SettlementProcessor());
		}

		// Submit producer tasks
//	    processFile("cbs_transactions.txt",    SourceType.CBS,    producerPool);
//	    processFile("neft_transactions.txt",   SourceType.NEFT,   producerPool);
//	    processFile("upi_transactions.txt",    SourceType.UPI,    producerPool);
//	    processFile("rtgs_transactions.txt",   SourceType.RTGS,   producerPool);
//	    processSwiftFile("swift_transactions.txt", SourceType.SWIFT, producerPool);
//	    processFile("fintech_transactions.txt", SourceType.FINTECH, producerPool);

		processFile("cbs_transactions_1.txt", SourceType.CBS, producerPool);
		processFile("neft_transactions_1.txt", SourceType.NEFT, producerPool);
		processFile("upi_transactions_1.txt", SourceType.UPI, producerPool);
		processFile("rtgs_transactions_1.txt", SourceType.RTGS, producerPool);
		processSwiftFile("swift_transactions_1.txt", SourceType.SWIFT, producerPool);
		processFile("fintech_transactions_1.txt", SourceType.FINTECH, producerPool);

		// Wait for all producers to finish
		producerPool.shutdown();
		try {
			producerPool.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// Signal consumers — all data is in the queue
		QueueManager.PRODUCERS_DONE.set(true);

		// Consumers drain queue and exit
		consumerPool.shutdown();
		try {
			consumerPool.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		transactionService.printAllTransactions();
	}

	// ================= NETTING =================
	private static void runNettingOperations() {

		NettingService nettingService = new NettingServiceImpl(
				new NettingPositionDAOImpl(DBConnection.getConnection()));

		TransactionDao dao = new TransactionDaoImpl();

		nettingService.processNetting(dao.findSuccessfulTransactions());

		nettingService.printNettingReport();
	}

	private static void runSettlementMenu(Scanner scanner) {

		char mychoice;

		do {

			System.out.println("\n====== SETTLEMENT MENU ======");
			System.out.println("1. Create Settlement Batch");
			System.out.println("2. Get All Batch Summary");
			System.out.println("3. Get Batch Wise Summary");
			System.out.println("4. Send Batches to NPCI");
			System.out.println("5. View NPCI Sent Batches");
			System.out.println("0. Back");

			int choice = readInt(scanner);

			switch (choice) {

			// ===============================
			// CREATE SETTLEMENT BATCH
			// ===============================
			case 1:
				System.out.println("Welcome to Settlement");
				SettlementService.createSettlementBatch();
				break;

			// ===============================
			// GET ALL BATCH SUMMARY
			// ===============================
			case 2:
				SettlementService service = new SettlementServiceImpl();

				List<SettlementBatch> batches = service.getAllBatchesWithRecords();

				service.printBatchSummary(batches);
				break;

			// ===============================
			// GET SINGLE BATCH SUMMARY
			// ===============================
			case 3:

				SettlementService service1 = new SettlementServiceImpl();

				List<SettlementBatch> batches1 = service1.getAllBatchesWithRecords();

				service1.printBatchListWithIndex(batches1);

				System.out.print("Enter batch number: ");

				int batchChoice;

				try {
					batchChoice = Integer.parseInt(scanner.nextLine().trim());
				} catch (NumberFormatException e) {
					System.out.println("Invalid input! Please enter a valid batch number.");
					break;
				}

				if (batchChoice < 1 || batchChoice > batches1.size()) {

					System.out.println("Invalid selection!");
					break;
				}

				SettlementBatch selectedBatch = batches1.get(batchChoice - 1);

				service1.printSingleBatchSummary(selectedBatch);

				break;

			// ===============================
			// SEND TO NPCI
			// ===============================
			case 4:

				SettlementService settlementService = new SettlementServiceImpl();

				List<SettlementBatch> allBatches = settlementService.getAllBatchesWithRecords();

				SettlementService.sendBatchToNpc(allBatches);

				break;

			// ===============================
			// VIEW SENT XML
			// ===============================
			case 5:
				SettlementService.viewXmlByIndex();
				break;

			// ===============================
			// BACK
			// ===============================
			case 0:
				return;

			default:
				System.out.println("Invalid choice! Enter right choice.");
				break;
			}

			System.out.print("Do you want to continue (y/n): ");
			mychoice = scanner.nextLine().trim().toLowerCase().charAt(0);

		} while (mychoice == 'y');

		System.out.println("Thank you...");
	}

	private static void processFile(String fileName, SourceType sourceType, ExecutorService producerPool) {
		try (BufferedReader reader = new BufferedReader(new FileReader("src/resources/" + fileName))) {
			String line;
			while ((line = reader.readLine()) != null) {
				producerPool.submit(new IngestionWorker(line, sourceType, transactionService));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void processSwiftFile(String fileName, SourceType sourceType, ExecutorService producerPool) {
		log.info("Processing SWIFT file: {}", fileName);
		try (BufferedReader reader = new BufferedReader(new FileReader("src/resources/" + fileName))) {
			StringBuilder block = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					if (block.length() > 0) {
						producerPool.submit(new IngestionWorker(block.toString(), sourceType, transactionService));
						block.setLength(0);
					}
				} else {
					block.append(line).append("\n");
				}
			}
			if (block.length() > 0) {
				producerPool.submit(new IngestionWorker(block.toString(), sourceType, transactionService));
			}
		} catch (Exception e) {
			log.error("SWIFT processing error -> {}", e.getMessage(), e);
		}
	}

	// ================= CUSTOMER =================
	private static void runCustomerMenu(Scanner scanner) {

		int choice;

		do {
			System.out.println("\n====== CUSTOMER & ACCOUNT ======");
			System.out.println("1. Get All Customers");
			System.out.println("2. Get Customer by ID");
			System.out.println("3. Get Customer by Full Name");
			System.out.println("4. Get All Active Customers");
			System.out.println("5. Get All Verified Customers");
			System.out.println("6. Get All Accounts");
			System.out.println("7. Get Account by Number");
			System.out.println("8. Get Accounts by Bank Name");
			System.out.println("9. Get Accounts by Account Type");
			System.out.println("10. Get Accounts by Account Status");
			System.out.println("11. Get Accounts by Customer ID");
			System.out.println("0. Back");

			choice = readInt(scanner);

			switch (choice) {
			case 1:
				getAllCustomers();
				break;
			case 2:
				getCustomerByCustomerId(scanner);
				break;
			case 3:
				getCustomersByFullName(scanner);
				break;
			case 4:
				getActiveCustomers();
				break;
			case 5:
				getVerifiedCustomers();
				break;
			case 6:
				getAllAccounts();
				break;
			case 7:
				getAccountByAccountNumber(scanner);
				break;
			case 8:
				getAccountsByBankName(scanner);
				break;
			case 9:
				getAccountsByAccountType(scanner);
				break;
			case 10:
				getAccountsByAccountStatus(scanner);
				break;
			case 11:
				getAccountsByCustomerId(scanner);
				break;
			case 0:
				break;
			default:
				System.out.println("Invalid choice!");
				break;
			}

		} while (choice != 0);
	}

	// ================= CUSTOMER METHODS =================
	private static void getAllCustomers() {
		printCustomerTable(customerService.getAllCustomers());
	}

	private static void getCustomerByCustomerId(Scanner scanner) {
		System.out.print("Enter Customer ID: ");
		String id = scanner.nextLine();

		Optional<Customer> result = customerService.getCustomerByCustomerId(id);

		if (result.isPresent()) {
			printCustomerTable(List.of(result.get()));
		} else {
			System.out.println("No Customer Found.");
		}
	}

	private static void getCustomersByFullName(Scanner scanner) {
		System.out.print("Enter Full Name: ");
		String name = scanner.nextLine();
		printCustomerTable(customerService.getCustomersByFullName(name));
	}

	private static void getActiveCustomers() {
		printCustomerTable(customerService.getActiveCustomers());
	}

	private static void getVerifiedCustomers() {
		printCustomerTable(customerService.getVerifiedCustomers());
	}

	// ================= ACCOUNT METHODS =================
	private static void getAllAccounts() {
		printAccountTable(accountService.getAllAccounts());
	}

	private static void getAccountByAccountNumber(Scanner scanner) {
		System.out.print("Enter Account Number: ");
		String acc = scanner.nextLine();

		Optional<Account> result = accountService.getAccountByAccountNumber(acc);

		if (result.isPresent()) {
			printAccountTable(List.of(result.get()));
		} else {
			System.out.println("No Account Found.");
		}
	}

	private static void getAccountsByBankName(Scanner scanner) {
		System.out.print("Enter Bank Name: ");
		String bank = scanner.nextLine();
		printAccountTable(accountService.getAccountsByBankName(bank));
	}

	private static void getAccountsByAccountType(Scanner scanner) {

		System.out.println("Available Account Types:");
		System.out.println("SAVINGS");
		System.out.println("CURRENT");
		System.out.println("FIXED_DEPOSIT");
		System.out.println("RECURRING_DEPOSIT");
		System.out.println("FOREX");

		System.out.print("Enter Account Type: ");
		String type = scanner.nextLine().trim().toUpperCase();

		try {

			AccountType accountType = AccountType.valueOf(type);

			printAccountTable(accountService.getAccountsByAccountType(accountType));

		} catch (IllegalArgumentException e) {

			System.out.println("Invalid Account Type Entered.");
			System.out.println("Please use valid values only.");
		}
	}

	private static void getAccountsByAccountStatus(Scanner scanner) {

		System.out.println("Available Account Status:");
		System.out.println("ACTIVE");
		System.out.println("INACTIVE");
		System.out.println("FROZEN");
		System.out.println("CLOSED");
		System.out.println("BLOCKED");

		System.out.print("Enter Account Status: ");
		String status = scanner.nextLine().trim().toUpperCase();

		try {

			AccountStatus accountStatus = AccountStatus.valueOf(status);

			printAccountTable(accountService.getAccountsByAccountStatus(accountStatus));

		} catch (IllegalArgumentException e) {

			System.out.println("Invalid Account Status Entered.");
			System.out.println("Please use only:");
			System.out.println("ACTIVE, INACTIVE, FROZEN, CLOSED, BLOCKED");
		}
	}

	private static void getAccountsByCustomerId(Scanner scanner) {
		System.out.print("Enter Customer ID: ");
		String customerId = scanner.nextLine();
		printAccountTable(accountService.getAccountsByCustomerId(customerId));
	}

	private static void printCustomerTable(List<Customer> customerList) {

		System.out.println();
		System.out.println(
				"=================================================================================================================");
		System.out.printf("%-5s %-12s %-25s %-15s %-18s %-15s%n", "NO", "CUSTOMER ID", "FULL NAME", "KYC STATUS",
				"CUSTOMER STATUS", "CREATED BY");
		System.out.println(
				"=================================================================================================================");

		if (customerList.isEmpty()) {
			System.out.println("No Customers Found.");
		} else {

			int count = 1;

			for (Customer customer : customerList) {

				String fullName = customer.getFullName().replace("\n", "").replace("\r", "").trim();

				if (fullName.length() > 24) {
					fullName = fullName.substring(0, 24);
				}

				String createdBy = customer.getCreatedBy().replace("\n", "").replace("\r", "").trim();

				if (createdBy.length() > 14) {
					createdBy = createdBy.substring(0, 14);
				}

				System.out.printf("%-5d %-12s %-25s %-15s %-18s %-15s%n", count++, customer.getCustomerId(), fullName,
						customer.getKycStatus(), customer.getCustomerStatus(), createdBy.toUpperCase());
			}

		}

		System.out.println(
				"=================================================================================================================");
		System.out.println("Total Customers : " + customerList.size());
		System.out.println();
	}

	// ================= PRINT ACCOUNT TABLE =================
	private static void printAccountTable(List<Account> accountList) {

		System.out.println();
		System.out.println(
				"================================================================================================================================================================");
		System.out.printf("%-5s %-15s %-22s %-15s %-12s %-18s %-15s %-12s %-18s%n", "NO", "ACCOUNT NO", "BANK NAME",
				"IFSC CODE", "CUST ID", "ACCOUNT TYPE", "BALANCE", "CURRENCY", "ACCOUNT STATUS");
		System.out.println(
				"================================================================================================================================================================");

		if (accountList.isEmpty()) {
			System.out.println("No Accounts Found.");
		} else {
			int count = 1;
			for (Account account : accountList) {
				System.out.printf("%-5d %-15s %-22s %-15s %-12s %-18s %-15.2f %-12s %-18s%n", count++,
						account.getAccountNumber(), account.getBankName(), account.getIfscCode(),
						account.getCustomerId(), account.getAccountType(), account.getBalance(), account.getCurrency(),
						account.getAccountStatus());
			}
		}

		System.out.println(
				"================================================================================================================================================================");
		System.out.println("Total Accounts : " + accountList.size());
		System.out.println();
	}

}

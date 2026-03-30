package com.iispl.utility;

import com.iispl.entity.Account;
import com.iispl.entity.CreditTransaction;
import com.iispl.entity.DebitTransaction;
import com.iispl.entity.InterBankTransaction;
import com.iispl.entity.Transaction;
import com.iispl.service.AccountService;
import com.iispl.service.TransactionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class MainMenuForTransactionDomainFeature {

    private static final AccountService accountService = new AccountService();
    private static final TransactionService transactionService = new TransactionService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        while (true) {
            displayMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> createAccount();
                case "2" -> viewAccountById();
                case "3" -> viewAllAccounts();
                case "4" -> creditAccount();
                case "5" -> debitAccount();
                case "6" -> processInterBankTransaction();
                case "7" -> viewAllTransactions();
                case "8" -> deleteAccount();
                case "9" -> deleteTransaction();
                case "0" -> {
                    System.out.println("Exiting... Thank you!");
                    System.exit(0);
                }
                default -> System.out.println("Invalid choice. Try again!");
            }

            System.out.println("\n------------------------------------\n");
        }
    }

    private static void displayMenu() {
        System.out.println("===== BANKING SYSTEM MENU =====");
        System.out.println("1. Create Account");
        System.out.println("2. View Account by ID");
        System.out.println("3. View All Accounts");
        System.out.println("4. Credit Amount to Account");
        System.out.println("5. Debit Amount from Account");
        System.out.println("6. InterBank Transaction");
        System.out.println("7. View All Transactions");
        System.out.println("8. Delete Account");
        System.out.println("9. Delete Transaction");
        System.out.println("0. Exit");
        System.out.println("===============================");
    }

    private static void createAccount() {
        try {
            System.out.print("Enter Account Number: ");
            String accountNumber = scanner.nextLine();

            System.out.print("Enter Account Name: ");
            String accountName = scanner.nextLine();

            System.out.print("Enter Initial Balance: ");
            BigDecimal initialBalance = new BigDecimal(scanner.nextLine());

            Account account = new Account();
            account.setAccountNumber(accountNumber);
            account.setAccountName(accountName);
            account.setBalance(initialBalance);

            accountService.createAccount(account);
            System.out.println("Account created successfully with ID: " + account.getId());
        } catch (Exception e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    private static void viewAccountById() {
        try {
            System.out.print("Enter Account ID: ");
            Long accountId = Long.parseLong(scanner.nextLine());

            Account account = accountService.getAccount(accountId);
            System.out.println("Account ID: " + account.getId());
            System.out.println("Account Number: " + account.getAccountNumber());
            System.out.println("Account Name: " + account.getAccountName());
            System.out.println("Balance: " + account.getBalance());
        } catch (Exception e) {
            System.out.println("Error fetching account: " + e.getMessage());
        }
    }

    private static void viewAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }

        System.out.println("All Accounts:");
        for (Account account : accounts) {
            System.out.println("ID: " + account.getId() +
                    " | Number: " + account.getAccountNumber() +
                    " | Name: " + account.getAccountName() +
                    " | Balance: " + account.getBalance());
        }
    }

    private static void creditAccount() {
        try {
            System.out.print("Enter Account ID: ");
            Long accountId = Long.parseLong(scanner.nextLine());

            System.out.print("Enter Amount to Credit: ");
            BigDecimal creditAmount = new BigDecimal(scanner.nextLine());

            accountService.credit(accountId, creditAmount);
            System.out.println("Amount credited successfully!");
        } catch (Exception e) {
            System.out.println("Error crediting account: " + e.getMessage());
        }
    }

    private static void debitAccount() {
        try {
            System.out.print("Enter Account ID: ");
            Long accountId = Long.parseLong(scanner.nextLine());

            System.out.print("Enter Amount to Debit: ");
            BigDecimal debitAmount = new BigDecimal(scanner.nextLine());

            accountService.debit(accountId, debitAmount);
            System.out.println("Amount debited successfully!");
        } catch (Exception e) {
            System.out.println("Error debiting account: " + e.getMessage());
        }
    }

    private static void processInterBankTransaction() {
        try {
            System.out.print("Enter Sender Account ID: ");
            Long senderAccountId = Long.parseLong(scanner.nextLine());

            System.out.print("Enter Receiver Account Number: ");
            String receiverAccountNumber = scanner.nextLine();

            System.out.print("Enter Amount: ");
            BigDecimal transactionAmount = new BigDecimal(scanner.nextLine());

            InterBankTransaction transaction = new InterBankTransaction();
            transaction.setAccountId(senderAccountId);
            transaction.setReceiverAccount(receiverAccountNumber);
            transaction.setAmount(transactionAmount);

            transactionService.process(transaction);
            System.out.println("InterBank transaction processed successfully!");
        } catch (Exception e) {
            System.out.println("Error processing transaction: " + e.getMessage());
        }
    }

    private static void viewAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        System.out.println("All Transactions:");
        for (Transaction transaction : transactions) {
            System.out.print("ID: " + transaction.getId() +
                    " | Account ID: " + transaction.getAccountId() +
                    " | Type: " + transaction.getType() +
                    " | Amount: " + transaction.getAmount());

            if (transaction instanceof InterBankTransaction interBankTransaction) {
                System.out.print(" | Receiver: " + interBankTransaction.getReceiverAccount());
            }
            System.out.println();
        }
    }

    private static void deleteAccount() {
        try {
            System.out.print("Enter Account ID to delete: ");
            Long accountId = Long.parseLong(scanner.nextLine());

            accountService.deleteAccount(accountId);
            System.out.println("Account deleted successfully!");
        } catch (Exception e) {
            System.out.println("Error deleting account: " + e.getMessage());
        }
    }

    private static void deleteTransaction() {
        try {
            System.out.print("Enter Transaction ID to delete: ");
            Long transactionId = Long.parseLong(scanner.nextLine());

            transactionService.deleteTransaction(transactionId);
            System.out.println("Transaction deleted successfully!");
        } catch (Exception e) {
            System.out.println("Error deleting transaction: " + e.getMessage());
        }
    }
}
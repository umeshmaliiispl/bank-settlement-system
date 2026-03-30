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
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        while (true) {
            showMenu();
            System.out.print("Enter your choice: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> createAccount();
                case "2" -> viewAccount();
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

    private static void showMenu() {
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
            String accNumber = sc.nextLine();

            System.out.print("Enter Account Name: ");
            String accName = sc.nextLine();

            System.out.print("Enter Initial Balance: ");
            BigDecimal balance = new BigDecimal(sc.nextLine());

            Account acc = new Account();
            acc.setAccountNumber(accNumber);
            acc.setAccountName(accName);
            acc.setBalance(balance);

            accountService.createAccount(acc);
            System.out.println("Account created successfully with ID: " + acc.getId());
        } catch (Exception e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    private static void viewAccount() {
        try {
            System.out.print("Enter Account ID: ");
            Long id = Long.parseLong(sc.nextLine());

            Account acc = accountService.getAccount(id);
            System.out.println("Account ID: " + acc.getId());
            System.out.println("Account Number: " + acc.getAccountNumber());
            System.out.println("Account Name: " + acc.getAccountName());
            System.out.println("Balance: " + acc.getBalance());
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
        for (Account acc : accounts) {
            System.out.println("ID: " + acc.getId() + " | Number: " + acc.getAccountNumber() +
                    " | Name: " + acc.getAccountName() + " | Balance: " + acc.getBalance());
        }
    }

    private static void creditAccount() {
        try {
            System.out.print("Enter Account ID: ");
            Long id = Long.parseLong(sc.nextLine());
            System.out.print("Enter Amount to Credit: ");
            BigDecimal amount = new BigDecimal(sc.nextLine());

            accountService.credit(id, amount);
            System.out.println("Amount credited successfully!");
        } catch (Exception e) {
            System.out.println("Error crediting account: " + e.getMessage());
        }
    }

    private static void debitAccount() {
        try {
            System.out.print("Enter Account ID: ");
            Long id = Long.parseLong(sc.nextLine());
            System.out.print("Enter Amount to Debit: ");
            BigDecimal amount = new BigDecimal(sc.nextLine());

            accountService.debit(id, amount);
            System.out.println("Amount debited successfully!");
        } catch (Exception e) {
            System.out.println("Error debiting account: " + e.getMessage());
        }
    }

    private static void processInterBankTransaction() {
        try {
            System.out.print("Enter Sender Account ID: ");
            Long senderId = Long.parseLong(sc.nextLine());
            System.out.print("Enter Receiver Account Number: ");
            String receiverAcc = sc.nextLine();
            System.out.print("Enter Amount: ");
            BigDecimal amount = new BigDecimal(sc.nextLine());

            InterBankTransaction txn = new InterBankTransaction();
            txn.setAccountId(senderId);
            txn.setReceiverAccount(receiverAcc);
            txn.setAmount(amount);

            transactionService.process(txn);
            System.out.println("InterBank transaction processed successfully!");
        } catch (Exception e) {
            System.out.println("Error processing transaction: " + e.getMessage());
        }
    }

    private static void viewAllTransactions() {
        List<Transaction> txns = transactionService.getAllTransactions();
        if (txns.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        System.out.println("All Transactions:");
        for (Transaction txn : txns) {
            System.out.println("ID: " + txn.getId() +
                    " | Account ID: " + txn.getAccountId() +
                    " | Type: " + txn.getType() +
                    " | Amount: " + txn.getAmount() +
                    ((txn instanceof InterBankTransaction ibt) ? " | Receiver: " + ibt.getReceiverAccount() : ""));
        }
    }

    private static void deleteAccount() {
        try {
            System.out.print("Enter Account ID to delete: ");
            Long id = Long.parseLong(sc.nextLine());
            accountService.deleteAccount(id);
            System.out.println("Account deleted successfully!");
        } catch (Exception e) {
            System.out.println("Error deleting account: " + e.getMessage());
        }
    }

    private static void deleteTransaction() {
        try {
            System.out.print("Enter Transaction ID to delete: ");
            Long id = Long.parseLong(sc.nextLine());
            transactionService.deleteTransaction(id);
            System.out.println("Transaction deleted successfully!");
        } catch (Exception e) {
            System.out.println("Error deleting transaction: " + e.getMessage());
        }
    }
}

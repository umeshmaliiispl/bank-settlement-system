package com.iispl.utility;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import com.iispl.dao.AccountDaoImpl;
import com.iispl.dao.CustomerDAOImpl;
import com.iispl.entity.Account;
import com.iispl.entity.Customer;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;
import com.iispl.service.AccountService;
import com.iispl.service.AccountServiceImpl;
import com.iispl.service.CustomerService;
import com.iispl.service.CustomerServiceImpl;

/*
 * MainforAccountANDCustomer
 *
 * This is the main console application for Customer and Account Management.
 * It shows a menu and allows the user to perform selected
 * Read operations on Customer and Account.
 *
 * It uses CustomerService and AccountService for business logic and
 * CustomerDAOImpl and AccountDaoImpl for actual database operations.
 *
 * CUSTOMER READ OPERATIONS:
 *   1.  Get All Customers
 *   2.  Get Customer by Customer ID
 *   3.  Get Customer by Full Name
 *   4.  Get All Active Customers
 *   5.  Get All Verified Customers
 *
 * ACCOUNT READ OPERATIONS:
 *   6.  Get All Accounts
 *   7.  Get Account by Account Number
 *   8.  Get Accounts by Bank Name
 *   9.  Get Accounts by Account Type
 *   10. Get Accounts by Account Status
 *   11. Get Accounts by Customer ID
 */
public class MainforAccountANDCustomer {

    // ===========================================================
    // CONSTANTS
    // ===========================================================

    /*
     * Date and Time formatter.
     * Formats LocalDateTime to a clean readable format.
     * Example Output : 09 Apr 2026  02:27:54 PM
     *
     * Pattern breakdown:
     *   dd        → Day with leading zero        : 09
     *   MMM       → Month short name             : Apr
     *   yyyy      → Full year                    : 2026
     *   hh        → Hour in 12-hour format       : 02
     *   mm        → Minutes with leading zero    : 27
     *   ss        → Seconds with leading zero    : 54
     *   a         → AM / PM marker               : PM
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm:ss a");

    /*
     * Separator line used for consistent output formatting.
     */
    private static final String LINE =
        "  ──────────────────────────────────────────────────";

    /*
     * Separator line for section headers.
     */
    private static final String THICK_LINE =
        "  ══════════════════════════════════════════════════";


    // ===========================================================
    // SERVICE INSTANCES
    // DAOImpl is injected into ServiceImpl.
    // All database calls go through Service → DAO → Database.
    // ===========================================================

    private static final CustomerService customerService =
        new CustomerServiceImpl(new CustomerDAOImpl());

    private static final AccountService accountService =
        new AccountServiceImpl(new AccountDaoImpl());

    /*
     * Scanner reads user input from the console.
     */
    private static final Scanner scanner = new Scanner(System.in);


    // ===========================================================
    // MAIN METHOD
    // Shows the menu in a loop until user enters 0 to exit.
    // ===========================================================
    public static void main(String[] args) {

        printBanner();

        int choice;

        do {
            printMenu();
            System.out.print("  Enter Your Choice : ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("    Invalid input. Please enter a number.");
                choice = -1;
                continue;
            }

            switch (choice) {

                // CUSTOMER READ OPERATIONS
                case 1:  getAllCustomers();            break;
                case 2:  getCustomerByCustomerId();   break;
                case 3:  getCustomersByFullName();    break;
                case 4:  getActiveCustomers();        break;
                case 5:  getVerifiedCustomers();      break;

                // ACCOUNT READ OPERATIONS
                case 6:  getAllAccounts();             break;
                case 7:  getAccountByAccountNumber(); break;
                case 8:  getAccountsByBankName();     break;
                case 9:  getAccountsByAccountType();  break;
                case 10: getAccountsByAccountStatus(); break;
                case 11: getAccountsByCustomerId();   break;

                case 0:
                    System.out.println();
                    System.out.println(THICK_LINE);
                    System.out.println("      Thank You for using IISPL System.");
                    System.out.println("                  Goodbye !");
                    System.out.println(THICK_LINE);
                    System.out.println();
                    break;

                default:
                    System.out.println("    Invalid Choice. Please Try Again.");
            }

        } while (choice != 0);

        scanner.close();
    }


    // ===========================================================
    // BANNER
    // Printed once when application starts.
    // ===========================================================
    private static void printBanner() {
        System.out.println();
        System.out.println(THICK_LINE);
        System.out.println("       IISPL SETTLEMENT SYSTEM");
        System.out.println("       Customer and Account Management");
        System.out.println(THICK_LINE);
        System.out.println();
    }


    // ===========================================================
    // MENU
    // Prints all available options to the console.
    // ===========================================================
    private static void printMenu() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║     CUSTOMER AND ACCOUNT READ SYSTEM     ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║   CUSTOMER READ OPERATIONS               ║");
        System.out.println("  ║   1.  Get All Customers                  ║");
        System.out.println("  ║   2.  Get Customer by Customer ID        ║");
        System.out.println("  ║   3.  Get Customer by Full Name          ║");
        System.out.println("  ║   4.  Get All Active Customers           ║");
        System.out.println("  ║   5.  Get All Verified Customers         ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║   ACCOUNT READ OPERATIONS                ║");
        System.out.println("  ║   6.  Get All Accounts                   ║");
        System.out.println("  ║   7.  Get Account by Account Number      ║");
        System.out.println("  ║   8.  Get Accounts by Bank Name          ║");
        System.out.println("  ║   9.  Get Accounts by Account Type       ║");
        System.out.println("  ║   10. Get Accounts by Account Status     ║");
        System.out.println("  ║   11. Get Accounts by Customer ID        ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║   0.  Exit                               ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println();
    }


    // ===========================================================
    //  CUSTOMER READ OPERATIONS
    // ===========================================================


    // ===========================================================
    // 1. GET ALL CUSTOMERS
    // Calls service.getAllCustomers() which calls DAO.
    // DAO runs SELECT * FROM customer and returns all records.
    // ===========================================================
    private static void getAllCustomers() {

        printSectionHeader("ALL CUSTOMERS");

        /*
         * service.getAllCustomers() calls customerDAO.getAllCustomers()
         * which fetches all rows from the customer table.
         */
        List<Customer> customerList = customerService.getAllCustomers();

        if (customerList.isEmpty()) {
            System.out.println("    No Customers Found in Database.");
        } else {
            System.out.println("  Total Customers : " + customerList.size());
            System.out.println();
            for (Customer customer : customerList) {
                printCustomer(customer);
            }
        }
    }


    // ===========================================================
    // 2. GET CUSTOMER BY CUSTOMER ID
    // Takes the business Customer ID as input.
    // Calls service.getCustomerByCustomerId() which calls DAO.
    // DAO runs SELECT * FROM customer WHERE customer_id = ?
    // ===========================================================
    private static void getCustomerByCustomerId() {

        printSectionHeader("GET CUSTOMER BY CUSTOMER ID");

        System.out.print("  Enter Customer ID  (e.g. CID1001) : ");
        String customerId = scanner.nextLine().trim();

        /*
         * service.getCustomerByCustomerId() validates input is not blank
         * then calls customerDAO.getCustomerByCustomerId()
         * which queries the customer table using customer_id column.
         */
        Optional<Customer> result =
            customerService.getCustomerByCustomerId(customerId);

        if (result.isPresent()) {
            System.out.println();
//            printCustomer(result.get());
        } else {
            System.out.println("2 No Customer Found for Customer ID : "
                + customerId);
        }
    }


    // ===========================================================
    // 3. GET CUSTOMERS BY FULL NAME
    // Takes a name keyword and does a partial search.
    // Calls service.getCustomersByFullName() which calls DAO.
    // DAO runs SELECT * FROM customer WHERE LOWER(full_name) LIKE ?
    // Example: input "rahul" finds "Rahul Sharma", "Rahul Kumar"
    // ===========================================================
    private static void getCustomersByFullName() {

        printSectionHeader("GET CUSTOMERS BY FULL NAME");

        System.out.print("  Enter Full Name to Search  (e.g. Rahul) : ");
        String fullName = scanner.nextLine().trim();

        /*
         * service.getCustomersByFullName() validates minimum 2 characters
         * then calls customerDAO.getCustomersByFullName()
         * which does a case insensitive partial search on full_name column.
         */
        List<Customer> customerList =
            customerService.getCustomersByFullName(fullName);

        if (customerList.isEmpty()) {
            System.out.println("   No Customers Found for Name : " + fullName);
        } else {
            System.out.println("  Total Customers Found : " + customerList.size());
            System.out.println();
            for (Customer customer : customerList) {
                printCustomer(customer);
            }
        }
    }


    // ===========================================================
    // 4. GET ALL ACTIVE CUSTOMERS
    // Calls service.getActiveCustomers()
    // DAO uses SQL WHERE UPPER(customer_status) = 'ACTIVE'
    // ===========================================================
    private static void getActiveCustomers() {

        printSectionHeader("ALL ACTIVE CUSTOMERS");

        /*
         * service.getActiveCustomers() calls customerDAO.getAllActiveCustomers()
         * which queries customer table filtering by customer_status = ACTIVE.
         */
        List<Customer> customerList = customerService.getActiveCustomers();

////        if (customerList.isEmpty()) {
////            System.out.println("  ⚠  No Active Customers Found.");
////        } else {
////            System.out.println("  Total Active Customers : " + customerList.size());
////            System.out.println();
//            for (Customer customer : customerList) {
//                printCustomer(customer);
//            }
//        }
   }


    // ===========================================================
    // 5. GET ALL KYC VERIFIED CUSTOMERS
    // Calls service.getVerifiedCustomers()
    // DAO uses SQL WHERE UPPER(kyc_status) = 'VERIFIED'
    // ===========================================================
    private static void getVerifiedCustomers() {

        printSectionHeader("ALL KYC VERIFIED CUSTOMERS");

        /*
         * service.getVerifiedCustomers() calls customerDAO.getAllVerifiedCustomers()
         * which queries customer table filtering by kyc_status = VERIFIED.
         */
        List<Customer> customerList = customerService.getVerifiedCustomers();


    }


    // ===========================================================
    //  ACCOUNT READ OPERATIONS
    // ===========================================================


    // ===========================================================
    // 6. GET ALL ACCOUNTS
    // Calls service.getAllAccounts() which calls DAO.
    // DAO runs SELECT * FROM account and returns all records.
    // ===========================================================
    private static void getAllAccounts() {

        printSectionHeader("ALL ACCOUNTS");

        /*
         * service.getAllAccounts() calls accountDAO.findAll()
         * which fetches all rows from the account table.
         */
        List<Account> accountList = accountService.getAllAccounts();

        if (accountList.isEmpty()) {
            System.out.println("   No Accounts Found in Database.");
        } else {
            System.out.println("  Total Accounts : " + accountList.size());
            System.out.println();
            for (Account account : accountList) {
                printAccount(account);
            }
        }
    }


    // ===========================================================
    // 7. GET ACCOUNT BY ACCOUNT NUMBER
    // Takes the business Account Number as input.
    // Calls service.getAccountByAccountNumber() which calls DAO.
    // DAO runs SELECT * FROM account WHERE account_number = ?
    // ===========================================================
    private static void getAccountByAccountNumber() {

        printSectionHeader("GET ACCOUNT BY ACCOUNT NUMBER");

        System.out.print("  Enter Account Number  (e.g. ACC10001) : ");
        String accountNumber = scanner.nextLine().trim();

        /*
         * service.getAccountByAccountNumber() validates input is not blank
         * then calls accountDAO.findByAccountNumber()
         * which queries the account table using account_number column.
         */
        try {
            Optional<Account> result =
                accountService.getAccountByAccountNumber(accountNumber);

            if (result.isPresent()) {
                System.out.println("  ✔  Account Found :");
                System.out.println();
                printAccount(result.get());
            } else {
                System.out.println("  ⚠  No Account Found for Account Number : "
                    + accountNumber);
            }

        } catch (IllegalArgumentException e) {
            System.out.println("  ✖  Input Error : " + e.getMessage());
        }
    }


    // ===========================================================
    // 8. GET ACCOUNTS BY BANK NAME
    // Takes a bank name keyword and does a partial search.
    // Calls service.getAccountsByBankName() which calls DAO.
    // DAO runs SELECT * FROM account WHERE LOWER(bank_name) LIKE ?
    // Example: input "State" finds "State Bank of India"
    // ===========================================================
    private static void getAccountsByBankName() {

        printSectionHeader("GET ACCOUNTS BY BANK NAME");

        System.out.print("  Enter Bank Name to Search"
            + "  (e.g. State Bank / HDFC / ICICI) : ");
        String bankName = scanner.nextLine().trim();

        /*
         * service.getAccountsByBankName() validates minimum 2 characters
         * then calls accountDAO.findByBankName()
         * which does a case insensitive partial search on bank_name column.
         */
        try {
            List<Account> accountList =
                accountService.getAccountsByBankName(bankName);

            if (accountList.isEmpty()) {
                System.out.println("    No Accounts Found for Bank : " + bankName);
            } else {
                System.out.println("  Total Accounts Found : " + accountList.size());
                System.out.println();
                for (Account account : accountList) {
                    printAccount(account);
                }
            }

        } catch (IllegalArgumentException e) {
            System.out.println("  ✖  Input Error : " + e.getMessage());
        }
    }


    // ===========================================================
    // 9. GET ACCOUNTS BY ACCOUNT TYPE
    // Shows available types for selection.
    // Calls service.getAccountsByAccountType() which calls DAO.
    // DAO runs SELECT * FROM account WHERE account_type = ?
    // Valid Types : SAVINGS, CURRENT, FIXED_DEPOSIT, RECURRING_DEPOSIT
    // ===========================================================
    private static void getAccountsByAccountType() {

        printSectionHeader("GET ACCOUNTS BY ACCOUNT TYPE");

        System.out.println("  Available Account Types :");
        System.out.println("    1. SAVINGS");
        System.out.println("    2. CURRENT");
        System.out.println("    3. FIXED_DEPOSIT");
        System.out.println("    4. RECURRING_DEPOSIT");
        System.out.println();
        System.out.print("  Enter Account Type  (e.g. SAVINGS) : ");
        String typeInput = scanner.nextLine().trim().toUpperCase();

        AccountType accountType;

        /*
         * Parse the input string to AccountType enum.
         * If input does not match any valid enum value,
         * show error and return to menu.
         */
        try {
            accountType = AccountType.valueOf(typeInput);
        } catch (IllegalArgumentException e) {
            System.out.println("  ✖  Invalid Account Type : " + typeInput);
            System.out.println("     Valid Types : SAVINGS, CURRENT,"
                + " FIXED_DEPOSIT, RECURRING_DEPOSIT");
            return;
        }

        /*
         * service.getAccountsByAccountType() validates AccountType is not null
         * then calls accountDAO.findByAccountType()
         * which queries the account table using account_type column.
         */
        List<Account> accountList =
            accountService.getAccountsByAccountType(accountType);

        if (accountList.isEmpty()) {
            System.out.println("    No Accounts Found for Type : " + accountType);
        } else {
            System.out.println("  Total Accounts Found : " + accountList.size());
            System.out.println();
            for (Account account : accountList) {
                printAccount(account);
            }
        }
    }


    // ===========================================================
    // 10. GET ACCOUNTS BY ACCOUNT STATUS
    // Shows available statuses for selection.
    // Calls service.getAccountsByAccountStatus() which calls DAO.
    // DAO runs SELECT * FROM account WHERE account_status = ?
    // Valid Statuses : ACTIVE, INACTIVE, FROZEN, CLOSED
    // ===========================================================
    private static void getAccountsByAccountStatus() {

        printSectionHeader("GET ACCOUNTS BY ACCOUNT STATUS");

        System.out.println("  Available Account Statuses :");
        System.out.println("    1. ACTIVE");
        System.out.println("    2. INACTIVE");
        System.out.println("    3. FROZEN");
        System.out.println("    4. CLOSED");
        System.out.println();
        System.out.print("  Enter Account Status  (e.g. ACTIVE) : ");
        String statusInput = scanner.nextLine().trim().toUpperCase();

        AccountStatus accountStatus;

        /*
         * Parse the input string to AccountStatus enum.
         * If input does not match any valid enum value,
         * show error and return to menu.
         */
        try {
            accountStatus = AccountStatus.valueOf(statusInput);
        } catch (IllegalArgumentException e) {
            System.out.println("    Invalid Account Status : " + statusInput);
            System.out.println("     Valid Statuses : ACTIVE, INACTIVE, FROZEN, CLOSED");
            return;
        }

        /*
         * service.getAccountsByAccountStatus() validates AccountStatus is not null
         * then calls accountDAO.findByAccountStatus()
         * which queries the account table using account_status column.
         */
        List<Account> accountList =
            accountService.getAccountsByAccountStatus(accountStatus);

        if (accountList.isEmpty()) {
            System.out.println("    No Accounts Found for Status : " + accountStatus);
        } else {
            System.out.println("  Total Accounts Found : " + accountList.size());
            System.out.println();
            for (Account account : accountList) {
                printAccount(account);
            }
        }
    }


    // ===========================================================
    // 11. GET ACCOUNTS BY CUSTOMER ID
    // Takes a Customer ID and returns all accounts linked to it.
    // Calls service.getAccountsByCustomerId() which calls DAO.
    // DAO runs SELECT * FROM account WHERE customer_id = ?
    // One customer can have multiple accounts.
    // ===========================================================
    private static void getAccountsByCustomerId() {

        printSectionHeader("GET ACCOUNTS BY CUSTOMER ID");

        System.out.print("  Enter Customer ID  (e.g. CID1001) : ");
        String customerId = scanner.nextLine().trim();

        /*
         * service.getAccountsByCustomerId() validates input is not blank
         * then calls accountDAO.findByCustomerId()
         * which queries the account table using customer_id column.
         * One customer can have multiple accounts so List is returned.
         */
        try {
            List<Account> accountList =
                accountService.getAccountsByCustomerId(customerId);

            if (accountList.isEmpty()) {
                System.out.println("    No Accounts Found for Customer ID : "
                    + customerId);
            } else {
                System.out.println("  Total Accounts for Customer [ "
                    + customerId + " ] : " + accountList.size());
                System.out.println();
                for (Account account : accountList) {
                    printAccount(account);
                }
            }

        } catch (IllegalArgumentException e) {
            System.out.println("    Input Error : " + e.getMessage());
        }
    }


    // ===========================================================
    // PRINT HELPERS
    // ===========================================================

    /*
     * Prints a formatted section header for each operation.
     */
    private static void printSectionHeader(String title) {
        System.out.println();
        System.out.println(THICK_LINE);
        System.out.println("    " + title);
        System.out.println(THICK_LINE);
        System.out.println();
    }

    /*
     * Formats a LocalDateTime value to readable string.
     * Returns "N/A" if value is null.
     * Example Output : 09 Apr 2026  02:27:54 PM
     */
    private static String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /*
     * Prints all fields of a Customer object in a clean readable format.
     * Used by all Customer READ operation methods.
     */
    private static void printCustomer(Customer customer) {
        System.out.println(LINE);
        System.out.println("   Customer ID      :  " + customer.getCustomerId());
        System.out.println("   Full Name        :  " + customer.getFullName());
        System.out.println("   KYC Status       :  " + customer.getKycStatus());
        System.out.println("   Customer Status  :  " + customer.getCustomerStatus());
        System.out.println("   Created By       :  " + customer.getCreatedBy());
        System.out.println("   Created At       :  " + formatDateTime(customer.getCreatedAt()));
        System.out.println("   Version          :  " + customer.getVersion());
        System.out.println(LINE);
        System.out.println();
    }

    /*
     * Prints all fields of an Account object in a clean readable format.
     * Used by all Account READ operation methods.
     */
    private static void printAccount(Account account) {
        System.out.println(LINE);
        System.out.println("   Account Number   :  " + account.getAccountNumber());
        System.out.println("   Bank Name        :  " + account.getBankName());
        System.out.println("   IFSC Code        :  " + account.getIfscCode());
        System.out.println("   Customer ID      :  " + account.getCustomerId());
        System.out.println("   Account Type     :  " + account.getAccountType());
        System.out.println("   Balance          :  " + account.getBalance());
        System.out.println("   Currency         :  " + account.getCurrency());
        System.out.println("   Account Status   :  " + account.getAccountStatus());
        System.out.println("   Created By       :  " + account.getCreatedBy());
        System.out.println("   Created At       :  " + formatDateTime(account.getCreatedAt()));
        System.out.println(LINE);
        System.out.println();
    }
}
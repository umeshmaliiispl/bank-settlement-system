package com.iispl.main;

import com.iispl.entity.Account;
import com.iispl.entity.Customer;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;
import com.iispl.service.AccountService;
import com.iispl.service.AccountServiceImpl;
import com.iispl.service.CustomerService;
import com.iispl.service.CustomerServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * MainApp - Entry point for the Settlement System.
 *
 * Demonstrates ALL operations:
 *
 * CUSTOMER OPERATIONS
 *    1.  Register Customer
 *    2.  Get All Customers
 *    3.  Get Customer by ID
 *    4.  Get Customer by Customer ID
 *    5.  Get Customers by Full Name
 *    6.  Get All Active Customers
 *    7.  Get All Verified Customers
 *    8.  Update Customer
 *    9.  Update KYC Status
 *    10. Activate Customer
 *    11. Deactivate Customer
 *    12. Remove Customer
 *
 * ACCOUNT OPERATIONS
 *    1.  Create Account
 *    2.  Get All Accounts
 *    3.  Get Account by ID
 *    4.  Get Account by Account Number
 *    5.  Get Accounts by Bank Name
 *    6.  Get Accounts by Account Type
 *    7.  Get Accounts by Account Status
 *    8.  Get Accounts by Customer ID
 *    9.  Update Account
 *    10. Credit Amount
 *    11. Debit Amount
 *    12. Delete Account
 */
public class MainApp1 {

    // Service References
    private static final CustomerService customerService = new CustomerServiceImpl();
    private static final AccountService  accountService  = new AccountServiceImpl();

    // Main Entry Point
    public static void main(String[] args) {

        printBanner();

        // CUSTOMER OPERATIONS
        runCustomerOperations();

        // ACCOUNT OPERATIONS
        runAccountOperations();

        printFooter();
    }


    // =============================================================================
    //  CUSTOMER OPERATIONS
    // =============================================================================

    private static void runCustomerOperations() {

        printSectionHeader("CUSTOMER OPERATIONS");

//        registerCustomers();
        getAllCustomers();
//        getCustomerById();
        getCustomerByCustomerId();
        getCustomersByFullName();
        getAllActiveCustomers();
        getAllVerifiedCustomers();
       
    }

    
    private static void runAccountOperations() {

        printSectionHeader("ACCOUNT OPERATIONS");

//        createAccounts();
        getAllAccounts();
        
        getAccountByAccountNumber();
        getAccountsByBankName();
        getAccountsByAccountType();
        getAccountsByAccountStatus();
        getAccountsByCustomerId();
        
    }
    
    
6
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    // -----------------------------------------------------------------------------
    // 1. REGISTER CUSTOMERS
    // -----------------------------------------------------------------------------

//    private static void registerCustomers() {
//
//        printOperationHeader("1. REGISTER CUSTOMERS");
//
//        // Customer 1 - ACTIVE + VERIFIED
//        Customer c1 = new Customer(
//            "ADMIN",
//            "CID1001",
//            "Rahul Sharma",
//            "VERIFIED",
//            "ACTIVE"
//        );
//
//        // Customer 2 - ACTIVE + PENDING
//        Customer c2 = new Customer(
//            "ADMIN",
//            "CID1002",
//            "Priya Mehta",
//            "PENDING",
//            "ACTIVE"
//        );
//
//        // Customer 3 - INACTIVE + VERIFIED
//        Customer c3 = new Customer(
//            "ADMIN",
//            "CID1003",
//            "Amit Kumar",
//            "VERIFIED",
//            "INACTIVE"
//        );
//
//        // Customer 4 - ACTIVE + PENDING
//        Customer c4 = new Customer(
//            "ADMIN",
//            "CID1004",
//            "Sneha Sharma",
//            "PENDING",
//            "ACTIVE"
//        );
//
//        // Customer 5 - ACTIVE + VERIFIED
//        Customer c5 = new Customer(
//            "ADMIN",
//            "CID1005",
//            "Vikram Singh",
//            "VERIFIED",
//            "ACTIVE"
//        );
//
//        // Register all customers
//        boolean r1 = customerService.registerCustomer(c1);
//        boolean r2 = customerService.registerCustomer(c2);
//        boolean r3 = customerService.registerCustomer(c3);
//        boolean r4 = customerService.registerCustomer(c4);
//        boolean r5 = customerService.registerCustomer(c5);
//
//        // Registration Summary
//        printSubHeader("Registration Summary");
//        System.out.println("   CID1001 - Rahul Sharma  : " + (r1 ? "SUCCESS" : "FAILED"));
//        System.out.println("   CID1002 - Priya Mehta   : " + (r2 ? "SUCCESS" : "FAILED"));
//        System.out.println("   CID1003 - Amit Kumar    : " + (r3 ? "SUCCESS" : "FAILED"));
//        System.out.println("   CID1004 - Sneha Sharma  : " + (r4 ? "SUCCESS" : "FAILED"));
//        System.out.println("   CID1005 - Vikram Singh  : " + (r5 ? "SUCCESS" : "FAILED"));
//
//        // Test - Duplicate Customer ID
//        printSubHeader("Test - Duplicate Customer ID");
//        Customer duplicate = new Customer("ADMIN", "CID1001", "Duplicate Test", "PENDING", "ACTIVE");
//        boolean rDup = customerService.registerCustomer(duplicate);
//        System.out.println("   Duplicate CID1001 Result : " + (rDup ? "SUCCESS" : "REJECTED (Expected)"));
//
//        // Test - Null Customer Object
//        printSubHeader("Test - Null Customer Object");
//        boolean rNull = customerService.registerCustomer(null);
//        System.out.println("   Null Customer Result     : " + (rNull ? "SUCCESS" : "REJECTED (Expected)"));
//
//        // Test - Missing Customer ID
//        printSubHeader("Test - Missing Customer ID");
//        Customer noId = new Customer("ADMIN", "", "No ID Test", "PENDING", "ACTIVE");
//        boolean rNoId = customerService.registerCustomer(noId);
//        System.out.println("   Missing ID Result        : " + (rNoId ? "SUCCESS" : "REJECTED (Expected)"));
//    }
//

    // -----------------------------------------------------------------------------
    // 2. GET ALL CUSTOMERS
    // -----------------------------------------------------------------------------

    private static void getAllCustomers() {

        printOperationHeader("2. GET ALL CUSTOMERS");

        List<Customer> customers = customerService.getAllCustomers();

        if (customers.isEmpty()) {
            System.out.println("   No customers found.");
            return;
        }

        System.out.println("   Total Customers : " + customers.size());
        System.out.println();
        System.out.printf("   %-12s %-22s %-12s %-12s%n",
            "Customer ID", "Full Name", "KYC Status", "Status");
        System.out.println("   " + "-".repeat(62));

        for (Customer c : customers) {
            System.out.printf("   %-12s %-22s %-12s %-12s%n",
                c.getCustomerId(),
                c.getFullName(),
                c.getKycStatus(),
                c.getCustomerStatus());
        }
    }


    // -----------------------------------------------------------------------------
    // 3. GET CUSTOMER BY ID (Primary Key)
    // -----------------------------------------------------------------------------

    private static void getCustomerById() {

        printOperationHeader("3. GET CUSTOMER BY ID (Primary Key)");

        // Test with valid primary key
        printSubHeader("Search ID: 1");
        Optional<Customer> customer = customerService.getCustomerById("1");

        if (customer.isPresent()) {
            printCustomerDetails(customer.get());
        } else {
            System.out.println("   No Customer found with ID: 1");
        }

        // Test with non-existent ID
        printSubHeader("Search Non-Existent ID: 9999");
        Optional<Customer> notFound = customerService.getCustomerById("9999");
        if (notFound.isEmpty()) {
            System.out.println("   No Customer found with ID: 9999 (Expected)");
        }
    }


    // -----------------------------------------------------------------------------
    // 4. GET CUSTOMER BY CUSTOMER ID
    // -----------------------------------------------------------------------------

    private static void getCustomerByCustomerId() {

        printOperationHeader("4. GET CUSTOMER BY CUSTOMER ID");

        // Test 1 - Valid Customer ID
        printSubHeader("Search Customer ID: CID1001");
        Optional<Customer> c1 = customerService.getCustomerByCustomerId("CID1001");
        if (c1.isPresent()) {
            printCustomerDetails(c1.get());
        } else {
            System.out.println("   Customer not found.");
        }

        // Test 2 - Another Valid Customer ID
        printSubHeader("Search Customer ID: CID1003");
        Optional<Customer> c2 = customerService.getCustomerByCustomerId("CID1003");
        if (c2.isPresent()) {
            printCustomerDetails(c2.get());
        } else {
            System.out.println("   Customer not found.");
        }

        // Test 3 - Non-existent Customer ID
        printSubHeader("Search Non-Existent ID: CID9999");
        Optional<Customer> c3 = customerService.getCustomerByCustomerId("CID9999");
        if (c3.isEmpty()) {
            System.out.println("   No Customer found with ID: CID9999 (Expected)");
        }

        // Test 4 - Null Customer ID
        printSubHeader("Search Null Customer ID");
        Optional<Customer> c4 = customerService.getCustomerByCustomerId(null);
        if (c4.isEmpty()) {
            System.out.println("   Null Customer ID rejected (Expected)");
        }
    }


    // -----------------------------------------------------------------------------
    // 5. GET CUSTOMERS BY FULL NAME
    // -----------------------------------------------------------------------------

    private static void getCustomersByFullName() {

        printOperationHeader("5. GET CUSTOMERS BY FULL NAME");

        // Test 1 - Partial name search - "sharma"
        printSubHeader("Search: sharma");
        List<Customer> sharma = customerService.getCustomersByFullName("sharma");
        printCustomerList(sharma);

        // Test 2 - Partial name search - "Priya"
        printSubHeader("Search: Priya");
        List<Customer> priya = customerService.getCustomersByFullName("Priya");
        printCustomerList(priya);

        // Test 3 - Full name search
        printSubHeader("Search: Vikram Singh");
        List<Customer> vikram = customerService.getCustomersByFullName("Vikram Singh");
        printCustomerList(vikram);

        // Test 4 - No match
        printSubHeader("Search: NoMatch");
        List<Customer> noMatch = customerService.getCustomersByFullName("NoMatch");
        if (noMatch.isEmpty()) {
            System.out.println("   No customers found matching NoMatch (Expected)");
        }

        // Test 5 - Single character - too short
        printSubHeader("Search: A (Too Short)");
        List<Customer> tooShort = customerService.getCustomersByFullName("A");
        if (tooShort.isEmpty()) {
            System.out.println("   Search keyword too short - rejected (Expected)");
        }
    }


    // -----------------------------------------------------------------------------
    // 6. GET ALL ACTIVE CUSTOMERS
    // -----------------------------------------------------------------------------

    private static void getAllActiveCustomers() {

        printOperationHeader("6. GET ALL ACTIVE CUSTOMERS");

        List<Customer> activeCustomers = customerService.getActiveCustomers();

        if (activeCustomers.isEmpty()) {
            System.out.println("   No active customers found.");
            return;
        }

        System.out.println("   Total Active Customers: " + activeCustomers.size());
        printCustomerList(activeCustomers);
    }


    // -----------------------------------------------------------------------------
    // 7. GET ALL VERIFIED CUSTOMERS
    // -----------------------------------------------------------------------------

    private static void getAllVerifiedCustomers() {

        printOperationHeader("7. GET ALL VERIFIED CUSTOMERS");

        List<Customer> verifiedCustomers = customerService.getVerifiedCustomers();

        if (verifiedCustomers.isEmpty()) {
            System.out.println("   No KYC verified customers found.");
            return;
        }

        System.out.println("   Total Verified Customers: " + verifiedCustomers.size());
        printCustomerList(verifiedCustomers);
    }


    // -----------------------------------------------------------------------------
    // 8. UPDATE CUSTOMER
    // -----------------------------------------------------------------------------

    private static void updateCustomer() {

        printOperationHeader("8. UPDATE CUSTOMER");

        // Test 1 - Valid update
        printSubHeader("Update CID1002 - Priya Mehta");

        Customer updated = new Customer(
            "ADMIN",
            "CID1002",
            "Priya Mehta Kapoor",
            "VERIFIED",
            "ACTIVE"
        );

        boolean result = customerService.updateCustomer(updated);
        System.out.println("   Update Result: " + (result ? "UPDATED" : "FAILED"));

        // Verify the update
        Optional<Customer> verify = customerService.getCustomerByCustomerId("CID1002");
        verify.ifPresent(c -> {
            System.out.println("   Verified Updated Name : " + c.getFullName());
            System.out.println("   Verified Updated KYC  : " + c.getKycStatus());
        });

        // Test 2 - Non-existent customer
        printSubHeader("Update Non-Existent Customer: CID9999");
        Customer notExist = new Customer("ADMIN", "CID9999", "Ghost User", "PENDING", "ACTIVE");
        boolean r2 = customerService.updateCustomer(notExist);
        System.out.println("   Result: " + (r2 ? "UPDATED" : "NOT FOUND (Expected)"));

        // Test 3 - Invalid KYC Status
        printSubHeader("Update with Invalid KYC Status");
        Customer invalidKyc = new Customer("ADMIN", "CID1001", "Rahul Sharma", "UNKNOWN", "ACTIVE");
        boolean r3 = customerService.updateCustomer(invalidKyc);
        System.out.println("   Result: " + (r3 ? "UPDATED" : "REJECTED (Expected)"));
    }


    // -----------------------------------------------------------------------------
    // 9. UPDATE KYC STATUS
    // -----------------------------------------------------------------------------

    private static void updateKycStatus() {

        printOperationHeader("9. UPDATE KYC STATUS");

        // Test 1 - PENDING to VERIFIED
        printSubHeader("CID1004 - PENDING to VERIFIED");
        boolean r1 = customerService.updateKycStatus("CID1004", "VERIFIED");
        System.out.println("   Result: " + (r1 ? "KYC UPDATED" : "FAILED"));

        // Verify
        customerService.getCustomerByCustomerId("CID1004")
            .ifPresent(c -> System.out.println(
                "   Current KYC Status: " + c.getKycStatus()
            ));

        // Test 2 - Invalid KYC Value
        printSubHeader("Invalid KYC Value: APPROVED");
        boolean r2 = customerService.updateKycStatus("CID1001", "APPROVED");
        System.out.println("   Result: " + (r2 ? "UPDATED" : "REJECTED (Expected)"));

        // Test 3 - Non-existent customer
        printSubHeader("Non-Existent Customer: CID9999");
        boolean r3 = customerService.updateKycStatus("CID9999", "VERIFIED");
        System.out.println("   Result: " + (r3 ? "UPDATED" : "NOT FOUND (Expected)"));
    }


    // -----------------------------------------------------------------------------
    // 10. ACTIVATE CUSTOMER
    // -----------------------------------------------------------------------------

    private static void activateCustomer() {

        printOperationHeader("10. ACTIVATE CUSTOMER");

        // Test 1 - Activate INACTIVE customer CID1003
        printSubHeader("Activate CID1003 (Currently INACTIVE)");
        boolean r1 = customerService.activateCustomer("CID1003");
        System.out.println("   Result: " + (r1 ? "ACTIVATED" : "FAILED"));

        // Verify
        customerService.getCustomerByCustomerId("CID1003")
            .ifPresent(c -> System.out.println(
                "   Current Status: " + c.getCustomerStatus()
            ));

        // Test 2 - Already ACTIVE customer
        printSubHeader("Activate CID1001 (Already ACTIVE)");
        boolean r2 = customerService.activateCustomer("CID1001");
        System.out.println("   Result: " + (r2 ? "ACTIVATED" : "ALREADY ACTIVE (Expected)"));

        // Test 3 - Non-existent customer
        printSubHeader("Activate Non-Existent: CID9999");
        boolean r3 = customerService.activateCustomer("CID9999");
        System.out.println("   Result: " + (r3 ? "ACTIVATED" : "NOT FOUND (Expected)"));
    }


    // -----------------------------------------------------------------------------
    // 11. DEACTIVATE CUSTOMER
    // -----------------------------------------------------------------------------

    private static void deactivateCustomer() {

        printOperationHeader("11. DEACTIVATE CUSTOMER");

        // Test 1 - Deactivate ACTIVE customer CID1005
        printSubHeader("Deactivate CID1005 (Currently ACTIVE)");
        boolean r1 = customerService.deactivateCustomer("CID1005");
        System.out.println("   Result: " + (r1 ? "DEACTIVATED" : "FAILED"));

        // Verify
        customerService.getCustomerByCustomerId("CID1005")
            .ifPresent(c -> System.out.println(
                "   Current Status: " + c.getCustomerStatus()
            ));

        // Test 2 - Already INACTIVE customer
        printSubHeader("Deactivate CID1003 (Check Current Status)");
        boolean r2 = customerService.deactivateCustomer("CID1003");
        System.out.println("   Result: " + (r2 ? "DEACTIVATED" : "CHECK STATUS"));

        // Test 3 - Non-existent customer
        printSubHeader("Deactivate Non-Existent: CID9999");
        boolean r3 = customerService.deactivateCustomer("CID9999");
        System.out.println("   Result: " + (r3 ? "DEACTIVATED" : "NOT FOUND (Expected)"));
    }


    // -----------------------------------------------------------------------------
    // 12. REMOVE CUSTOMER
    // -----------------------------------------------------------------------------

    private static void removeCustomer() {

        printOperationHeader("12. REMOVE CUSTOMER");

        // Register a temp customer to delete
        printSubHeader("Register Temp Customer: CID9000");
        Customer temp = new Customer(
            "ADMIN", "CID9000", "Temp Delete User", "PENDING", "ACTIVE"
        );
        customerService.registerCustomer(temp);

        // Delete the temp customer
        printSubHeader("Delete CID9000");
        boolean r1 = customerService.removeCustomer("CID9000");
        System.out.println("   Delete Result: " + (r1 ? "DELETED" : "FAILED"));

        // Verify deletion
        Optional<Customer> verify = customerService.getCustomerByCustomerId("CID9000");
        System.out.println("   After Delete Search: "
            + (verify.isEmpty() ? "NOT FOUND (Expected)" : "STILL EXISTS"));

        // Test - Delete non-existent customer
        printSubHeader("Delete Non-Existent: CID9999");
        boolean r2 = customerService.removeCustomer("CID9999");
        System.out.println("   Result: " + (r2 ? "DELETED" : "NOT FOUND (Expected)"));

        // Test - Delete with null ID
        printSubHeader("Delete with Null ID");
        boolean r3 = customerService.removeCustomer(null);
        System.out.println("   Result: " + (r3 ? "DELETED" : "REJECTED (Expected)"));
    }


    // =============================================================================
    //  ACCOUNT OPERATIONS
    // =============================================================================

    private static void runAccountOperations() {

        printSectionHeader("ACCOUNT OPERATIONS");

//        createAccounts();
        getAllAccounts();
        
        getAccountByAccountNumber();
        getAccountsByBankName();
        getAccountsByAccountType();
        getAccountsByAccountStatus();
        getAccountsByCustomerId();
        
    }


    // -----------------------------------------------------------------------------
    // 1. CREATE ACCOUNTS
    // -----------------------------------------------------------------------------

//    private static void createAccounts() {
//
//        printOperationHeader("1. CREATE ACCOUNTS");
//
//        // Account 1 - CID1001, SBI, SAVINGS, ACTIVE
//        Account a1 = new Account(
//            "ADMIN",
//            "ACC10001",
//            "SBIN0001234",
//            "State Bank of India",
//            "CID1001",
//            AccountType.SAVINGS,
//            new BigDecimal("50000.00"),
//            "INR",
//            AccountStatus.ACTIVE
//        );
//
//        // Account 2 - CID1001, HDFC, CURRENT, ACTIVE
//        Account a2 = new Account(
//            "ADMIN",
//            "ACC10002",
//            "HDFC0001234",
//            "HDFC Bank",
//            "CID1001",
//            AccountType.CURRENT,
//            new BigDecimal("150000.00"),
//            "INR",
//            AccountStatus.ACTIVE
//        );
//
//        // Account 3 - CID1002, ICICI, SAVINGS, ACTIVE
//        Account a3 = new Account(
//            "ADMIN",
//            "ACC10003",
//            "ICIC0001234",
//            "ICICI Bank",
//            "CID1002",
//            AccountType.SAVINGS,
//            new BigDecimal("75000.00"),
//            "INR",
//            AccountStatus.ACTIVE
//        );
//
//        // Account 4 - CID1003, Axis, FIXED_DEPOSIT, FROZEN
//        Account a4 = new Account(
//            "ADMIN",
//            "ACC10004",
//            "UTIB0001234",
//            "Axis Bank",
//            "CID1003",
//            AccountType.FIXED_DEPOSIT,
//            new BigDecimal("200000.00"),
//            "INR",
//            AccountStatus.FROZEN
//        );
//
//        // Account 5 - CID1004, SBI, SAVINGS, INACTIVE
//        Account a5 = new Account(
//            "ADMIN",
//            "ACC10005",
//            "SBIN0005678",
//            "State Bank of India",
//            "CID1004",
//            AccountType.SAVINGS,
//            new BigDecimal("10000.00"),
//            "INR",
//            AccountStatus.INACTIVE
//        );
//
//        // Account 6 - CID1005, HDFC, RECURRING_DEPOSIT, ACTIVE
//        Account a6 = new Account(
//            "ADMIN",
//            "ACC10006",
//            "HDFC0005678",
//            "HDFC Bank",
//            "CID1005",
//            AccountType.RECURRING_DEPOSIT,
//            new BigDecimal("30000.00"),
//            "INR",
//            AccountStatus.ACTIVE
//        );
//
//        // Create all accounts
//        accountService.createAccount(a1);
//        accountService.createAccount(a2);
//        accountService.createAccount(a3);
//        accountService.createAccount(a4);
//        accountService.createAccount(a5);
//        accountService.createAccount(a6);
//
//        // Summary
//        printSubHeader("Creation Summary");
//        System.out.println("   ACC10001 - SBI   SAVINGS           CID1001 : SUCCESS");
//        System.out.println("   ACC10002 - HDFC  CURRENT           CID1001 : SUCCESS");
//        System.out.println("   ACC10003 - ICICI SAVINGS           CID1002 : SUCCESS");
//        System.out.println("   ACC10004 - Axis  FIXED_DEPOSIT     CID1003 : SUCCESS");
//        System.out.println("   ACC10005 - SBI   SAVINGS           CID1004 : SUCCESS");
//        System.out.println("   ACC10006 - HDFC  RECURRING_DEPOSIT CID1005 : SUCCESS");
//
//        // Test - Duplicate account number
//        printSubHeader("Test - Duplicate Account Number: ACC10001");
//        try {
//            Account dup = new Account(
//                "ADMIN", "ACC10001", "SBIN0001234",
//                "SBI", "CID1001", AccountType.SAVINGS,
//                new BigDecimal("1000.00"), "INR", AccountStatus.ACTIVE
//            );
//            accountService.createAccount(dup);
//        } catch (IllegalArgumentException e) {
//            System.out.println("   Duplicate Rejected (Expected): " + e.getMessage());
//        }
//
//        // Test - Negative balance
//        printSubHeader("Test - Negative Balance");
//        try {
//            Account negBal = new Account(
//                "ADMIN", "ACC99999", "SBIN0000001",
//                "SBI", "CID1001", AccountType.SAVINGS,
//                new BigDecimal("-5000.00"), "INR", AccountStatus.ACTIVE
//            );
//            accountService.createAccount(negBal);
//        } catch (IllegalArgumentException e) {
//            System.out.println("   Negative Balance Rejected (Expected): " + e.getMessage());
//        }
//
//        // Test - Null account
//        printSubHeader("Test - Null Account");
//        try {
//            accountService.createAccount(null);
//        } catch (IllegalArgumentException e) {
//            System.out.println("   Null Account Rejected (Expected): " + e.getMessage());
//        }
//    }


    // -----------------------------------------------------------------------------
    // 2. GET ALL ACCOUNTS
    // -----------------------------------------------------------------------------

    private static void getAllAccounts() {

        printOperationHeader("2. GET ALL ACCOUNTS");

        List<Account> accounts = accountService.getAllAccounts();

        if (accounts.isEmpty()) {
            System.out.println("   No accounts found.");
            return;
        }

        System.out.println("   Total Accounts: " + accounts.size());
        System.out.println();
        System.out.printf("   %-12s %-25s %-20s %-15s %-12s%n",
            "Acc Number", "Bank Name", "Type", "Balance", "Status");
        System.out.println("   " + "-".repeat(88));

        for (Account a : accounts) {
            System.out.printf("   %-12s %-25s %-20s %-15s %-12s%n",
                a.getAccountNumber(),
                a.getBankName(),
                a.getAccountType(),
                a.getBalance(),
                a.getAccountStatus());
        }
    }


    // -----------------------------------------------------------------------------
    // 3. GET ACCOUNT BY ID
    // -----------------------------------------------------------------------------

    private static void getAccountById() {

        printOperationHeader("3. GET ACCOUNT BY ID");

        // Test 1 - Valid ID
        printSubHeader("Search Account ID: 1");
        Optional<Account> a1 = accountService.getAccountById(1L);
        if (a1.isPresent()) {
            printAccountDetails(a1.get());
        } else {
            System.out.println("   No Account found with ID: 1");
        }

        // Test 2 - Non-existent ID
        printSubHeader("Search Non-Existent Account ID: 9999");
        Optional<Account> a2 = accountService.getAccountById(9999L);
        if (a2.isEmpty()) {
            System.out.println("   No Account found with ID: 9999 (Expected)");
        }

        // Test 3 - Null ID
        printSubHeader("Search Null Account ID");
        try {
            accountService.getAccountById(null);
        } catch (IllegalArgumentException e) {
            System.out.println("   Null ID Rejected (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 4. GET ACCOUNT BY ACCOUNT NUMBER
    // -----------------------------------------------------------------------------

    private static void getAccountByAccountNumber() {

        printOperationHeader("4. GET ACCOUNT BY ACCOUNT NUMBER");

        // Test 1 - Valid Account Number
        printSubHeader("Search Account Number: ACC10001");
        Optional<Account> a1 = accountService.getAccountByAccountNumber("ACC10001");
        if (a1.isPresent()) {
            printAccountDetails(a1.get());
        } else {
            System.out.println("   Not found.");
        }

        // Test 2 - Another valid account number
        printSubHeader("Search Account Number: ACC10003");
        Optional<Account> a2 = accountService.getAccountByAccountNumber("ACC10003");
        if (a2.isPresent()) {
            printAccountDetails(a2.get());
        } else {
            System.out.println("   Not found.");
        }

        // Test 3 - Non-existent account number
        printSubHeader("Search Non-Existent: ACC99999");
        Optional<Account> a3 = accountService.getAccountByAccountNumber("ACC99999");
        if (a3.isEmpty()) {
            System.out.println("   No Account found (Expected)");
        }

        // Test 4 - Blank account number
        printSubHeader("Search Blank Account Number");
        try {
            accountService.getAccountByAccountNumber("");
        } catch (IllegalArgumentException e) {
            System.out.println("   Blank Rejected (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 5. GET ACCOUNTS BY BANK NAME
    // -----------------------------------------------------------------------------

    private static void getAccountsByBankName() {

        printOperationHeader("5. GET ACCOUNTS BY BANK NAME");

        // Test 1 - SBI accounts
        printSubHeader("Search Bank: State Bank");
        List<Account> sbi = accountService.getAccountsByBankName("State Bank");
        printAccountList(sbi);

        // Test 2 - HDFC accounts
        printSubHeader("Search Bank: HDFC");
        List<Account> hdfc = accountService.getAccountsByBankName("HDFC");
        printAccountList(hdfc);

        // Test 3 - ICICI accounts
        printSubHeader("Search Bank: ICICI");
        List<Account> icici = accountService.getAccountsByBankName("ICICI");
        printAccountList(icici);

        // Test 4 - No match
        printSubHeader("Search Bank: XYZ Bank");
        List<Account> noMatch = accountService.getAccountsByBankName("XYZ Bank");
        if (noMatch.isEmpty()) {
            System.out.println("   No Accounts found for XYZ Bank (Expected)");
        }

        // Test 5 - Too short
        printSubHeader("Search Bank: A (Too Short)");
        try {
            accountService.getAccountsByBankName("A");
        } catch (IllegalArgumentException e) {
            System.out.println("   Too Short Rejected (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 6. GET ACCOUNTS BY ACCOUNT TYPE
    // -----------------------------------------------------------------------------

    private static void getAccountsByAccountType() {

        printOperationHeader("6. GET ACCOUNTS BY ACCOUNT TYPE");

        // Test 1 - SAVINGS accounts
        printSubHeader("Account Type: SAVINGS");
        List<Account> savings = accountService.getAccountsByAccountType(AccountType.SAVINGS);
        printAccountList(savings);

        // Test 2 - CURRENT accounts
        printSubHeader("Account Type: CURRENT");
        List<Account> current = accountService.getAccountsByAccountType(AccountType.CURRENT);
        printAccountList(current);

        // Test 3 - FIXED_DEPOSIT accounts
        printSubHeader("Account Type: FIXED_DEPOSIT");
        List<Account> fd = accountService.getAccountsByAccountType(AccountType.FIXED_DEPOSIT);
        printAccountList(fd);

        // Test 4 - RECURRING_DEPOSIT accounts
        printSubHeader("Account Type: RECURRING_DEPOSIT");
        List<Account> rd = accountService.getAccountsByAccountType(AccountType.RECURRING_DEPOSIT);
        printAccountList(rd);

        // Test 5 - Null type
        printSubHeader("Account Type: NULL");
        try {
            accountService.getAccountsByAccountType(null);
        } catch (IllegalArgumentException e) {
            System.out.println("   Null Type Rejected (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 7. GET ACCOUNTS BY ACCOUNT STATUS
    // -----------------------------------------------------------------------------

    private static void getAccountsByAccountStatus() {

        printOperationHeader("7. GET ACCOUNTS BY ACCOUNT STATUS");

        // Test 1 - ACTIVE accounts
        printSubHeader("Account Status: ACTIVE");
        List<Account> active = accountService.getAccountsByAccountStatus(AccountStatus.ACTIVE);
        printAccountList(active);

        // Test 2 - INACTIVE accounts
        printSubHeader("Account Status: INACTIVE");
        List<Account> inactive = accountService.getAccountsByAccountStatus(AccountStatus.INACTIVE);
        printAccountList(inactive);

        // Test 3 - FROZEN accounts
        printSubHeader("Account Status: FROZEN");
        List<Account> frozen = accountService.getAccountsByAccountStatus(AccountStatus.FROZEN);
        printAccountList(frozen);

        // Test 4 - CLOSED accounts
        printSubHeader("Account Status: CLOSED");
        List<Account> closed = accountService.getAccountsByAccountStatus(AccountStatus.CLOSED);
        if (closed.isEmpty()) {
            System.out.println("   No CLOSED accounts found.");
        }

        // Test 5 - Null status
        printSubHeader("Account Status: NULL");
        try {
            accountService.getAccountsByAccountStatus(null);
        } catch (IllegalArgumentException e) {
            System.out.println("   Null Status Rejected (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 8. GET ACCOUNTS BY CUSTOMER ID
    // -----------------------------------------------------------------------------

    private static void getAccountsByCustomerId() {

        printOperationHeader("8. GET ACCOUNTS BY CUSTOMER ID");

        // Test 1 - CID1001 (has 2 accounts)
        printSubHeader("Customer ID: CID1001");
        List<Account> cid1001 = accountService.getAccountsByCustomerId("CID1001");
        printAccountList(cid1001);

        // Test 2 - CID1002 (has 1 account)
        printSubHeader("Customer ID: CID1002");
        List<Account> cid1002 = accountService.getAccountsByCustomerId("CID1002");
        printAccountList(cid1002);

        // Test 3 - CID1003 (has 1 account)
        printSubHeader("Customer ID: CID1003");
        List<Account> cid1003 = accountService.getAccountsByCustomerId("CID1003");
        printAccountList(cid1003);

        // Test 4 - Non-existent Customer ID
        printSubHeader("Customer ID: CID9999");
        List<Account> noAccounts = accountService.getAccountsByCustomerId("CID9999");
        if (noAccounts.isEmpty()) {
            System.out.println("   No Accounts found for CID9999 (Expected)");
        }

        // Test 5 - Null Customer ID
        printSubHeader("Customer ID: NULL");
        try {
            accountService.getAccountsByCustomerId(null);
        } catch (IllegalArgumentException e) {
            System.out.println("   Null Customer ID Rejected (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 9. UPDATE ACCOUNT
    // -----------------------------------------------------------------------------

    private static void updateAccount() {

        printOperationHeader("9. UPDATE ACCOUNT");

        // Test 1 - Valid update
        printSubHeader("Update Account ID: 1");

        Optional<Account> existing = accountService.getAccountById(1L);

        if (existing.isPresent()) {
            Account toUpdate = existing.get();
            toUpdate.setBankName("SBI Updated Branch");
            toUpdate.setAccountStatus(AccountStatus.ACTIVE);
            toUpdate.setBalance(new BigDecimal("55000.00"));

            accountService.updateAccount(toUpdate);
            System.out.println("   Account Updated.");

            // Verify
            accountService.getAccountById(1L)
                .ifPresent(a -> {
                    System.out.println("   Verified Bank Name : " + a.getBankName());
                    System.out.println("   Verified Balance   : " + a.getBalance());
                });
        }

        // Test 2 - Non-existent account
        printSubHeader("Update Non-Existent Account ID: 9999");
        try {
            Account ghost = new Account();
            ghost.setId(9999L);
            ghost.setAccountNumber("ACC99999");
            ghost.setBankName("Ghost Bank");
            ghost.setAccountType(AccountType.SAVINGS);
            ghost.setBalance(new BigDecimal("0.00"));
            ghost.setAccountStatus(AccountStatus.ACTIVE);

            accountService.updateAccount(ghost);
        } catch (RuntimeException e) {
            System.out.println("   Not Found (Expected): " + e.getMessage());
        }

        // Test 3 - Null account
        printSubHeader("Update Null Account");
        try {
            accountService.updateAccount(null);
        } catch (IllegalArgumentException e) {
            System.out.println("   Null Rejected (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 10. CREDIT AMOUNT
    // -----------------------------------------------------------------------------

    private static void creditAmount() {

        printOperationHeader("10. CREDIT AMOUNT");

        // Test 1 - Valid credit to ACTIVE account
        printSubHeader("Credit 10000 to Account ID: 1");

        accountService.getAccountById(1L)
            .ifPresent(a -> System.out.println(
                "   Balance Before Credit: " + a.getBalance()
            ));

        accountService.credit(1L, new BigDecimal("10000.00"));

        accountService.getAccountById(1L)
            .ifPresent(a -> System.out.println(
                "   Balance After Credit : " + a.getBalance()
            ));

        // Test 2 - Credit to FROZEN account
        printSubHeader("Credit to FROZEN Account ID: 4");
        try {
            accountService.credit(4L, new BigDecimal("5000.00"));
        } catch (RuntimeException e) {
            System.out.println("   FROZEN Account Rejected (Expected): " + e.getMessage());
        }

        // Test 3 - Negative credit amount
        printSubHeader("Credit Negative Amount: -1000");
        try {
            accountService.credit(1L, new BigDecimal("-1000.00"));
        } catch (IllegalArgumentException e) {
            System.out.println("   Negative Amount Rejected (Expected): " + e.getMessage());
        }

        // Test 4 - Zero credit amount
        printSubHeader("Credit Zero Amount");
        try {
            accountService.credit(1L, BigDecimal.ZERO);
        } catch (IllegalArgumentException e) {
            System.out.println("   Zero Amount Rejected (Expected): " + e.getMessage());
        }

        // Test 5 - Credit to non-existent account
        printSubHeader("Credit to Non-Existent Account ID: 9999");
        try {
            accountService.credit(9999L, new BigDecimal("1000.00"));
        } catch (RuntimeException e) {
            System.out.println("   Not Found (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 11. DEBIT AMOUNT
    // -----------------------------------------------------------------------------

    private static void debitAmount() {

        printOperationHeader("11. DEBIT AMOUNT");

        // Test 1 - Valid debit from ACTIVE account
        printSubHeader("Debit 5000 from Account ID: 1");

        accountService.getAccountById(1L)
            .ifPresent(a -> System.out.println(
                "   Balance Before Debit : " + a.getBalance()
            ));

        accountService.debit(1L, new BigDecimal("5000.00"));

        accountService.getAccountById(1L)
            .ifPresent(a -> System.out.println(
                "   Balance After Debit  : " + a.getBalance()
            ));

        // Test 2 - Insufficient balance
        printSubHeader("Debit 9999999 (Insufficient Balance)");
        try {
            accountService.debit(1L, new BigDecimal("9999999.00"));
        } catch (RuntimeException e) {
            System.out.println("   Insufficient Balance (Expected): " + e.getMessage());
        }

        // Test 3 - Debit from FROZEN account
        printSubHeader("Debit from FROZEN Account ID: 4");
        try {
            accountService.debit(4L, new BigDecimal("1000.00"));
        } catch (RuntimeException e) {
            System.out.println("   FROZEN Account Rejected (Expected): " + e.getMessage());
        }

        // Test 4 - Negative debit amount
        printSubHeader("Debit Negative Amount: -500");
        try {
            accountService.debit(1L, new BigDecimal("-500.00"));
        } catch (IllegalArgumentException e) {
            System.out.println("   Negative Amount Rejected (Expected): " + e.getMessage());
        }

        // Test 5 - Debit from non-existent account
        printSubHeader("Debit from Non-Existent Account ID: 9999");
        try {
            accountService.debit(9999L, new BigDecimal("100.00"));
        } catch (RuntimeException e) {
            System.out.println("   Not Found (Expected): " + e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------
    // 12. DELETE ACCOUNT
    // -----------------------------------------------------------------------------

    private static void deleteAccount() {

        printOperationHeader("12. DELETE ACCOUNT");

        // Create a temp account to delete
        printSubHeader("Create Temp Account: ACC99999");
        Account temp = new Account(
            "ADMIN", "ACC99999", "TEST0001234",
            "Test Bank", "CID1001",
            AccountType.SAVINGS,
            new BigDecimal("1000.00"),
            "INR", AccountStatus.ACTIVE
        );
        accountService.createAccount(temp);
        System.out.println("   Temp Account Created.");

        // Get the ID of temp account
        Optional<Account> tempFetched =
            accountService.getAccountByAccountNumber("ACC99999");

        if (tempFetched.isPresent()) {
            Long tempId = tempFetched.get().getId();

            // Delete the temp account
            printSubHeader("Delete Temp Account ID: " + tempId);
            accountService.deleteAccount(tempId);

            // Verify deletion
            Optional<Account> afterDelete = accountService.getAccountById(tempId);
            System.out.println("   After Delete Search: "
                + (afterDelete.isEmpty() ? "NOT FOUND (Expected)" : "STILL EXISTS"));
        }

        // Test - Delete non-existent account
        printSubHeader("Delete Non-Existent Account ID: 9999");
        try {
            accountService.deleteAccount(9999L);
        } catch (RuntimeException e) {
            System.out.println("   Not Found (Expected): " + e.getMessage());
        }

        // Test - Delete with null ID
        printSubHeader("Delete with Null ID");
        try {
            accountService.deleteAccount(null);
        } catch (IllegalArgumentException e) {
            System.out.println("   Null ID Rejected (Expected): " + e.getMessage());
        }
    }


    // =============================================================================
    //  PRINT HELPER METHODS
    // =============================================================================

    private static void printCustomerDetails(Customer c) {
        System.out.println("   -----------------------------------");
        System.out.println("   Customer ID     : " + c.getCustomerId());
        System.out.println("   Full Name       : " + c.getFullName());
        System.out.println("   KYC Status      : " + c.getKycStatus());
        System.out.println("   Customer Status : " + c.getCustomerStatus());
        System.out.println("   Created By      : " + c.getCreatedBy());
        System.out.println("   -----------------------------------");
    }

    private static void printAccountDetails(Account a) {
        System.out.println("   -----------------------------------");
        System.out.println("   Account Number  : " + a.getAccountNumber());
        System.out.println("   Bank Name       : " + a.getBankName());
        System.out.println("   IFSC Code       : " + a.getIfscCode());
        System.out.println("   Customer ID     : " + a.getCustomerId());
        System.out.println("   Account Type    : " + a.getAccountType());
        System.out.println("   Balance         : " + a.getBalance());
        System.out.println("   Currency        : " + a.getCurrency());
        System.out.println("   Account Status  : " + a.getAccountStatus());
        System.out.println("   -----------------------------------");
    }

    private static void printCustomerList(List<Customer> customers) {
        if (customers.isEmpty()) {
            System.out.println("   No customers found.");
            return;
        }
        System.out.printf("   %-12s %-22s %-12s %-12s%n",
            "Customer ID", "Full Name", "KYC Status", "Status");
        System.out.println("   " + "-".repeat(62));
        for (Customer c : customers) {
            System.out.printf("   %-12s %-22s %-12s %-12s%n",
                c.getCustomerId(),
                c.getFullName(),
                c.getKycStatus(),
                c.getCustomerStatus());
        }
        System.out.println("   Total: " + customers.size());
    }

    private static void printAccountList(List<Account> accounts) {
        if (accounts.isEmpty()) {
            System.out.println("   No accounts found.");
            return;
        }
        System.out.printf("   %-12s %-25s %-20s %-15s %-12s%n",
            "Acc Number", "Bank Name", "Type", "Balance", "Status");
        System.out.println("   " + "-".repeat(88));
        for (Account a : accounts) {
            System.out.printf("   %-12s %-25s %-20s %-15s %-12s%n",
                a.getAccountNumber(),
                a.getBankName(),
                a.getAccountType(),
                a.getBalance(),
                a.getAccountStatus());
        }
        System.out.println("   Total: " + accounts.size());
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("=================================================================");
        System.out.println("        IISPL SETTLEMENT SYSTEM - DEMO APPLICATION              ");
        System.out.println("               Customer and Account Management                   ");
        System.out.println("=================================================================");
        System.out.println();
    }

    private static void printFooter() {
        System.out.println();
        System.out.println("=================================================================");
        System.out.println("            ALL OPERATIONS COMPLETED SUCCESSFULLY                ");
        System.out.println("=================================================================");
        System.out.println();
    }

    private static void printSectionHeader(String title) {
        System.out.println();
        System.out.println("=================================================================");
        System.out.println("   " + title);
        System.out.println("=================================================================");
        System.out.println();
    }

    private static void printOperationHeader(String title) {
        System.out.println();
        System.out.println("   ---------------------------------------------------------");
        System.out.println("   " + title);
        System.out.println("   ---------------------------------------------------------");
    }

    private static void printSubHeader(String title) {
        System.out.println();
        System.out.println("   -- " + title + " --");
    }
}
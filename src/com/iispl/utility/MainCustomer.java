package com.iispl.utility;

import com.iispl.dao.CustomerDAOImpl;
import com.iispl.entity.Customer;
import com.iispl.service.CustomerService;
import com.iispl.service.CustomerServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/*
 * MainCustomer
 * This is the main console application for Customer Management.
 * It shows a menu and allows the user to perform all
 * Create, Read, Update and Delete operations on Customer.
 * It uses CustomerService for business logic and
 * CustomerDAOImpl for actual database operations.
 */
public class MainCustomer {

    /*
     * Service layer instance.
     * CustomerDAOImpl is injected into CustomerServiceImpl.
     * All database calls go through service then DAO.
     */
    static CustomerService service = new CustomerServiceImpl(new CustomerDAOImpl());

    /*
     * Scanner reads user input from the console.
     */
    static Scanner scanner = new Scanner(System.in);


    // ===========================================================
    // MAIN METHOD
    // Shows the menu in a loop until user enters 0 to exit.
    // ===========================================================
    public static void main(String[] args) {

        int choice;

        do {
            printMenu();
            System.out.print("Enter Your Choice : ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                choice = -1;
                continue;
            }

            switch (choice) {
                case 1:  createCustomer();         break;
                case 2:  getAllCustomers();         break;
                case 3:  getCustomerById();         break;
                case 4:  getCustomerByCustomerId(); break;
                case 5:  getCustomersByFullName();  break;
                case 6:  getActiveCustomers();      break;
                case 7:  getVerifiedCustomers();    break;
                case 8:  updateCustomer();          break;
                case 9:  updateKycStatus();         break;
                case 10: activateCustomer();        break;
                case 11: deactivateCustomer();      break;
                case 12: removeCustomer();          break;
                case 0:
                    System.out.println("Exiting Customer Menu. Goodbye.");
                    break;
                default:
                    System.out.println("Invalid Choice. Please Try Again.");
            }

        } while (choice != 0);

        scanner.close();
    }


    // ===========================================================
    // MENU
    // Prints all available options to the console.
    // ===========================================================
    private static void printMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║     CUSTOMER MANAGEMENT SYSTEM       ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  CREATE OPERATIONS                   ║");
        System.out.println("║  1.  Create New Customer             ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  READ OPERATIONS                     ║");
        System.out.println("║  2.  Get All Customers               ║");
        System.out.println("║  3.  Get Customer by Auto ID         ║");
        System.out.println("║  4.  Get Customer by Customer ID     ║");
        System.out.println("║  5.  Get Customer by Full Name       ║");
        System.out.println("║  6.  Get All Active Customers        ║");
        System.out.println("║  7.  Get All Verified Customers      ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  UPDATE OPERATIONS                   ║");
        System.out.println("║  8.  Update Customer Details         ║");
        System.out.println("║  9.  Update KYC Status               ║");
        System.out.println("║  10. Activate Customer               ║");
        System.out.println("║  11. Deactivate Customer             ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  DELETE OPERATIONS                   ║");
        System.out.println("║  12. Remove Customer                 ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  0.  Exit                            ║");
        System.out.println("╚══════════════════════════════════════╝");
    }


    // ===========================================================
    // 1. CREATE CUSTOMER
    // Takes all details from user and calls service.registerCustomer()
    // Service validates the data and DAO saves it to database.
    // ===========================================================
    private static void createCustomer() {

        System.out.println("\n--- CREATE NEW CUSTOMER ---");

        Customer c = new Customer();

        System.out.print("Enter Customer ID   (e.g. CID1011)        : ");
        c.setCustomerId(scanner.nextLine().trim());

        System.out.print("Enter Full Name     (e.g. Rahul Sharma)   : ");
        c.setFullName(scanner.nextLine().trim());

        System.out.print("Enter KYC Status    (VERIFIED / PENDING)  : ");
        c.setKycStatus(scanner.nextLine().trim().toUpperCase());

        System.out.print("Enter Cust Status   (ACTIVE / INACTIVE)   : ");
        c.setCustomerStatus(scanner.nextLine().trim().toUpperCase());

        System.out.print("Enter Created By    (e.g. SYSTEM / ADMIN) : ");
        c.setCreatedBy(scanner.nextLine().trim());

        c.setVersion(0);

        /*
         * Call service.registerCustomer()
         * Service checks:
         *   - Customer ID not duplicate
         *   - Full Name not blank
         *   - KYC Status valid
         *   - Customer Status valid
         * Then delegates to DAO for DB insert.
         */
        boolean result = service.registerCustomer(c);

        if (result) {
            System.out.println("Customer Created Successfully.");
            System.out.println("Customer ID : " + c.getCustomerId());
            System.out.println("Full Name   : " + c.getFullName());
        } else {
            System.out.println("Failed to Create Customer. Please check the details.");
        }
    }


    // ===========================================================
    // 2. GET ALL CUSTOMERS
    // Calls service.getAllCustomers() which calls DAO.
    // DAO runs SELECT * FROM customer and returns all records.
    // ===========================================================
    private static void getAllCustomers() {

        System.out.println("\n--- ALL CUSTOMERS ---");

        /*
         * service.getAllCustomers() calls customerDAO.getAllCustomers()
         * which fetches all rows from the customer table.
         */
        List<Customer> list = service.getAllCustomers();

        if (list.isEmpty()) {
            System.out.println("No Customers Found in Database.");
        } else {
            System.out.println("Total Customers : " + list.size());
            System.out.println("--------------------------------------------------");
            for (Customer c : list) {
                printCustomer(c);
            }
        }
    }


    // ===========================================================
    // 3. GET CUSTOMER BY AUTO ID (bigserial primary key)
    // Takes the database auto-generated ID as input.
    // Calls service.getCustomerById() which calls DAO.
    // DAO runs SELECT * FROM customer WHERE id = ?
    // ===========================================================
    private static void getCustomerById() {

        System.out.println("\n--- GET CUSTOMER BY AUTO ID ---");
        System.out.print("Enter Auto ID (e.g. 1, 2, 3 ...) : ");
        String id = scanner.nextLine().trim();

        /*
         * service.getCustomerById() calls customerDAO.getCustomerById()
         * which queries the customer table using the bigserial primary key.
         */
        Optional<Customer> opt = service.getCustomerById(id);

        if (opt.isPresent()) {
            System.out.println("Customer Found :");
            printCustomer(opt.get());
        } else {
            System.out.println("No Customer Found for Auto ID : " + id);
        }
    }


    // ===========================================================
    // 4. GET CUSTOMER BY CUSTOMER ID (CID like CID1001)
    // Takes the business Customer ID as input.
    // Calls service.getCustomerByCustomerId() which calls DAO.
    // DAO runs SELECT * FROM customer WHERE customer_id = ?
    // ===========================================================
    private static void getCustomerByCustomerId() {

        System.out.println("\n--- GET CUSTOMER BY CUSTOMER ID ---");
        System.out.print("Enter Customer ID (e.g. CID1001) : ");
        String customerId = scanner.nextLine().trim();

        /*
         * service.getCustomerByCustomerId() validates input is not blank
         * then calls customerDAO.getCustomerByCustomerId()
         * which queries the customer table using customer_id column.
         */
        Optional<Customer> opt = service.getCustomerByCustomerId(customerId);

        if (opt.isPresent()) {
            System.out.println("Customer Found :");
            printCustomer(opt.get());
        } else {
            System.out.println("No Customer Found for Customer ID : " + customerId);
        }
    }


    // ===========================================================
    // 5. GET CUSTOMERS BY FULL NAME
    // Takes a name keyword and does a partial search.
    // Calls service.getCustomersByFullName() which calls DAO.
    // DAO runs SELECT * FROM customer WHERE LOWER(full_name) LIKE ?
    // Example: input "rahul" finds "Rahul Sharma", "Rahul Kumar"
    // ===========================================================
    private static void getCustomersByFullName() {

        System.out.println("\n--- GET CUSTOMERS BY FULL NAME ---");
        System.out.print("Enter Full Name to Search (e.g. Rahul) : ");
        String fullName = scanner.nextLine().trim();

        /*
         * service.getCustomersByFullName() validates minimum 2 characters
         * then calls customerDAO.getCustomersByFullName()
         * which does a case insensitive partial search on full_name column.
         */
        List<Customer> list = service.getCustomersByFullName(fullName);

        if (list.isEmpty()) {
            System.out.println("No Customers Found for Name : " + fullName);
        } else {
            System.out.println("Total Customers Found : " + list.size());
            System.out.println("--------------------------------------------------");
            for (Customer c : list) {
                printCustomer(c);
            }
        }
    }


    // ===========================================================
    // 6. GET ACTIVE CUSTOMERS
    // Calls service.getActiveCustomers()
    // Service fetches all customers from DAO and filters
    // only those with customer_status = ACTIVE using Java Stream.
    // ===========================================================
    private static void getActiveCustomers() {

        System.out.println("\n--- ACTIVE CUSTOMERS ---");

        /*
         * service.getActiveCustomers() calls customerDAO.getAllCustomers()
         * then filters records where customerStatus equals ACTIVE.
         */
        List<Customer> list = service.getActiveCustomers();

        if (list.isEmpty()) {
            System.out.println("No Active Customers Found.");
        } else {
            System.out.println("Total Active Customers : " + list.size());
            System.out.println("--------------------------------------------------");
            for (Customer c : list) {
                printCustomer(c);
            }
        }
    }


    // ===========================================================
    // 7. GET KYC VERIFIED CUSTOMERS
    // Calls service.getVerifiedCustomers()
    // Service fetches all customers from DAO and filters
    // only those with kyc_status = VERIFIED using Java Stream.
    // ===========================================================
    private static void getVerifiedCustomers() {

        System.out.println("\n--- KYC VERIFIED CUSTOMERS ---");

        /*
         * service.getVerifiedCustomers() calls customerDAO.getAllCustomers()
         * then filters records where kycStatus equals VERIFIED.
         */
        List<Customer> list = service.getVerifiedCustomers();

        if (list.isEmpty()) {
            System.out.println("No KYC Verified Customers Found.");
        } else {
            System.out.println("Total Verified Customers : " + list.size());
            System.out.println("--------------------------------------------------");
            for (Customer c : list) {
                printCustomer(c);
            }
        }
    }


    // ===========================================================
    // 8. UPDATE CUSTOMER DETAILS
    // Fetches existing customer first, shows current values,
    // then takes new values. Press ENTER to keep current value.
    // Calls service.updateCustomer() which calls DAO.
    // DAO runs UPDATE customer SET ... WHERE customer_id = ?
    // ===========================================================
    private static void updateCustomer() {

        System.out.println("\n--- UPDATE CUSTOMER DETAILS ---");
        System.out.print("Enter Customer ID to Update (e.g. CID1001) : ");
        String customerId = scanner.nextLine().trim();

        /*
         * First fetch existing customer to show current values.
         * service.getCustomerByCustomerId() calls DAO to fetch from DB.
         */
        Optional<Customer> opt = service.getCustomerByCustomerId(customerId);

        if (!opt.isPresent()) {
            System.out.println("Customer Not Found : " + customerId);
            return;
        }

        Customer c = opt.get();

        System.out.println("Current Details :");
        printCustomer(c);

        System.out.println("Enter New Details (Press ENTER to keep current value)");

        System.out.print("Enter New Full Name       [" + c.getFullName() + "] : ");
        String fullName = scanner.nextLine().trim();
        if (!fullName.isEmpty()) {
            c.setFullName(fullName);
        }

        System.out.print("Enter New KYC Status      [" + c.getKycStatus()
            + "] (VERIFIED / PENDING) : ");
        String kyc = scanner.nextLine().trim().toUpperCase();
        if (!kyc.isEmpty()) {
            c.setKycStatus(kyc);
        }

        System.out.print("Enter New Customer Status [" + c.getCustomerStatus()
            + "] (ACTIVE / INACTIVE) : ");
        String status = scanner.nextLine().trim().toUpperCase();
        if (!status.isEmpty()) {
            c.setCustomerStatus(status);
        }

        /*
         * service.updateCustomer() validates the updated customer object
         * then calls customerDAO.updateCustomer()
         * which runs UPDATE on the customer table.
         * version column is auto incremented by SQL on each update.
         */
        boolean result = service.updateCustomer(c);

        if (result) {
            System.out.println("Customer Updated Successfully.");
            System.out.println("Customer ID : " + c.getCustomerId());
            System.out.println("New Name    : " + c.getFullName());
            System.out.println("KYC Status  : " + c.getKycStatus());
            System.out.println("Status      : " + c.getCustomerStatus());
        } else {
            System.out.println("Failed to Update Customer.");
        }
    }


    // ===========================================================
    // 9. UPDATE KYC STATUS ONLY
    // Takes Customer ID and new KYC Status from user.
    // Calls service.updateKycStatus() which fetches customer,
    // sets new KYC status and calls DAO to update in database.
    // ===========================================================
    private static void updateKycStatus() {

        System.out.println("\n--- UPDATE KYC STATUS ---");

        System.out.print("Enter Customer ID  (e.g. CID1001)         : ");
        String customerId = scanner.nextLine().trim();

        System.out.print("Enter New KYC Status (VERIFIED / PENDING) : ");
        String kycStatus = scanner.nextLine().trim().toUpperCase();

        /*
         * service.updateKycStatus() validates both customerId and kycStatus.
         * Fetches existing customer from DAO.
         * Sets new kycStatus on customer object.
         * Calls customerDAO.updateCustomer() to save changes.
         */
        boolean result = service.updateKycStatus(customerId, kycStatus);

        if (result) {
            System.out.println("KYC Status Updated Successfully.");
            System.out.println("Customer ID : " + customerId);
            System.out.println("New KYC     : " + kycStatus);
        } else {
            System.out.println("Failed to Update KYC Status.");
        }
    }


    // ===========================================================
    // 10. ACTIVATE CUSTOMER
    // Takes Customer ID and sets status to ACTIVE.
    // Calls service.activateCustomer() which checks if customer
    // is INACTIVE before updating. Calls DAO to save change.
    // ===========================================================
    private static void activateCustomer() {

        System.out.println("\n--- ACTIVATE CUSTOMER ---");

        System.out.print("Enter Customer ID to Activate (e.g. CID1001) : ");
        String customerId = scanner.nextLine().trim();

        /*
         * service.activateCustomer() checks:
         *   - Customer exists in DB
         *   - Customer is currently INACTIVE
         * Then sets status to ACTIVE and calls customerDAO.updateCustomer().
         */
        boolean result = service.activateCustomer(customerId);

        if (result) {
            System.out.println("Customer Activated Successfully.");
            System.out.println("Customer ID : " + customerId);
            System.out.println("New Status  : ACTIVE");
        } else {
            System.out.println("Failed to Activate.");
            System.out.println("Customer may already be ACTIVE or not found.");
        }
    }


    // ===========================================================
    // 11. DEACTIVATE CUSTOMER
    // Takes Customer ID and sets status to INACTIVE.
    // Calls service.deactivateCustomer() which checks if customer
    // is ACTIVE before updating. Calls DAO to save change.
    // ===========================================================
    private static void deactivateCustomer() {

        System.out.println("\n--- DEACTIVATE CUSTOMER ---");

        System.out.print("Enter Customer ID to Deactivate (e.g. CID1001) : ");
        String customerId = scanner.nextLine().trim();

        /*
         * service.deactivateCustomer() checks:
         *   - Customer exists in DB
         *   - Customer is currently ACTIVE
         * Then sets status to INACTIVE and calls customerDAO.updateCustomer().
         */
        boolean result = service.deactivateCustomer(customerId);

        if (result) {
            System.out.println("Customer Deactivated Successfully.");
            System.out.println("Customer ID : " + customerId);
            System.out.println("New Status  : INACTIVE");
        } else {
            System.out.println("Failed to Deactivate.");
            System.out.println("Customer may already be INACTIVE or not found.");
        }
    }


    // ===========================================================
    // 12. REMOVE CUSTOMER
    // Takes Customer ID and asks for confirmation before delete.
    // Calls service.removeCustomer() which checks if customer
    // exists then calls DAO to delete from database.
    // DAO runs DELETE FROM customer WHERE customer_id = ?
    // ===========================================================
    private static void removeCustomer() {

        System.out.println("\n--- REMOVE CUSTOMER ---");

        System.out.print("Enter Customer ID to Remove (e.g. CID1001) : ");
        String customerId = scanner.nextLine().trim();

        System.out.print("Are You Sure to Delete Customer " + customerId
            + " ? (yes / no) : ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("yes")) {
            System.out.println("Delete Cancelled. No changes made.");
            return;
        }

        /*
         * service.removeCustomer() checks:
         *   - Customer ID is not blank
         *   - Customer exists in DB
         * Then calls customerDAO.deleteCustomerByCustomerId()
         * which runs DELETE query on the customer table.
         */
        boolean result = service.removeCustomer(customerId);

        if (result) {
            System.out.println("Customer Removed Successfully.");
            System.out.println("Customer ID : " + customerId);
        } else {
            System.out.println("Failed to Remove Customer.");
            System.out.println("Customer may not exist in database.");
        }
    }


    // ===========================================================
    // PRINT HELPER
    // Prints all fields of a Customer object in a readable format.
    // Called by all READ operation methods above.
    // ===========================================================
    private static void printCustomer(Customer c) {
        System.out.println("--------------------------------------------------");
        System.out.println(" Customer ID     : " + c.getCustomerId());
        System.out.println(" Full Name       : " + c.getFullName());
        System.out.println(" KYC Status      : " + c.getKycStatus());
        System.out.println(" Customer Status : " + c.getCustomerStatus());
        System.out.println(" Created By      : " + c.getCreatedBy());
        System.out.println(" Created At      : " + c.getCreatedAt());
        System.out.println(" Version         : " + c.getVersion());
        System.out.println("--------------------------------------------------");
    }
}
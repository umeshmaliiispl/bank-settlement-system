package com.iispl.dao;


import com.iispl.entity.Customer;

import java.util.List;
import java.util.Optional;

/**
 * CustomerDAO Interface
 * Defines all CRUD + Query operations for Customer entity
 *
 * Operations:
 * READ   : getAllCustomers, getAllActiveCustomers,
 *             getAllVerifiedCustomers, getCustomerById,
 *             getCustomerByCustomerId, getCustomersByFullName
 */
public interface CustomerDAO {

    

    // ==================== READ ====================

    List<Customer> getAllCustomers();

    List<Customer> getAllActiveCustomers();

    List<Customer> getAllVerifiedCustomers();

    Optional<Customer> getCustomerByCustomerId(String customerId);

    List<Customer> getCustomersByFullName(String fullName);
}

package com.iispl.dao;

import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AccountDao Interface
 * Defines all CRUD + Query operations for Account entity
 *
 * Operations:
 * ── WRITE  : save, update, updateBalance, delete
 * ── READ   : findAll, findById, findByAccountNumber,
 *             findByBankName, findByAccountType,
 *             findByAccountStatus, findByCustomerId
 */
public interface AccountDao {

    // ==================== WRITE ====================

    void save(Account account);

    void update(Account account);

    void updateBalance(Long accountId, BigDecimal balance);

    void delete(Long accountId);

    // ==================== READ ====================

    List<Account> findAll();

    Optional<Account> findById(Long accountId);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByBankName(String bankName);

    List<Account> findByAccountType(AccountType accountType);

    List<Account> findByAccountStatus(AccountStatus accountStatus);

    List<Account> findByCustomerId(String customerId);
}
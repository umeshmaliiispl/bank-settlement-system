package com.iispl.dao;

import com.iispl.entity.Account;
import com.iispl.enums.AccountStatus;
import com.iispl.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AccountDao Interface Defines all CRUD + Query operations for Account entity
 *
 * Operations: ── WRITE : save, update, updateBalance, delete ── READ : findAll,
 * findById, findByAccountNumber, findByBankName, findByAccountType,
 * findByAccountStatus, findByCustomerId
 */
public interface AccountDao {

	// ==================== READ ====================

	List<Account> findAll();

	Optional<Account> findByAccountNumber(String accountNumber);

	List<Account> findByBankName(String bankName);

	List<Account> findByAccountType(AccountType accountType);

	List<Account> findByAccountStatus(AccountStatus accountStatus);

	List<Account> findByCustomerId(String customerId);
}
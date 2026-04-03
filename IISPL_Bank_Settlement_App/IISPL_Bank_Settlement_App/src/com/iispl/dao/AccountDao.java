
package com.iispl.dao;

import com.iispl.entity.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountDao {

    void save(Account account);

    Optional<Account> findById(Long accountId);

    List<Account> findAll();

    void update(Account account);

    void updateBalance(Long accountId, BigDecimal balance);

    void delete(Long accountId);
}
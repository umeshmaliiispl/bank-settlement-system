package com.iispl.service;


import com.iispl.entity.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    void createAccount(Account account);

    Account getAccount(Long id);

    List<Account> getAllAccounts();

    void updateAccount(Account account);

    void deleteAccount(Long id);

    void credit(Long id, BigDecimal amount);

    void debit(Long id, BigDecimal amount);
}

package com.iispl.dao;


import com.iispl.entity.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionDao {

    void save(Transaction transaction);

    Optional<Transaction> findById(Long transactionId);

    List<Transaction> findAll();

    void delete(Long transactionId);
}

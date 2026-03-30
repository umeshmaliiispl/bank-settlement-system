package com.iispl.entity;

import java.math.BigDecimal;

public class DebitTransaction extends Transaction {

    public DebitTransaction() {
        setType("DEBIT");
    }

    @Override
    public void execute(Account account) {

        if (account.getBalance().compareTo(getAmount()) < 0) {
            throw new RuntimeException("Insufficient Balance");
        }

        BigDecimal newBal = account.getBalance().subtract(getAmount());
        account.setBalance(newBal);
    }
}
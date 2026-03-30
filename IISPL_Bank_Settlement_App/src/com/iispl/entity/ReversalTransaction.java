package com.iispl.entity;

import java.math.BigDecimal;

public class ReversalTransaction extends Transaction {

    public ReversalTransaction() {
        setType("REVERSAL");
    }

    @Override
    public void execute(Account account) {
        BigDecimal newBal = account.getBalance().add(getAmount());
        account.setBalance(newBal);
    }
}
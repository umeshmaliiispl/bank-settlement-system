package com.iispl.entity;

import java.math.BigDecimal;

public class CreditTransaction extends Transaction {

    public CreditTransaction() {
        setType("CREDIT");
    }

    @Override
    public void execute(Account account) {
        BigDecimal newBal = account.getBalance().add(getAmount());
        account.setBalance(newBal);
    }

}
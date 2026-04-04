package com.iispl.entity;

import java.math.BigDecimal;

public class CreditTransaction extends Transaction {

    public CreditTransaction() {
        setType("CREDIT");
    }

    @Override
    public void execute(Account account) {
        account.credit(getAmount());  // ✅ correct way
    }
}
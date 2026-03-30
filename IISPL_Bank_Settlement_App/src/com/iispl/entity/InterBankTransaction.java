package com.iispl.entity;

import java.math.BigDecimal;

public class InterBankTransaction extends Transaction {

    private String receiverAccount;

    public InterBankTransaction() {
        setType("INTERBANK");
    }

    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }

    @Override
    public void execute(Account account) {
        BigDecimal newBal = account.getBalance().subtract(getAmount());
        account.setBalance(newBal);
    }
}
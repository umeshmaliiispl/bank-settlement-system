package com.iispl.entity;

import java.math.BigDecimal;

public class DebitTransaction extends Transaction {

	public DebitTransaction() {
		setType("DEBIT");
	}

	@Override
	public void execute(Account account) {
	    account.debit(getAmount());   
	}
}
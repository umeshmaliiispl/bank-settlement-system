package com.iispl.entity;

import java.math.BigDecimal;

public class ReversalTransaction extends Transaction {

	public ReversalTransaction() {
		setType("REVERSAL");
	}
	@Override
	public void execute(Account account) {
	    account.credit(getAmount()); 
	}
}
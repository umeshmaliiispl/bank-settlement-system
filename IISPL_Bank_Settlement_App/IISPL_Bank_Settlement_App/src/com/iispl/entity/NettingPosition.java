package com.iispl.entity;

import java.time.LocalDate;
import com.iispl.enums.NetDirection;

public class NettingPosition extends BaseEntity {

	public long getCounterpartyBankId() {
		return counterpartyBankId;
	}

	public void setCounterpartyBankId(long counterpartyBankId) {
		this.counterpartyBankId = counterpartyBankId;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public double getGrossDebitAmount() {
		return grossDebitAmount;
	}

	public void setGrossDebitAmount(double grossDebitAmount) {
		this.grossDebitAmount = grossDebitAmount;
	}

	public double getGrossCreditAmount() {
		return grossCreditAmount;
	}

	public void setGrossCreditAmount(double grossCreditAmount) {
		this.grossCreditAmount = grossCreditAmount;
	}

	public double getNetAmount() {
		return netAmount;
	}

	public void setNetAmount(double netAmount) {
		this.netAmount = netAmount;
	}

	public NetDirection getDirection() {
		return direction;
	}

	public void setDirection(NetDirection direction) {
		this.direction = direction;
	}

	public LocalDate getPositionDate() {
		return positionDate;
	}

	public void setPositionDate(LocalDate positionDate) {
		this.positionDate = positionDate;
	}

	public NettingPosition(long counterpartyBankId, String currency, double grossDebitAmount, double grossCreditAmount,
			double netAmount, NetDirection direction, LocalDate positionDate) {
		super();
		this.counterpartyBankId = counterpartyBankId;
		this.currency = currency;
		this.grossDebitAmount = grossDebitAmount;
		this.grossCreditAmount = grossCreditAmount;
		this.netAmount = netAmount;
		this.direction = direction;
		this.positionDate = positionDate;
	}

	private long counterpartyBankId;
	private String currency;

	private double grossDebitAmount;
	private double grossCreditAmount;
	private double netAmount;

	private NetDirection direction;
	private LocalDate positionDate;

}
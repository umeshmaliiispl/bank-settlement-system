package com.iispl.entity;

import java.time.LocalDate;

import com.iispl.enums.ChannelType;
import com.iispl.enums.InstructionStatus;

public class SettlementInstruction {

	private String instructionId;
	private String fromBank;
	private String toBank;
	private double amount;

	private ChannelType channel;
	private InstructionStatus instructionStatus;

	private LocalDate valueDate;

	public String getInstructionId() {
		return instructionId;
	}

	public void setInstructionId(String instructionId) {
		this.instructionId = instructionId;
	}

	public String getFromBank() {
		return fromBank;
	}

	public void setFromBank(String fromBank) {
		this.fromBank = fromBank;
	}

	public String getToBank() {
		return toBank;
	}

	public void setToBank(String toBank) {
		this.toBank = toBank;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public ChannelType getChannel() {
		return channel;
	}

	public void setChannel(ChannelType channel) {
		this.channel = channel;
	}

	public InstructionStatus getInstructionStatus() {
		return instructionStatus;
	}

	public void setInstructionStatus(InstructionStatus instructionStatus) {
		this.instructionStatus = instructionStatus;
	}

	public LocalDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(LocalDate valueDate) {
		this.valueDate = valueDate;
	}
}
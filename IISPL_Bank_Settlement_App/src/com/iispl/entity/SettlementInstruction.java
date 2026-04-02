package com.iispl.entity;

import java.time.LocalDate;
import com.iispl.enums.ChannelType;
import com.iispl.enums.InstructionStatus;

public class SettlementInstruction extends BaseEntity {

	public String getInstructionId() {
		return instructionId;
	}

	public void setInstructionId(String instructionId) {
		this.instructionId = instructionId;
	}

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

	public ChannelType getChannel() {
		return channel;
	}

	public void setChannel(ChannelType channel) {
		this.channel = channel;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public LocalDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(LocalDate valueDate) {
		this.valueDate = valueDate;
	}

	public String getSenderBankId() {
		return senderBankId;
	}

	public void setSenderBankId(String senderBankId) {
		this.senderBankId = senderBankId;
	}

	public String getReceiverBankId() {
		return receiverBankId;
	}

	public void setReceiverBankId(String receiverBankId) {
		this.receiverBankId = receiverBankId;
	}

	public InstructionStatus getInstructionStatus() {
		return instructionStatus;
	}

	public void setInstructionStatus(InstructionStatus instructionStatus) {
		this.instructionStatus = instructionStatus;
	}

	public SettlementInstruction(String instructionId, long transactionId, ChannelType channel, int priority,
			LocalDate valueDate, String senderBankId, String receiverBankId, InstructionStatus instructionStatus) {
		super();
		this.instructionId = instructionId;
		this.transactionId = transactionId;
		this.channel = channel;
		this.priority = priority;
		this.valueDate = valueDate;
		this.senderBankId = senderBankId;
		this.receiverBankId = receiverBankId;
		this.instructionStatus = instructionStatus;
	}

	private String instructionId;
	private long transactionId;

	private ChannelType channel;
	private int priority;
	private LocalDate valueDate;

	private String senderBankId;
	private String receiverBankId;

	private InstructionStatus instructionStatus;

}
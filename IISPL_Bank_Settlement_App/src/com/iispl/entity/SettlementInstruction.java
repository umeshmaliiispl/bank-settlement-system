package com.iispl.entity;

import java.time.LocalDate;
import com.iispl.enums.ChannelType;
import com.iispl.enums.InstructionStatus;

public class SettlementInstruction extends BaseEntity {

    private String instructionId;
    private long transactionId;

    private ChannelType channel;
    private int priority;
    private LocalDate valueDate;

    private long senderBankId;
    private long receiverBankId;

    private InstructionStatus instructionStatus;

    // Getters & Setters
}
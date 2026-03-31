package com.iispl.entity;

import java.time.LocalDateTime;
import com.iispl.enums.SettlementStatus;

public class SettlementRecord extends BaseEntity {

    private String batchId;
    private long incomingTxnId;
    private double settledAmount;
    private LocalDateTime settledDate;
    private SettlementStatus settledStatus;
    private String failureReason;

    // Getters & Setters
}
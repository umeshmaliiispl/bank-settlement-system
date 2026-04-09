package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.iispl.enums.NetDirection;

public class NettingPosition {
	
	

    private String cycleId;
    private LocalDate positionDate;

    private String channel;

    private String senderBank;
    private String receiverBank;

    private String currency;

    private int totalTxnCount;
    private int totalDepositCount;
    private int totalWithdrawCount;

    // ✅ Use BigDecimal (correct for banking)
    private BigDecimal totalDepositAmount = BigDecimal.ZERO;
    private BigDecimal totalWithdrawAmount = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;

    private NetDirection direction;

    // ================= BUSINESS LOGIC =================

    public void addDebit(BigDecimal amount) {
        if (amount == null) return;

        totalTxnCount++;
        totalWithdrawCount++;
        totalWithdrawAmount = totalWithdrawAmount.add(amount);
    }

    public void addCredit(BigDecimal amount) {
        if (amount == null) return;

        totalTxnCount++;
        totalDepositCount++;
        totalDepositAmount = totalDepositAmount.add(amount);
    }

    public void calculateNet() {
        netAmount = totalDepositAmount.subtract(totalWithdrawAmount);

        if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
            direction = NetDirection.NET_CREDIT;
        } else if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
            direction = NetDirection.NET_DEBIT;
        } else {
            direction = NetDirection.FLAT;
        }
    }

    // ================= GETTERS & SETTERS =================

    public String getCycleId() {
        return cycleId;
    }

    public void setCycleId(String cycleId) {
        this.cycleId = cycleId;
    }

    public LocalDate getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(LocalDate positionDate) {
        this.positionDate = positionDate;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSenderBank() {
        return senderBank;
    }

    public void setSenderBank(String senderBank) {
        this.senderBank = senderBank;
    }

    public String getReceiverBank() {
        return receiverBank;
    }

    public void setReceiverBank(String receiverBank) {
        this.receiverBank = receiverBank;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getTotalTxnCount() {
        return totalTxnCount;
    }

    public int getTotalDepositCount() {
        return totalDepositCount;
    }

    public int getTotalWithdrawCount() {
        return totalWithdrawCount;
    }

    public BigDecimal getTotalDepositAmount() {
        return totalDepositAmount;
    }

    public BigDecimal getTotalWithdrawAmount() {
        return totalWithdrawAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public NetDirection getDirection() {
        return direction;
    }

    public void setDirection(NetDirection direction) {
        this.direction = direction;
    }
    
    
 // 🔥 ADD THESE
    private Set<String> senderBanks = new HashSet<>();
    private Set<String> receiverBanks = new HashSet<>();
    
 // 🔥 ADD METHODS

    public void addSenderBank(String bank) {
        if (bank != null && !bank.isEmpty()) {
            senderBanks.add(bank);
        }
    }

    public void addReceiverBank(String bank) {
        if (bank != null && !bank.isEmpty()) {
            receiverBanks.add(bank);
        }
    }

    public void finalizeBanks() {
        this.senderBank = senderBanks.size() == 1
                ? senderBanks.iterator().next()
                : "MULTIPLE";

        this.receiverBank = receiverBanks.size() == 1
                ? receiverBanks.iterator().next()
                : "MULTIPLE";
    }
    
    
}
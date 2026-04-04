package com.iispl.entity;

import com.iispl.enums.NetDirection;

public class NettingPosition {

    private String bankName;
    private String currency;

    private double grossDebitAmount;
    private double grossCreditAmount;
    private double netAmount;

    private NetDirection direction;

    // ─────────────────────────────────────────────
    // BUSINESS METHODS
    // ─────────────────────────────────────────────

    public void addDebit(double amount) {
        this.grossDebitAmount += amount;
    }

    public void addCredit(double amount) {
        this.grossCreditAmount += amount;
    }

    public void calculateNet() {
        this.netAmount = grossCreditAmount - grossDebitAmount;

        if (netAmount > 0) {
            direction = NetDirection.NET_CREDIT;
        } else if (netAmount < 0) {
            direction = NetDirection.NET_DEBIT;
        } else {
            direction = NetDirection.FLAT;
        }
    }

    // ─────────────────────────────────────────────
    // GETTERS & SETTERS
    // ─────────────────────────────────────────────

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
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
}
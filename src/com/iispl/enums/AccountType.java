package com.iispl.enums;

/**
 * AccountType Enum
 * Represents the type of a bank account.
 *
 * Values:
 *   SAVINGS           → Regular savings account
 *   CURRENT           → Current / business account
 *   FIXED_DEPOSIT     → Fixed deposit account
 *   RECURRING_DEPOSIT → Recurring deposit account
 *   FOREX             → Foreign exchange account
 */
public enum AccountType {

    /*
     * Regular savings account for individuals.
     */
    SAVINGS,

    /*
     * Current account typically used for businesses.
     */
    CURRENT,

    /*
     * Fixed deposit account with locked amount for a fixed period.
     */
    FIXED_DEPOSIT,

    /*
     * Recurring deposit account with regular monthly deposits.
     */
    RECURRING_DEPOSIT,

    /*
     * Foreign exchange account used for international transactions.
     * Supports currencies like USD, EUR, GBP etc.
     */
    FOREX
}
package com.iispl.enums;

/**
 * AccountStatus Enum
 * Represents the current status of a bank account.
 *
 * Values:
 *   ACTIVE   → Account is active and operational
 *   INACTIVE → Account is inactive / dormant
 *   FROZEN   → Account is frozen, no transactions allowed
 *   CLOSED   → Account is permanently closed
 *   BLOCKED  → Account is blocked by bank or compliance
 */
public enum AccountStatus {

    /*
     * Account is fully active and operational.
     * All transactions are allowed.
     */
    ACTIVE,

    /*
     * Account is inactive or dormant.
     * No recent transactions detected.
     */
    INACTIVE,

    /*
     * Account is frozen.
     * No debit or credit transactions allowed.
     * Usually due to legal or compliance hold.
     */
    FROZEN,

    /*
     * Account is permanently closed.
     * No transactions allowed.
     */
    CLOSED,

    /*
     * Account is blocked.
     * Blocked by bank or compliance team.
     * Temporary restriction on all transactions.
     */
    BLOCKED
}
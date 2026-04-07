package com.iispl.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.iispl.config.DatabaseConfig;
import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.ProcessingStatus;

/**
 * ValidationService — Production-grade, database-backed transaction validator
 * for the IISPL Bank Settlement pipeline.
 *
 * <p>Validates every {@link IncomingTransaction} across three sequential gates:
 * <ol>
 *   <li><b>Basic field validation</b>   — null / blank / self-transfer checks</li>
 *   <li><b>Sender validation</b>        — account existence, account status, customer status, KYC</li>
 *   <li><b>Receiver validation</b>      — same checks applied to the receiving party</li>
 * </ol>
 *
 * <p>Each gate returns a new {@link IncomingTransaction} instance with an updated
 * {@link ProcessingStatus} and, on failure, a structured error code. Processing
 * stops at the first gate that does not produce {@code VALIDATED}.
 *
 * <p><b>Design characteristics:</b>
 * <ul>
 *   <li>Declared {@code final} — not intended for subclassing</li>
 *   <li>Immutable inputs — all results are produced via {@code toBuilder()} copies;
 *       the original transaction is never mutated</li>
 *   <li>DB-backed — account and customer lookups query live tables via {@link DatabaseConfig}</li>
 *   <li>Fintech-safe — no side-effects on the input object</li>
 * </ul>
 *
 * <p><b>Error Code Reference:</b>
 * <pre>
 * ┌──────────┬──────────────────────────────────────────────────────────┐
 * │ Code     │ Meaning                                                  │
 * ├──────────┼──────────────────────────────────────────────────────────┤
 * │ VAL-001  │ senderCustomerId is null or blank                        │
 * │ VAL-002  │ receiverCustomerId is null or blank                      │
 * │ VAL-003  │ senderAccount is null or blank                           │
 * │ VAL-004  │ receiverAccount is null or blank                         │
 * │ VAL-005  │ Sender and receiver account numbers are identical        │
 * │ VAL-101  │ Account number not found in the account table            │
 * │ VAL-102  │ Account exists but is not in ACTIVE status               │
 * │ VAL-103  │ Customer record not found for the account's customer_id  │
 * │ VAL-104  │ Customer exists but is not in ACTIVE status              │
 * │ VAL-105  │ Customer KYC not verified (FLAGGED, not FAILED)          │
 * │ VAL-500  │ Unexpected exception during validation                   │
 * └──────────┴──────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author IISPL Bank Platform Team
 * @since 1.0
 */
public final class ValidationService {

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Validates the given incoming transaction through all three validation gates.
     *
     * <p>A single database connection is opened for the full validation lifecycle
     * and closed automatically via try-with-resources, regardless of outcome.
     *
     * <p>Any unhandled exception is caught and returned as a {@code FAILED}
     * transaction with error code {@code VAL-500}, preventing unchecked exceptions
     * from propagating up the settlement pipeline.
     *
     * @param txn the incoming transaction to validate; must not be {@code null}
     * @return a new transaction instance carrying the final {@link ProcessingStatus}
     *         and, on failure or flagging, a structured error message
     */
    public IncomingTransaction validate(IncomingTransaction txn) {

        try (Connection dbConnection = DatabaseConfig.getConnection()) {

            // Gate 1 — Basic field presence and business-rule checks (no DB required)
            IncomingTransaction basicValidationResult = basicValidation(txn);
            if (basicValidationResult.getProcessingStatus() == ProcessingStatus.FAILED) {
                return basicValidationResult;
            }

            // Gate 2 — Validate sender's account and customer record against the DB
            IncomingTransaction senderValidationResult =
                    validateAccountAndCustomer(dbConnection, txn, txn.getSenderAccount(), "SENDER");

            if (senderValidationResult.getProcessingStatus() != ProcessingStatus.VALIDATED) {
                return senderValidationResult;
            }

            // Gate 3 — Validate receiver's account and customer record against the DB
            IncomingTransaction receiverValidationResult =
                    validateAccountAndCustomer(dbConnection, txn, txn.getReceiverAccount(), "RECEIVER");

            if (receiverValidationResult.getProcessingStatus() != ProcessingStatus.VALIDATED) {
                return receiverValidationResult;
            }

            // All gates passed — mark the transaction as fully validated
            return txn.toBuilder()
                    .processingStatus(ProcessingStatus.VALIDATED)
                    .errorMessage(null)
                    .build();

        } catch (Exception unexpectedException) {
            // Catch-all: wrap infrastructure or unexpected errors with a VAL-500 code
            return txn.toBuilder()
                    .processingStatus(ProcessingStatus.FAILED)
                    .errorMessage("[VAL-500] " + unexpectedException.getMessage())
                    .build();
        }
    }


    // =========================================================================
    // Gate 1 — Basic Field Validation (No DB)
    // =========================================================================

    /**
     * Performs lightweight, in-memory field validation before any database call.
     *
     * <p>Checks performed (in order):
     * <ol>
     *   <li>Sender customer ID is present</li>
     *   <li>Receiver customer ID is present</li>
     *   <li>Sender account number is present</li>
     *   <li>Receiver account number is present</li>
     *   <li>Sender and receiver account numbers are not identical</li>
     * </ol>
     *
     * @param txn the transaction to inspect
     * @return a copy with {@code VALIDATED} status if all checks pass,
     *         or a copy with {@code FAILED} status and the relevant VAL-00x code
     */
    private IncomingTransaction basicValidation(IncomingTransaction txn) {

        if (isBlank(txn.getSenderCustomerId())) {
            return fail(txn, "[VAL-001] senderCustomerId missing");
        }

        if (isBlank(txn.getReceiverCustomerId())) {
            return fail(txn, "[VAL-002] receiverCustomerId missing");
        }

        if (isBlank(txn.getSenderAccount())) {
            return fail(txn, "[VAL-003] senderAccount missing");
        }

        if (isBlank(txn.getReceiverAccount())) {
            return fail(txn, "[VAL-004] receiverAccount missing");
        }

        if (txn.getSenderAccount().equalsIgnoreCase(txn.getReceiverAccount())) {
            return fail(txn, "[VAL-005] sender and receiver account cannot be same");
        }

        return txn.toBuilder()
                .processingStatus(ProcessingStatus.VALIDATED)
                .build();
    }


    // =========================================================================
    // Gate 2 & 3 — DB-Backed Account + Customer Validation
    // =========================================================================

    /**
     * Validates a single party (sender or receiver) by performing two sequential
     * database lookups: account status, then customer status and KYC.
     *
     * <p><b>Account lookup</b> ({@code account} table):
     * <ul>
     *   <li>Account must exist          — else VAL-101 FAILED</li>
     *   <li>Account status must be ACTIVE — else VAL-102 FAILED</li>
     * </ul>
     *
     * <p><b>Customer lookup</b> ({@code customer} table, using the {@code customer_id}
     * returned by the account query):
     * <ul>
     *   <li>Customer must exist             — else VAL-103 FAILED</li>
     *   <li>Customer status must be ACTIVE  — else VAL-104 FAILED</li>
     *   <li>KYC status must be VERIFIED     — else VAL-105 FLAGGED (not FAILED;
     *       transaction is held for manual review rather than rejected outright)</li>
     * </ul>
     *
     * @param dbConnection  active JDBC connection shared across all gate checks
     * @param txn           the original transaction (used as builder base)
     * @param accountNumber the account number of the party being validated
     * @param partyRole     label used in error messages; either {@code "SENDER"} or {@code "RECEIVER"}
     * @return a copy with {@code VALIDATED} status on full success,
     *         {@code FLAGGED} on KYC failure, or {@code FAILED} on any hard failure
     * @throws Exception if a JDBC error occurs (propagated to the caller's catch block)
     */
    private IncomingTransaction validateAccountAndCustomer(
            Connection dbConnection,
            IncomingTransaction txn,
            String accountNumber,
            String partyRole) throws Exception {

        // ── Step 1: Account lookup ────────────────────────────────────────────
        String accountQuery = "SELECT customer_id, account_status FROM account WHERE account_number = ?";

        String linkedCustomerId;
        String accountStatus;

        try (PreparedStatement accountStatement = dbConnection.prepareStatement(accountQuery)) {
            accountStatement.setString(1, accountNumber);
            ResultSet accountResult = accountStatement.executeQuery();

            if (!accountResult.next()) {
                return fail(txn, "[VAL-101] " + partyRole + " ACCOUNT NOT FOUND: " + accountNumber);
            }

            linkedCustomerId = accountResult.getString("customer_id");
            accountStatus    = accountResult.getString("account_status");
        }

        if (!"ACTIVE".equalsIgnoreCase(accountStatus)) {
            return fail(txn, "[VAL-102] " + partyRole + " ACCOUNT BLOCKED");
        }

        // ── Step 2: Customer lookup (using customer_id resolved from the account) ──
        String customerQuery = "SELECT kyc_status, customer_status FROM customer WHERE customer_id = ?";

        String kycStatus;
        String customerStatus;

        try (PreparedStatement customerStatement = dbConnection.prepareStatement(customerQuery)) {
            customerStatement.setString(1, linkedCustomerId);
            ResultSet customerResult = customerStatement.executeQuery();

            if (!customerResult.next()) {
                return fail(txn, "[VAL-103] " + partyRole + " CUSTOMER NOT FOUND");
            }

            kycStatus      = customerResult.getString("kyc_status");
            customerStatus = customerResult.getString("customer_status");
        }

        if (!"ACTIVE".equalsIgnoreCase(customerStatus)) {
            return fail(txn, "[VAL-104] " + partyRole + " CUSTOMER INACTIVE");
        }

        // ── Step 3: KYC check — soft flag rather than hard rejection ─────────
        // KYC failure does not block the transaction outright; it routes it to
        // manual review via the FLAGGED status so compliance can investigate.
        if (!"VERIFIED".equalsIgnoreCase(kycStatus)) {
            return txn.toBuilder()
                    .processingStatus(ProcessingStatus.FLAGGED)
                    .errorMessage("[VAL-105] " + partyRole + " KYC NOT VERIFIED")
                    .build();
        }

        // All checks passed for this party
        return txn.toBuilder()
                .processingStatus(ProcessingStatus.VALIDATED)
                .build();
    }


    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Convenience method that produces a {@code FAILED} copy of the given transaction
     * with the supplied error message, keeping all other fields unchanged.
     *
     * @param txn          the transaction to copy from
     * @param errorMessage structured error code and description (e.g., "[VAL-003] senderAccount missing")
     * @return a new transaction instance with {@code FAILED} status and the given error message
     */
    private IncomingTransaction fail(IncomingTransaction txn, String errorMessage) {
        return txn.toBuilder()
                .processingStatus(ProcessingStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Returns {@code true} if the given string is {@code null}, empty, or contains
     * only whitespace characters.
     *
     * @param value the string to test
     * @return {@code true} if blank; {@code false} otherwise
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
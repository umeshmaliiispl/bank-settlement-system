package com.iispl.exception;

import com.iispl.entity.IncomingTransaction;

/**
 * Exception for database insert failures with full transaction context.
 */
public class DatabaseInsertException extends RuntimeException {

    private final IncomingTransaction failedTransaction;

    public DatabaseInsertException(String message,
                                   Throwable cause,
                                   IncomingTransaction failedTransaction) {
        super(message, cause);
        this.failedTransaction = failedTransaction;
    }

    public IncomingTransaction getFailedTransaction() {
        return failedTransaction;
    }
}
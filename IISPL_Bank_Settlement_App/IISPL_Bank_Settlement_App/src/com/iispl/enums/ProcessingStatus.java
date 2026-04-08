package com.iispl.enums;


/**
 * ProcessingStatus — tracks the lifecycle of an IncomingTransaction
 * from first receipt through settlement.
 *
 *  RECEIVED    — Raw payload received from source, not yet parsed
 *  VALIDATED   — Parsed and passed all validation rules
 *  QUEUED      — Placed onto BlockingQueue, awaiting settlement processor
 *  PROCESSING  — Settlement processor has picked it up
 *  PROCESSED   — Successfully settled
 *  FAILED      — Validation or processing failed (can retry)
 *  DEAD_LETTER — Max retries exceeded; moved to dead-letter queue
 */


public enum ProcessingStatus {
    RECEIVED,
    VALIDATED,
    QUEUED,
    PROCESSING,
    PROCESSED,
    FAILED,
    DEAD_LETTER
}
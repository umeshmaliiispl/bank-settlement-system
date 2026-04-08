package com.iispl.exception;

import com.iispl.enums.SourceType;

/**
 * DuplicateTransactionException — Raised when the same sourceRef has already
 * been successfully ingested.
 *
 * Prevents double-settlement: the AdapterRegistry or ingestion pipeline should
 * maintain a Set/Cache of ingested refs and throw this before the adapter is
 * even called.
 */
public class DuplicateTransactionException extends IngestionException {

	private static final long serialVersionUID = 1L;

	private final String duplicateRef;

	public DuplicateTransactionException(SourceType sourceType, String duplicateRef, String rawPayload) {
		super(ERR_DUPLICATE, sourceType, rawPayload, "Transaction already ingested — ref: " + duplicateRef);
		this.duplicateRef = duplicateRef;
	}

	public String getDuplicateRef() {
		return duplicateRef;
	}
}
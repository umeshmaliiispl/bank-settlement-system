package com.iispl.exception;

import com.iispl.enums.SourceType;

/**
 * IngestionException — Thrown when a raw payload cannot be parsed or validated.
 *
 * Carries structured context so the dead-letter handler can log with precision
 * and the monitoring dashboard can categorise failure reasons.
 *
 * ERROR CODES (prefix ING-): ING-001 rawPayload is null or empty ING-002
 * mandatory field missing ING-003 field format invalid (wrong length, wrong
 * pattern, wrong type) ING-004 business rule violated (RTGS minimum, UPI
 * maximum, etc.) ING-005 duplicate transaction (same sourceRef already
 * ingested) ING-006 source system is inactive or DOWN — ingestion blocked
 * ING-007 daily / single-transaction limit exceeded
 */
public class IngestionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	// ── Error code constants ──────────────────────────────────────────────────
	public static final String ERR_NULL_PAYLOAD = "ING-001";
	public static final String ERR_MISSING_FIELD = "ING-002";
	public static final String ERR_INVALID_FORMAT = "ING-003";
	public static final String ERR_BUSINESS_RULE = "ING-004";
	public static final String ERR_DUPLICATE = "ING-005";
	public static final String ERR_SOURCE_INACTIVE = "ING-006";
	public static final String ERR_LIMIT_EXCEEDED = "ING-007";

	// ── Structured context fields ─────────────────────────────────────────────
	private final SourceType sourceType;
	private final String rawPayload;
	private final String errorCode;

	// ── Constructors ──────────────────────────────────────────────────────────

	public IngestionException(String errorCode, SourceType sourceType, String rawPayload, String message) {
		super("[" + errorCode + "][" + sourceType + "] " + message);
		this.errorCode = errorCode;
		this.sourceType = sourceType;
		this.rawPayload = rawPayload;
	}

	public IngestionException(String errorCode, SourceType sourceType, String rawPayload, String message,
			Throwable cause) {
		super("[" + errorCode + "][" + sourceType + "] " + message, cause);
		this.errorCode = errorCode;
		this.sourceType = sourceType;
		this.rawPayload = rawPayload;
	}

	// ── Accessors ─────────────────────────────────────────────────────────────

	public SourceType getSourceType() {
		return sourceType;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
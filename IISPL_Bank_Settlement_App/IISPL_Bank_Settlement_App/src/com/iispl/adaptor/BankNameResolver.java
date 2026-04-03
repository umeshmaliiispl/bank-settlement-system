package com.iispl.adaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * BankNameResolver — Utility to resolve bank names from IFSC codes and SWIFT
 * BICs.
 *
 * In production this would query an RBI-maintained master table. For this
 * pure-Java implementation, a static map covers all banks present in the
 * project's CSV test data.
 *
 * IFSC prefix (first 4 chars) → bank name BIC (first 4 chars) → bank name
 */

public final class BankNameResolver {

	private static final Map<String, String> IFSC_MAP;
	private static final Map<String, String> BIC_MAP;

	static {
		// IFSC prefix → full bank name (RBI bank codes)
		Map<String, String> ifsc = new HashMap<>();
		ifsc.put("SBIN", "State Bank of India");
		ifsc.put("HDFC", "HDFC Bank");
		ifsc.put("ICIC", "ICICI Bank");
		ifsc.put("AXIS", "Axis Bank");
		ifsc.put("PUNB", "Punjab National Bank");
		ifsc.put("BARB", "Bank of Baroda");
		ifsc.put("CNRB", "Canara Bank");
		ifsc.put("UBIN", "Union Bank of India");
		ifsc.put("BKID", "Bank of India");
		ifsc.put("UTIB", "Axis Bank"); // Axis alternate IFSC prefix
		ifsc.put("KKBK", "Kotak Mahindra Bank");
		ifsc.put("IOBA", "Indian Overseas Bank");
		ifsc.put("CORP", "Corporation Bank");
		ifsc.put("INDB", "IndusInd Bank");
		ifsc.put("YESB", "Yes Bank");
		ifsc.put("IISPL", "IISPL Bank"); // our own bank
		IFSC_MAP = Collections.unmodifiableMap(ifsc);

		// SWIFT BIC prefix (4 chars) → bank name
		Map<String, String> bic = new HashMap<>();
		bic.put("SBIN", "State Bank of India");
		bic.put("HDFC", "HDFC Bank");
		bic.put("ICIC", "ICICI Bank");
		bic.put("AXIS", "Axis Bank");
		bic.put("BARC", "Barclays Bank");
		bic.put("DEUT", "Deutsche Bank");
		bic.put("ANZB", "ANZ Bank Australia");
		bic.put("CITI", "Citibank");
		bic.put("HSBC", "HSBC Bank");
		bic.put("JPMO", "JPMorgan Chase");
		bic.put("BNPP", "BNP Paribas");
		bic.put("SOCG", "Société Générale");
		bic.put("UBSW", "UBS Switzerland");
		BIC_MAP = Collections.unmodifiableMap(bic);
	}

	private BankNameResolver() {
	}

	/**
	 * Resolve bank name from a full 11-char IFSC code. Uses first 4 chars as the
	 * bank identifier.
	 *
	 * @param ifsc e.g. "SBIN0001234"
	 * @return "State Bank of India" or "UNKNOWN-SBIN" if not found
	 */
	public static String fromIfsc(String ifsc) {
		if (ifsc == null || ifsc.length() < 4)
			return "UNKNOWN";
		String prefix = ifsc.substring(0, 4).toUpperCase();
		return IFSC_MAP.getOrDefault(prefix, "UNKNOWN-" + prefix);
	}

	/**
	 * Resolve bank name from an 8 or 11-char SWIFT BIC code. Uses first 4 chars as
	 * the institution identifier.
	 *
	 * @param bic e.g. "SBININBB" or "SBININBBXXX"
	 * @return "State Bank of India" or "UNKNOWN-SBIN" if not found
	 */
	public static String fromBic(String bic) {
		if (bic == null || bic.length() < 4)
			return "UNKNOWN";
		String prefix = bic.substring(0, 4).toUpperCase();
		return BIC_MAP.getOrDefault(prefix, "UNKNOWN-" + prefix);
	}
}
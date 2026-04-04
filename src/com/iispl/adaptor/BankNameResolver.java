package com.iispl.adaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for resolving bank names from IFSC and SWIFT (BIC) codes.
 *
 * <p>
 * In a real-world production system, this data would typically be sourced from
 * an RBI-maintained master database or external service.
 * </p>
 *
 * <p>
 * Current implementation uses in-memory static maps:
 * <ul>
 *   <li>IFSC prefix (first 4 characters) → Bank Name</li>
 *   <li>SWIFT/BIC prefix (first 4 characters) → Bank Name</li>
 * </ul>
 * </p>
 *
 * <p>
 * Example:
 * <pre>
 * IFSC: SBIN0001234 → State Bank of India
 * BIC : SBININBB    → State Bank of India
 * </pre>
 * </p>
 */
public final class BankNameResolver {

    /** Immutable map of IFSC prefixes to bank names */
    private static final Map<String, String> IFSC_MAP;

    /** Immutable map of SWIFT/BIC prefixes to bank names */
    private static final Map<String, String> BIC_MAP;

    // Static initialization block
    static {
        Map<String, String> ifscMap = new HashMap<>();

        // RBI IFSC prefixes
        ifscMap.put("SBIN", "State Bank of India");
        ifscMap.put("HDFC", "HDFC Bank");
        ifscMap.put("ICIC", "ICICI Bank");
        ifscMap.put("AXIS", "Axis Bank");
        ifscMap.put("PUNB", "Punjab National Bank");
        ifscMap.put("BARB", "Bank of Baroda");
        ifscMap.put("CNRB", "Canara Bank");
        ifscMap.put("UBIN", "Union Bank of India");
        ifscMap.put("BKID", "Bank of India");
        ifscMap.put("UTIB", "Axis Bank"); // Alternate prefix
        ifscMap.put("KKBK", "Kotak Mahindra Bank");
        ifscMap.put("IOBA", "Indian Overseas Bank");
        ifscMap.put("CORP", "Corporation Bank");
        ifscMap.put("INDB", "IndusInd Bank");
        ifscMap.put("YESB", "Yes Bank");
        ifscMap.put("IISPL", "IISPL Bank"); // Internal bank

        IFSC_MAP = Collections.unmodifiableMap(ifscMap);

        Map<String, String> bicMap = new HashMap<>();

        // SWIFT/BIC prefixes
        bicMap.put("SBIN", "State Bank of India");
        bicMap.put("HDFC", "HDFC Bank");
        bicMap.put("ICIC", "ICICI Bank");
        bicMap.put("AXIS", "Axis Bank");
        bicMap.put("BARC", "Barclays Bank");
        bicMap.put("DEUT", "Deutsche Bank");
        bicMap.put("ANZB", "ANZ Bank Australia");
        bicMap.put("CITI", "Citibank");
        bicMap.put("HSBC", "HSBC Bank");
        bicMap.put("JPMO", "JPMorgan Chase");
        bicMap.put("BNPP", "BNP Paribas");
        bicMap.put("SOCG", "Société Générale");
        bicMap.put("UBSW", "UBS Switzerland");

        BIC_MAP = Collections.unmodifiableMap(bicMap);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private BankNameResolver() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Resolves bank name from an IFSC code.
     *
     * @param ifsc Full IFSC code (e.g., "SBIN0001234")
     * @return Bank name if found, otherwise "UNKNOWN-XXXX"
     */
    public static String fromIfsc(String ifsc) {
        if (ifsc == null || ifsc.length() < 4) {
            return "UNKNOWN";
        }

        String prefix = extractPrefix(ifsc);
        return IFSC_MAP.getOrDefault(prefix, "UNKNOWN-" + prefix);
    }

    /**
     * Resolves bank name from a SWIFT/BIC code.
     *
     * @param bic SWIFT/BIC code (e.g., "SBININBB" or "SBININBBXXX")
     * @return Bank name if found, otherwise "UNKNOWN-XXXX"
     */
    public static String fromBic(String bic) {
        if (bic == null || bic.length() < 4) {
            return "UNKNOWN";
        }

        String prefix = extractPrefix(bic);
        return BIC_MAP.getOrDefault(prefix, "UNKNOWN-" + prefix);
    }

    /**
     * Extracts and normalizes the first 4-character prefix.
     *
     * @param code IFSC or BIC code
     * @return Uppercase 4-character prefix
     */
    private static String extractPrefix(String code) {
        return code.substring(0, 4).toUpperCase();
    }
}




//package com.iispl.adaptor;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * BankNameResolver — Utility to resolve bank names from IFSC codes and SWIFT
// * BICs.
// *
// * In production this would query an RBI-maintained master table. For this
// * pure-Java implementation, a static map covers all banks present in the
// * project's CSV test data.
// *
// * IFSC prefix (first 4 chars) → bank name BIC (first 4 chars) → bank name
// */
//
//public final class BankNameResolver {
//
//	private static final Map<String, String> IFSC_MAP;
//	private static final Map<String, String> BIC_MAP;
//
//	static {
//		// IFSC prefix → full bank name (RBI bank codes)
//		Map<String, String> ifsc = new HashMap<>();
//		ifsc.put("SBIN", "State Bank of India");
//		ifsc.put("HDFC", "HDFC Bank");
//		ifsc.put("ICIC", "ICICI Bank");
//		ifsc.put("AXIS", "Axis Bank");
//		ifsc.put("PUNB", "Punjab National Bank");
//		ifsc.put("BARB", "Bank of Baroda");
//		ifsc.put("CNRB", "Canara Bank");
//		ifsc.put("UBIN", "Union Bank of India");
//		ifsc.put("BKID", "Bank of India");
//		ifsc.put("UTIB", "Axis Bank"); // Axis alternate IFSC prefix
//		ifsc.put("KKBK", "Kotak Mahindra Bank");
//		ifsc.put("IOBA", "Indian Overseas Bank");
//		ifsc.put("CORP", "Corporation Bank");
//		ifsc.put("INDB", "IndusInd Bank");
//		ifsc.put("YESB", "Yes Bank");
//		ifsc.put("IISPL", "IISPL Bank"); // our own bank
//		IFSC_MAP = Collections.unmodifiableMap(ifsc);
//
//		// SWIFT BIC prefix (4 chars) → bank name
//		Map<String, String> bic = new HashMap<>();
//		bic.put("SBIN", "State Bank of India");
//		bic.put("HDFC", "HDFC Bank");
//		bic.put("ICIC", "ICICI Bank");
//		bic.put("AXIS", "Axis Bank");
//		bic.put("BARC", "Barclays Bank");
//		bic.put("DEUT", "Deutsche Bank");
//		bic.put("ANZB", "ANZ Bank Australia");
//		bic.put("CITI", "Citibank");
//		bic.put("HSBC", "HSBC Bank");
//		bic.put("JPMO", "JPMorgan Chase");
//		bic.put("BNPP", "BNP Paribas");
//		bic.put("SOCG", "Société Générale");
//		bic.put("UBSW", "UBS Switzerland");
//		BIC_MAP = Collections.unmodifiableMap(bic);
//	}
//
//	private BankNameResolver() {
//	}
//
//	/**
//	 * Resolve bank name from a full 11-char IFSC code. Uses first 4 chars as the
//	 * bank identifier.
//	 *
//	 * @param ifsc e.g. "SBIN0001234"
//	 * @return "State Bank of India" or "UNKNOWN-SBIN" if not found
//	 */
//	public static String fromIfsc(String ifsc) {
//		if (ifsc == null || ifsc.length() < 4)
//			return "UNKNOWN";
//		String prefix = ifsc.substring(0, 4).toUpperCase();
//		return IFSC_MAP.getOrDefault(prefix, "UNKNOWN-" + prefix);
//	}
//
//	/**
//	 * Resolve bank name from an 8 or 11-char SWIFT BIC code. Uses first 4 chars as
//	 * the institution identifier.
//	 * @param bic e.g. "SBININBB" or "SBININBBXXX"
//	 * @return "State Bank of India" or "UNKNOWN-SBIN" if not found
//	 */
//	public static String fromBic(String bic) {
//		if (bic == null || bic.length() < 4)
//			return "UNKNOWN";
//		String prefix = bic.substring(0, 4).toUpperCase();
//		return BIC_MAP.getOrDefault(prefix, "UNKNOWN-" + prefix);
//	}
//}
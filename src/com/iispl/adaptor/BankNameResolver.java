package com.iispl.adaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for resolving bank names from IFSC and SWIFT (BIC) codes.
 */
public final class BankNameResolver {

    /** Immutable map of IFSC prefixes to bank names */
    private static final Map<String, String> ifscPrefixToBankNameMap;

    /** Immutable map of SWIFT/BIC prefixes to bank names */
    private static final Map<String, String> bicPrefixToBankNameMap;

    // Static initialization block
    static {
        Map<String, String> mutableIfscPrefixMap = new HashMap<>();

        // RBI IFSC prefixes
        mutableIfscPrefixMap.put("SBIN", "State Bank of India");
        mutableIfscPrefixMap.put("HDFC", "HDFC Bank");
        mutableIfscPrefixMap.put("ICIC", "ICICI Bank");
        mutableIfscPrefixMap.put("AXIS", "Axis Bank");
        mutableIfscPrefixMap.put("PUNB", "Punjab National Bank");
        mutableIfscPrefixMap.put("BARB", "Bank of Baroda");
        mutableIfscPrefixMap.put("CNRB", "Canara Bank");
        mutableIfscPrefixMap.put("UBIN", "Union Bank of India");
        mutableIfscPrefixMap.put("BKID", "Bank of India");
        mutableIfscPrefixMap.put("UTIB", "Axis Bank"); // Alternate prefix
        mutableIfscPrefixMap.put("KKBK", "Kotak Mahindra Bank");
        mutableIfscPrefixMap.put("IOBA", "Indian Overseas Bank");
        mutableIfscPrefixMap.put("CORP", "Corporation Bank");
        mutableIfscPrefixMap.put("INDB", "IndusInd Bank");
        mutableIfscPrefixMap.put("YESB", "Yes Bank");
        mutableIfscPrefixMap.put("IISPL", "IISPL Bank"); // Internal bank

        ifscPrefixToBankNameMap = Collections.unmodifiableMap(mutableIfscPrefixMap);

        Map<String, String> mutableBicPrefixMap = new HashMap<>();

        // SWIFT/BIC prefixes
        mutableBicPrefixMap.put("SBIN", "State Bank of India");
        mutableBicPrefixMap.put("HDFC", "HDFC Bank");
        mutableBicPrefixMap.put("ICIC", "ICICI Bank");
        mutableBicPrefixMap.put("AXIS", "Axis Bank");
        mutableBicPrefixMap.put("BARC", "Barclays Bank");
        mutableBicPrefixMap.put("DEUT", "Deutsche Bank");
        mutableBicPrefixMap.put("ANZB", "ANZ Bank Australia");
        mutableBicPrefixMap.put("CITI", "Citibank");
        mutableBicPrefixMap.put("HSBC", "HSBC Bank");
        mutableBicPrefixMap.put("JPMO", "JPMorgan Chase");
        mutableBicPrefixMap.put("BNPP", "BNP Paribas");
        mutableBicPrefixMap.put("SOCG", "Société Générale");
        mutableBicPrefixMap.put("UBSW", "UBS Switzerland");

        bicPrefixToBankNameMap = Collections.unmodifiableMap(mutableBicPrefixMap);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private BankNameResolver() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Resolves bank name from an IFSC code.
     */
    public static String fromIfsc(String ifscCode) {
        if (ifscCode == null || ifscCode.length() < 4) {
            return "UNKNOWN";
        }

        String prefixCode = extractPrefix(ifscCode);
        return ifscPrefixToBankNameMap.getOrDefault(prefixCode, "UNKNOWN-" + prefixCode);
    }

    /**
     * Resolves bank name from a SWIFT/BIC code.
     */
    public static String fromBic(String bicCode) {
        if (bicCode == null || bicCode.length() < 4) {
            return "UNKNOWN";
        }

        String prefixCode = extractPrefix(bicCode);
        return bicPrefixToBankNameMap.getOrDefault(prefixCode, "UNKNOWN-" + prefixCode);
    }

    /**
     * Extracts and normalizes the first 4-character prefix.
     */
    private static String extractPrefix(String fullCode) {
        return fullCode.substring(0, 4).toUpperCase();
    }
}
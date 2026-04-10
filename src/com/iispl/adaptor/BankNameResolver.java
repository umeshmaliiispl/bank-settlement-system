
package com.iispl.adaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for resolving bank names from IFSC and SWIFT (BIC) codes.
 *
 * <p>Covers all major RBI-regulated Indian banks, cooperative banks,
 * small finance banks, payment banks, and international SWIFT/BIC codes
 * relevant to IISPL's transaction processing scope.
 *
 * <p>IFSC lookup uses the first 4 characters (standard RBI convention).
 * Exception: IISPL internal accounts use prefix "IISP" (first 4 of "IISPL").
 */
public final class BankNameResolver {

    /** Immutable map of IFSC prefixes (4-char) to bank names */
    private static final Map<String, String> ifscPrefixToBankNameMap;

    /** Immutable map of SWIFT/BIC prefixes (4-char) to bank names */
    private static final Map<String, String> bicPrefixToBankNameMap;

    static {
        Map<String, String> ifsc = new HashMap<>();

        // ── Public Sector Banks ─────────────────────────────────────────────
        ifsc.put("SBIN", "State Bank of India");
        ifsc.put("BKID", "Bank of India");
        ifsc.put("BARB", "Bank of Baroda");
        ifsc.put("PUNB", "Punjab National Bank");
        ifsc.put("CNRB", "Canara Bank");
        ifsc.put("UBIN", "Union Bank of India");
        ifsc.put("IOBA", "Indian Overseas Bank");
        ifsc.put("UCBA", "UCO Bank");
        ifsc.put("BODB", "Bank of Maharashtra");
        ifsc.put("PSIB", "Punjab & Sind Bank");
        ifsc.put("CORP", "Corporation Bank");           // merged into Union Bank
        ifsc.put("ANDB", "Andhra Bank");                // merged into Union Bank
        ifsc.put("ALLA", "Allahabad Bank");             // merged into Indian Bank
        ifsc.put("IDIB", "Indian Bank");
        ifsc.put("CBIN", "Central Bank of India");
        ifsc.put("VIJB", "Vijaya Bank");                // merged into Bank of Baroda

        // ── Major Private Sector Banks ──────────────────────────────────────
        ifsc.put("HDFC", "HDFC Bank");
        ifsc.put("ICIC", "ICICI Bank");
        ifsc.put("AXIS", "Axis Bank");
        ifsc.put("UTIB", "Axis Bank");                  // alternate IFSC prefix
        ifsc.put("KKBK", "Kotak Mahindra Bank");
        ifsc.put("YESB", "Yes Bank");
        ifsc.put("INDB", "IndusInd Bank");
        ifsc.put("IDFB", "IDFC FIRST Bank");
        ifsc.put("FDRL", "Federal Bank");
        ifsc.put("KVBL", "Karur Vysya Bank");
        ifsc.put("SIBL", "South Indian Bank");
        ifsc.put("LAVB", "Lakshmi Vilas Bank");         // merged into DBS India
        ifsc.put("DBSS", "DBS Bank India");
        ifsc.put("DCBL", "DCB Bank");
        ifsc.put("KARB", "Karnataka Bank");
        ifsc.put("NKGS", "NKGSB Cooperative Bank");
        ifsc.put("JAKA", "Jammu & Kashmir Bank");
        ifsc.put("CSBK", "CSB Bank");
        ifsc.put("DLXB", "Dhanlaxmi Bank");
        ifsc.put("RBLB", "RBL Bank");
        ifsc.put("RATN", "RBL Bank");                   // alternate prefix
        ifsc.put("TMBL", "Tamilnad Mercantile Bank");
        ifsc.put("NSPB", "Nainital Bank");
        ifsc.put("SRCB", "Saraswat Cooperative Bank");

        // ── Small Finance Banks ─────────────────────────────────────────────
        ifsc.put("AUBL", "AU Small Finance Bank");
        ifsc.put("ESAF", "ESAF Small Finance Bank");
        ifsc.put("ESFB", "Equitas Small Finance Bank");
        ifsc.put("FINF", "Fincare Small Finance Bank");
        ifsc.put("JANA", "Jana Small Finance Bank");
        ifsc.put("NESF", "North East Small Finance Bank");
        ifsc.put("SUBL", "Suryoday Small Finance Bank");
        ifsc.put("UFSB", "Ujjivan Small Finance Bank");
        ifsc.put("UTKS", "Utkarsh Small Finance Bank");

        // ── Payment Banks ───────────────────────────────────────────────────
        ifsc.put("AIRP", "Airtel Payments Bank");
        ifsc.put("FINO", "Fino Payments Bank");
        ifsc.put("IPOS", "India Post Payments Bank");
        ifsc.put("JSFB", "Jio Payments Bank");
        ifsc.put("PAYTM", "Paytm Payments Bank");       // 5-char; stored as PAYT
        ifsc.put("PAYT", "Paytm Payments Bank");
        ifsc.put("NSDL", "NSDL Payments Bank");

        // ── Foreign Banks Operating in India ───────────────────────────────
        ifsc.put("CITI", "Citibank India");
        ifsc.put("HSBC", "HSBC India");
        ifsc.put("DEUT", "Deutsche Bank India");
        ifsc.put("SCBL", "Standard Chartered Bank India");
        ifsc.put("BARC", "Barclays Bank India");
        ifsc.put("BNPP", "BNP Paribas India");
        ifsc.put("DBSS", "DBS Bank India");
        ifsc.put("ABNA", "ABN AMRO / RBS India");
        ifsc.put("ANZB", "ANZ Bank India");
        ifsc.put("BOFA", "Bank of America India");
        ifsc.put("JPMO", "JPMorgan Chase India");
        ifsc.put("MHCB", "Mizuho Bank India");
        ifsc.put("SMBC", "Sumitomo Mitsui Banking India");
        ifsc.put("BOFS", "Bank of Scotland India");
        ifsc.put("CHAS", "JPMorgan Chase India");       // alternate

        // ── RBI / Clearing / Internal ───────────────────────────────────────
        ifsc.put("RBIS", "Reserve Bank of India");
        ifsc.put("NPCI", "NPCI");
        ifsc.put("IISP", "IISPL Internal");             // "IISPL".substring(0,4)

        ifscPrefixToBankNameMap = Collections.unmodifiableMap(ifsc);

        // ── SWIFT / BIC Map ─────────────────────────────────────────────────
        Map<String, String> bic = new HashMap<>();

        // Indian banks (international BICs)
        bic.put("SBIN", "State Bank of India");
        bic.put("HDFC", "HDFC Bank");
        bic.put("ICIC", "ICICI Bank");
        bic.put("AXIS", "Axis Bank");
        bic.put("KKBK", "Kotak Mahindra Bank");
        bic.put("PUNB", "Punjab National Bank");
        bic.put("BARB", "Bank of Baroda");
        bic.put("CNRB", "Canara Bank");
        bic.put("UBIN", "Union Bank of India");
        bic.put("BKID", "Bank of India");
        bic.put("YESB", "Yes Bank");
        bic.put("INDB", "IndusInd Bank");
        bic.put("IDFB", "IDFC FIRST Bank");
        bic.put("FDRL", "Federal Bank");
        bic.put("UTIB", "Axis Bank");

        // Global correspondent / international banks
        bic.put("CITI", "Citibank");
        bic.put("HSBC", "HSBC Bank");
        bic.put("DEUT", "Deutsche Bank");
        bic.put("BARC", "Barclays Bank");
        bic.put("BNPP", "BNP Paribas");
        bic.put("SOGE", "Société Générale");
        bic.put("UBSW", "UBS Switzerland");
        bic.put("CRED", "Credit Suisse");
        bic.put("DBSS", "DBS Bank");
        bic.put("ANZB", "ANZ Bank");
        bic.put("WPAC", "Westpac Banking Corp");
        bic.put("NWBK", "NatWest Bank");
        bic.put("ABNA", "ABN AMRO Bank");
        bic.put("INGB", "ING Bank");
        bic.put("RABO", "Rabobank");
        bic.put("BNPA", "BNP Paribas");                // alternate BIC prefix
        bic.put("CHAS", "JPMorgan Chase");
        bic.put("JPMO", "JPMorgan Chase");
        bic.put("BOFA", "Bank of America");
        bic.put("WELLS", "Wells Fargo");
        bic.put("WELL", "Wells Fargo");
        bic.put("MHCB", "Mizuho Bank");
        bic.put("SMBC", "Sumitomo Mitsui Banking Corp");
        bic.put("TOKY", "MUFG Bank (Tokyo)");
        bic.put("SCBL", "Standard Chartered Bank");
        bic.put("RBOS", "Royal Bank of Scotland");
        bic.put("LOYD", "Lloyds Bank");
        bic.put("MIDL", "HSBC (Midland)");
        bic.put("BOFS", "Bank of Scotland");
        bic.put("SOCY", "Société Générale");
        bic.put("KCBL", "KBC Bank");
        bic.put("BPOT", "Banco BPI");
        bic.put("BBVA", "BBVA Bank");
        bic.put("CECA", "CaixaBank");
        bic.put("IISP", "IISPL Internal");

        bicPrefixToBankNameMap = Collections.unmodifiableMap(bic);
    }

    private BankNameResolver() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Resolves bank name from an IFSC code.
     * Uses the first 4 characters as the lookup key (RBI standard).
     *
     * @param ifscCode full IFSC code (e.g. "SBIN0004321")
     * @return resolved bank name, or "UNKNOWN-{PREFIX}" if unrecognized
     */
    public static String fromIfsc(String ifscCode) {
        if (ifscCode == null || ifscCode.trim().isEmpty()) {
            return "UNKNOWN";
        }
        if (ifscCode.length() < 4) {
            return "UNKNOWN-" + ifscCode.toUpperCase();
        }
        String prefix = extractPrefix(ifscCode);
        return ifscPrefixToBankNameMap.getOrDefault(prefix, "UNKNOWN-" + prefix);
    }

    /**
     * Resolves bank name from a SWIFT/BIC code.
     * Uses the first 4 characters (institution code portion of BIC).
     *
     * @param bicCode full BIC/SWIFT code (e.g. "SBININBB")
     * @return resolved bank name, or "UNKNOWN-{PREFIX}" if unrecognized
     */
    public static String fromBic(String bicCode) {
        if (bicCode == null || bicCode.trim().isEmpty()) {
            return "UNKNOWN";
        }
        if (bicCode.length() < 4) {
            return "UNKNOWN-" + bicCode.toUpperCase();
        }
        String prefix = extractPrefix(bicCode);
        return bicPrefixToBankNameMap.getOrDefault(prefix, "UNKNOWN-" + prefix);
    }

    /**
     * Extracts and normalizes the first 4-character prefix from any bank code.
     */
    private static String extractPrefix(String fullCode) {
        return fullCode.substring(0, 4).toUpperCase();
    }
}


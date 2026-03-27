package com.iispl.intefaces;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;

/**
 * TransactionAdapter — Strategy Pattern interface.
 *
 * Each source system has its own adapter class that converts
 * raw payload → canonical IncomingTransaction.
 *
 *  SOURCE SYSTEM          ADAPTER CLASS
 *  ─────────────────      ─────────────────────
 *  CBS  (Core Banking)  → CbsAdapter
 *  RTGS (Gross Settle)  → RtgsAdapter
 *  SWIFT (Cross-border) → SwiftAdapter
 *  NEFT/UPI (Domestic)  → NeftUpiAdapter
 *  Fintech API          → FintechAdapter
 */
public interface TransactionAdapter {
    IncomingTransaction adapt(String rawPayload);
    SourceType getSourceType();
}

package com.iispl.adaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.intefaces.TransactionAdapter;

public final class AdapterRegistry {

    private static final AdapterRegistry adapterRegistryInstance = new AdapterRegistry();

    public static AdapterRegistry getInstance() {
        return adapterRegistryInstance;
    }

    // Internal state
    // Immutable after constructor completes — safe for concurrent read
    private final Map<SourceType, TransactionAdapter> sourceTypeToAdapterMap;

    // Constructor — registers all adapters
    private AdapterRegistry() {
        Map<SourceType, TransactionAdapter> adapterMapping = new HashMap<>();

        // Register CBS adapter
        registerAdapter(adapterMapping, new CbsAdapter());

        // Register RTGS adapter
        registerAdapter(adapterMapping, new RtgsAdapter());

        // Register SWIFT adapter
        registerAdapter(adapterMapping, new SwiftAdapter());

        // NeftUpiAdapter handles BOTH NEFT and UPI (same txt format, same adapter)
        NeftUpiAdapter neftUpiTransactionAdapter = new NeftUpiAdapter();
        adapterMapping.put(SourceType.NEFT, neftUpiTransactionAdapter);
        adapterMapping.put(SourceType.UPI, neftUpiTransactionAdapter);

        // Register Fintech adapter
        registerAdapter(adapterMapping, new FintechAdapter());

        // Freeze — no further modifications
        this.sourceTypeToAdapterMap = Collections.unmodifiableMap(adapterMapping);

    }

    /**
     * Route a raw payload to the correct adapter and return a canonical
     * IncomingTransaction.
     *
     * @param sourceType which system sent the payload
     * @param rawPayload raw wire string
     * @return fully parsed and validated IncomingTransaction
     * @throws IllegalArgumentException if no adapter is registered for sourceType
     */
    public IncomingTransaction adapt(SourceType sourceType, String rawPayload) {
        TransactionAdapter transactionAdapter = sourceTypeToAdapterMap.get(sourceType);

        if (transactionAdapter == null) {
            throw new IllegalArgumentException(
                    "[AdapterRegistry] No adapter registered for SourceType: " + sourceType
                            + ". Registered types: " + sourceTypeToAdapterMap.keySet()
            );
        }

        return transactionAdapter.adapt(rawPayload);
    }

    /**
     * Retrieve the adapter directly (for testing or manual invocation).
     */
    public TransactionAdapter getAdapter(SourceType sourceType) {
        TransactionAdapter transactionAdapter = sourceTypeToAdapterMap.get(sourceType);

        if (transactionAdapter == null) {
            throw new IllegalArgumentException("[AdapterRegistry] No adapter for: " + sourceType);
        }

        return transactionAdapter;
    }

    /** Returns true if an adapter is registered for this SourceType. */
    public boolean hasAdapter(SourceType sourceType) {
        return sourceTypeToAdapterMap.containsKey(sourceType);
    }

    // Read-only view of the full routing table (for health checks / monitoring).
    public Map<SourceType, TransactionAdapter> getAllAdapters() {
        return sourceTypeToAdapterMap;
    }

    private void registerAdapter(Map<SourceType, TransactionAdapter> adapterMapping,
                                 TransactionAdapter transactionAdapter) {
        adapterMapping.put(transactionAdapter.getSourceType(), transactionAdapter);
    }
}
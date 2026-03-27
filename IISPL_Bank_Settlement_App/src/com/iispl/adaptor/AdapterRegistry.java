package com.iispl.adaptor;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.intefaces.TransactionAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * AdapterRegistry — Central registry that maps SourceType → TransactionAdapter.
 *
 * This is the heart of the Adapter Pattern implementation.
 *
 * HOW IT WORKS:
 *   1. At startup, all 5 adapters are registered into a HashMap.
 *   2. When a raw payload arrives, the caller provides its SourceType.
 *   3. Registry looks up the correct adapter and calls adapt().
 *   4. A canonical IncomingTransaction is returned — pipeline continues.
 *
 * ADDING A NEW SOURCE SYSTEM (e.g. ACH):
 *   Step 1: Create AchAdapter implements TransactionAdapter
 *   Step 2: Add one line here: register(new AchAdapter());
 *   Step 3: Done. Zero changes to pipeline, settlement, or concurrency code.
 *
 * DESIGN PATTERN: Strategy + Registry (also called "Plugin Registry")
 *
 * ─────────────────────────────────────────────────────────────────
 *   SourceType    │  Adapter Class     │  Protocol
 *   ──────────────┼────────────────────┼────────────────────
 *   CBS           │  CbsAdapter        │  DIRECT_DB
 *   RTGS          │  RtgsAdapter       │  MESSAGE_QUEUE
 *   SWIFT         │  SwiftAdapter      │  MESSAGE_QUEUE
 *   NEFT          │  NeftUpiAdapter    │  FLAT_FILE
 *   UPI           │  NeftUpiAdapter    │  REST_API
 *   FINTECH       │  FintechAdapter    │  REST_API
 * ─────────────────────────────────────────────────────────────────
 */
public class AdapterRegistry {

    // Immutable after initialization — thread-safe for concurrent reads
    private final Map<SourceType, TransactionAdapter> registry;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final AdapterRegistry INSTANCE = new AdapterRegistry();

    public static AdapterRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor — registers all adapters at startup.
     */
    private AdapterRegistry() {
        Map<SourceType, TransactionAdapter> map = new HashMap<>();

        // Register all 5 source adapters
        register(map, new CbsAdapter());
        register(map, new RtgsAdapter());
        register(map, new SwiftAdapter());
        register(map, new FintechAdapter());

        // NeftUpiAdapter handles BOTH NEFT and UPI
        NeftUpiAdapter neftUpiAdapter = new NeftUpiAdapter();
        map.put(SourceType.NEFT,    neftUpiAdapter);
        map.put(SourceType.UPI,     neftUpiAdapter);

        this.registry = Collections.unmodifiableMap(map);

        System.out.println("[AdapterRegistry] Initialized with " + registry.size() + " adapters:");
        registry.forEach((k, v) ->
            System.out.println("    " + k + "  →  " + v.getClass().getSimpleName())
        );
    }

    private void register(Map<SourceType, TransactionAdapter> map, TransactionAdapter adapter) {
        map.put(adapter.getSourceType(), adapter);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Look up adapter for a SourceType and adapt the raw payload.
     *
     * This is the MAIN entry point used by IngestionWorker (Member 4's threading layer).
     *
     * @param sourceType  which system sent this payload (CBS / RTGS / SWIFT etc.)
     * @param rawPayload  raw string payload from that source system
     * @return            canonical IncomingTransaction ready for BlockingQueue
     *
     * @throws IllegalArgumentException if no adapter is registered for sourceType
     */
    public IncomingTransaction adapt(SourceType sourceType, String rawPayload) {
        TransactionAdapter adapter = registry.get(sourceType);

        if (adapter == null) {
            throw new IllegalArgumentException(
                "[AdapterRegistry] No adapter registered for SourceType: " + sourceType
                + ". Registered types: " + registry.keySet()
            );
        }

        System.out.println("[AdapterRegistry] Routing " + sourceType + " → " + adapter.getClass().getSimpleName());
        return adapter.adapt(rawPayload);
    }

    /**
     * Get the adapter directly (for testing or manual use).
     */
    public TransactionAdapter getAdapter(SourceType sourceType) {
        TransactionAdapter adapter = registry.get(sourceType);
        if (adapter == null) {
            throw new IllegalArgumentException(
                "[AdapterRegistry] No adapter for: " + sourceType
            );
        }
        return adapter;
    }

    /**
     * Check if an adapter is registered for a given SourceType.
     */
    public boolean hasAdapter(SourceType sourceType) {
        return registry.containsKey(sourceType);
    }

    /**
     * Returns read-only view of all registered adapters.
     */
    public Map<SourceType, TransactionAdapter> getAllAdapters() {
        return registry;
    }
}

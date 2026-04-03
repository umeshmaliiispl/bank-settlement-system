package com.iispl.adaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.intefaces.TransactionAdapter;

/**
 * AdapterRegistry — Central routing registry for all TransactionAdapters.
 *
 * Implements the Strategy + Registry pattern:
 *   - At startup, all adapters self-register into an immutable Map
 *   - At runtime, the pipeline provides a SourceType and raw payload
 *   - Registry looks up the correct adapter and delegates adapt()
 *
 * ─────────────────────────────────────────────────────────────────────────
 *  ROUTING TABLE (built at startup)
 *  ─────────────────────────────────────────────────────────────────────────
 *  SourceType   │ Adapter Class     │ Wire Format
 *  ─────────────┼───────────────────┼──────────────────────────────────────
 *  CBS          │ CbsAdapter        │ Pipe-delimited (7 fields)
 *  RTGS         │ RtgsAdapter       │ JSON (MQ message)
 *  SWIFT        │ SwiftAdapter      │ MT103 multi-line tagged
 *  NEFT         │ NeftUpiAdapter    │ CSV (7 columns, NPCI batch)
 *  UPI          │ NeftUpiAdapter    │ CSV (same format, real-time push)
 *  FINTECH      │ FintechAdapter    │ JSON (webhook POST body)
 * ─────────────────────────────────────────────────────────────────────────
 *
 * THREAD SAFETY:
 *   The registry Map is wrapped in Collections.unmodifiableMap() after init.
 *   All reads are concurrent-safe. No locking needed.
 *
 * HOW TO ADD A NEW SOURCE SYSTEM (e.g. ACH):
 *   Step 1 — Create  AchAdapter  implements TransactionAdapter
 *   Step 2 — Add SourceType.ACH to the SourceType enum
 *   Step 3 — Add one line in the private constructor: register(map, new AchAdapter())
 *   Step 4 — Done. Zero changes anywhere else in the pipeline.
 *
 * SINGLETON:
 *   Eagerly initialized — safe, fast, no double-checked locking needed.
 */
public final class AdapterRegistry {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final AdapterRegistry INSTANCE = new AdapterRegistry();

    public static AdapterRegistry getInstance() { return INSTANCE; }

    // ── Internal state ────────────────────────────────────────────────────────
    // Immutable after constructor completes — safe for concurrent read
    private final Map<SourceType, TransactionAdapter> registry;

    // ── Constructor — registers all adapters ──────────────────────────────────

    private AdapterRegistry() {
        Map<SourceType, TransactionAdapter> map = new HashMap<>();

        // Register CBS adapter
        register(map, new CbsAdapter());

        // Register RTGS adapter
        register(map, new RtgsAdapter());

        // Register SWIFT adapter
        register(map, new SwiftAdapter());

        // NeftUpiAdapter handles BOTH NEFT and UPI (same CSV format, same adapter)
        NeftUpiAdapter neftUpiAdapter = new NeftUpiAdapter();
        map.put(SourceType.NEFT, neftUpiAdapter);
        map.put(SourceType.UPI,  neftUpiAdapter);

        // Register Fintech adapter
        register(map, new FintechAdapter());

        // Freeze — no further modifications
        this.registry = Collections.unmodifiableMap(map);

        // Startup banner
        System.out.println();
        System.out.println("  [AdapterRegistry] Initialized with "
                          + registry.size() + " entries:");
        registry.forEach((k, v) ->
            System.out.printf("    %-10s → %s%n", k, v.getClass().getSimpleName()));
        System.out.println();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Route a raw payload to the correct adapter and return a canonical
     * IncomingTransaction. This is the ONLY entry point used by the
     * IngestionPipelineRunner and (later) by IngestionWorker threads.
     *
     * @param sourceType  which system sent the payload
     * @param rawPayload  raw wire string
     * @return            fully parsed and validated IncomingTransaction
     * @throws IllegalArgumentException if no adapter is registered for sourceType
     */
    public IncomingTransaction adapt(SourceType sourceType, String rawPayload) {
        TransactionAdapter adapter = registry.get(sourceType);
        if (adapter == null)
            throw new IllegalArgumentException(
                "[AdapterRegistry] No adapter registered for SourceType: " + sourceType
                + ". Registered types: " + registry.keySet());

        return adapter.adapt(rawPayload);
    }

    /**
     * Retrieve the adapter directly (for testing or manual invocation).
     */
    public TransactionAdapter getAdapter(SourceType sourceType) {
        TransactionAdapter adapter = registry.get(sourceType);
        if (adapter == null)
            throw new IllegalArgumentException(
                "[AdapterRegistry] No adapter for: " + sourceType);
        return adapter;
    }

    /** Returns true if an adapter is registered for this SourceType. */
    public boolean hasAdapter(SourceType sourceType) {
        return registry.containsKey(sourceType);
    }

    /** Read-only view of the full routing table (for health checks / monitoring). */
    public Map<SourceType, TransactionAdapter> getAllAdapters() {
        return registry;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void register(Map<SourceType, TransactionAdapter> map, TransactionAdapter adapter) {
        map.put(adapter.getSourceType(), adapter);
    }
}
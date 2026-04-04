package com.iispl.adaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iispl.entity.IncomingTransaction;
import com.iispl.enums.SourceType;
import com.iispl.intefaces.TransactionAdapter;

public final class AdapterRegistry {

	private static final AdapterRegistry INSTANCE = new AdapterRegistry();

	public static AdapterRegistry getInstance() {
		return INSTANCE;
	}

	// Internal state
	// Immutable after constructor completes — safe for concurrent read
	private final Map<SourceType, TransactionAdapter> registry;

	// Constructor — registers all adapters
	private AdapterRegistry() {
		Map<SourceType, TransactionAdapter> map = new HashMap<>();

		// Register CBS adapter
		register(map, new CbsAdapter());

		// Register RTGS adapter
		register(map, new RtgsAdapter());

		// Register SWIFT adapter
		register(map, new SwiftAdapter());

		// NeftUpiAdapter handles BOTH NEFT and UPI (same txt format, same adapter)
		NeftUpiAdapter neftUpiAdapter = new NeftUpiAdapter();
		map.put(SourceType.NEFT, neftUpiAdapter);
		map.put(SourceType.UPI, neftUpiAdapter);

		// Register Fintech adapter
		register(map, new FintechAdapter());

		// Freeze — no further modifications
		this.registry = Collections.unmodifiableMap(map);

//        // Startup banner
//        System.out.println();
//        System.out.println("  [AdapterRegistry] Initialized with "
//                          + registry.size() + " entries:");
//        registry.forEach((k, v) ->
//            System.out.printf("    %-10s ->  %s%n", k, v.getClass().getSimpleName()));
//        System.out.println();

	}

	// Public API
	/**
	 * Route a raw payload to the correct adapter and return a canonical
	 * IncomingTransaction. This is the ONLY entry point used by the
	 * IngestionPipelineRunner and (later) by IngestionWorker threads.
	 *
	 * @param sourceType which system sent the payload
	 * @param rawPayload raw wire string
	 * @return fully parsed and validated IncomingTransaction
	 * @throws IllegalArgumentException if no adapter is registered for sourceType
	 */
	public IncomingTransaction adapt(SourceType sourceType, String rawPayload) {
		TransactionAdapter adapter = registry.get(sourceType);
		if (adapter == null)
			throw new IllegalArgumentException("[AdapterRegistry] No adapter registered for SourceType: " + sourceType
					+ ". Registered types: " + registry.keySet());

		return adapter.adapt(rawPayload);
	}

	/**
	 * Retrieve the adapter directly (for testing or manual invocation).
	 */
	public TransactionAdapter getAdapter(SourceType sourceType) {
		TransactionAdapter adapter = registry.get(sourceType);
		if (adapter == null)
			throw new IllegalArgumentException("[AdapterRegistry] No adapter for: " + sourceType);
		return adapter;
	}

	/** Returns true if an adapter is registered for this SourceType. */
	public boolean hasAdapter(SourceType sourceType) {
		return registry.containsKey(sourceType);
	}

// Read-only view of the full routing table (for health checks / monitoring).
	public Map<SourceType, TransactionAdapter> getAllAdapters() {
		return registry;
	}

	private void register(Map<SourceType, TransactionAdapter> map, TransactionAdapter adapter) {
		map.put(adapter.getSourceType(), adapter);
	}
}
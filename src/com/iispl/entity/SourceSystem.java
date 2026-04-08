package com.iispl.entity;

import com.iispl.enums.ProtocolType;
import com.iispl.enums.SourceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SourceSystem — Enterprise-grade representation of every external system that
 * sends transactions into the IISPL Bank Settlement pipeline.
 *
 * <p>
 * <b>Relationship:</b> {@code IncomingTransaction} HAS-A {@code SourceSystem}
 *
 * <p>
 * <b>Supported Source Channels:</b>
 * 
 * <pre>
 * ┌──────────┬──────────┬────────────────┬────────────────────────────────────────────┐
 * │ Code     │ Type     │ Protocol       │ Description                                │
 * ├──────────┼──────────┼────────────────┼────────────────────────────────────────────┤
 * │ CBS      │ CBS      │ DIRECT_DB      │ Core Banking System (staging table poll)   │
 * │ RTGS     │ RTGS     │ MESSAGE_QUEUE  │ RBI Real-Time Gross Settlement via MQ      │
 * │ SWIFT    │ SWIFT    │ MESSAGE_QUEUE  │ SWIFT Alliance Gateway (MT103 messages)    │
 * │ NEFT     │ NEFT     │ FLAT_FILE      │ NPCI NEFT batch CSV delivered via SFTP     │
 * │ UPI      │ UPI      │ REST_API       │ NPCI UPI real-time push (24×7)             │
 * │ FINTECH  │ FINTECH  │ REST_API       │ Fintech partner webhook POST               │
 * └──────────┴──────────┴────────────────┴────────────────────────────────────────────┘
 * </pre>
 *
 * <p>
 * <b>Enterprise Fields (beyond the basic design-doc spec):</b>
 * <ul>
 * <li>{@code primaryEndpointUrl} — live connection URL / MQ queue name / DB
 * table</li>
 * <li>{@code failoverEndpointUrl} — backup URL used when primary is DOWN</li>
 * <li>{@code encryptedAuthToken} — API key / AMQP credential (masked in all
 * logs)</li>
 * <li>{@code connectionTimeoutSecs} — connection + read timeout in seconds</li>
 * <li>{@code maxRetryAttempts} — retry attempts on transient failure</li>
 * <li>{@code retryBackoffSecs} — delay between consecutive retries</li>
 * <li>{@code dailyTransactionLimit} — max transactions per day (0 =
 * unlimited)</li>
 * <li>{@code dailyTransactionCount} — running counter, reset at midnight by
 * scheduler</li>
 * <li>{@code maxSingleTransactionAmount} — ceiling for a single transaction
 * (RBI rule)</li>
 * <li>{@code healthStatus} — "UP" / "DEGRADED" / "DOWN"</li>
 * <li>{@code consecutiveFailureCount}— circuit-breaker counter</li>
 * <li>{@code lastHeartbeatAt} — timestamp of last successful ping or poll</li>
 * <li>{@code opsOwnerTeam} — internal ops team owning this integration</li>
 * </ul>
 *
 * @author IISPL Bank Platform Team
 * @since 1.0
 */
public class SourceSystem extends BaseEntity {

	// =========================================================================
	// Core Identity
	// =========================================================================

	/**
	 * Short code uniquely identifying the source channel (e.g., "CBS", "RTGS",
	 * "UPI").
	 */
	private String systemCode;

	/**
	 * Enum classification of the source type (CBS / RTGS / SWIFT / NEFT / UPI /
	 * FINTECH).
	 */
	private SourceType sourceType;

	/** Communication protocol used to ingest transactions from this source. */
	private ProtocolType protocol;

	/** Human-readable name of the source system (e.g., "Core Banking System"). */
	private String systemName;

	/**
	 * Free-text description providing additional context about this integration.
	 */
	private String description;

	// =========================================================================
	// Connection Configuration
	// =========================================================================

	/**
	 * Primary endpoint: REST URL, MQ topic, SFTP path, or DB staging table name.
	 */
	private String primaryEndpointUrl;

	/**
	 * Failover endpoint activated automatically when the primary endpoint is DOWN.
	 */
	private String failoverEndpointUrl;

	/**
	 * Credential used to authenticate with this source system.
	 * <p>
	 * <b>Security:</b> NEVER log this field in raw form. Use
	 * {@link #getMaskedAuthToken()}.
	 */
	private String encryptedAuthToken;

	/**
	 * Additional connection parameters serialised as a JSON string. Holds
	 * transport-specific settings such as HTTP headers, database schema, or MQ
	 * queue configuration.
	 */
	private String connectionConfigJson;

	/** Combined connection and read timeout, in seconds. Default: 30. */
	private int connectionTimeoutSecs;

	/** Maximum number of retry attempts on transient failure. Default: 3. */
	private int maxRetryAttempts;

	/** Backoff delay in seconds between consecutive retry attempts. Default: 5. */
	private int retryBackoffSecs;

	// =========================================================================
	// Transaction Status Mapping
	// =========================================================================

	/**
	 * Indicates whether this source system delivers explicit transaction status
	 * values. When {@code false}, the pipeline infers status from context rather
	 * than a status field.
	 */
	private boolean supportsTxnStatus = true;

	/**
	 * Raw status string from the source that maps to {@code SUCCESS}. Default:
	 * "SUCCESS".
	 */
	private String successStatusValue = "SUCCESS";

	/**
	 * Raw status string from the source that maps to {@code FAILED}. Default:
	 * "FAILED".
	 */
	private String failureStatusValue = "FAILED";

	/**
	 * Raw status string from the source that maps to {@code PENDING}. Default:
	 * "PENDING".
	 */
	private String pendingStatusValue = "PENDING";

	// =========================================================================
	// Operational Limits (RBI / Business Rules)
	// =========================================================================

	/**
	 * Maximum number of transactions allowed per calendar day. A value of {@code 0}
	 * means unlimited.
	 */
	private long dailyTransactionLimit;

	/**
	 * Running count of successfully processed transactions today. Incremented on
	 * each success; reset to zero at midnight by a scheduled job.
	 */
	private long dailyTransactionCount;

	/**
	 * Maximum amount allowed for a single transaction. {@code null} indicates no
	 * upper cap is enforced for this source. Example: ₹1,00,000 for UPI (RBI
	 * mandate).
	 */
	private BigDecimal maxSingleTransactionAmount;

	// =========================================================================
	// Health & Circuit-Breaker Monitoring
	// =========================================================================

	/**
	 * Whether this source system is enabled and accepting transactions. Set to
	 * {@code false} to disable ingestion without deleting configuration.
	 */
	private boolean isActive;

	/**
	 * Current health of this source system, derived from the circuit-breaker
	 * counter.
	 * <ul>
	 * <li>{@code "UP"} — 0–1 consecutive failures; fully operational</li>
	 * <li>{@code "DEGRADED"} — 2–4 consecutive failures; alerts raised, processing
	 * continues</li>
	 * <li>{@code "DOWN"} — 5+ consecutive failures; polling stopped, on-call
	 * paged</li>
	 * </ul>
	 */
	private String healthStatus;

	/**
	 * Timestamp of the last successful contact (ping or successful transaction)
	 * with this system.
	 */
	private LocalDateTime lastHeartbeatAt;

	/**
	 * Circuit-breaker counter incremented on each consecutive failure. Resets to
	 * zero on any success. Thresholds: 0–1 → UP, 2–4 → DEGRADED, 5+ → DOWN.
	 */
	private int consecutiveFailureCount;

	// =========================================================================
	// Contact & Support
	// =========================================================================

	/** Email address of the ops team or vendor contact for this source system. */
	private String opsContactEmail;

	/**
	 * Phone number of the ops helpdesk for this source (RBI/NPCI helpdesk numbers).
	 */
	private String opsContactPhone;

	/**
	 * Internal team responsible for owning and maintaining this integration (e.g.,
	 * "CBS-OPS").
	 */
	private String opsOwnerTeam;

	// =========================================================================
	// Static Factories — Pre-wired Instances for All 6 Channels
	// =========================================================================

	/**
	 * Creates a fully configured {@code SourceSystem} for the Core Banking System.
	 * <p>
	 * Protocol: {@code DIRECT_DB} — polls a staging table in the CBS database.
	 *
	 * @return ready-to-use CBS source system instance
	 */
	public static SourceSystem CBS() {
		SourceSystem cbs = new SourceSystem("CBS", SourceType.CBS, ProtocolType.DIRECT_DB, "Core Banking System");
		cbs.setPrimaryEndpointUrl("Demo Url for importing source transaction and file type should be cbs");
		cbs.setOpsContactEmail("cbs-ops@iisplbank.in");
		cbs.setOpsContactPhone("+91-22-6666-0001");
		cbs.setOpsOwnerTeam("CBS-OPS");
		cbs.setConnectionTimeoutSecs(30);
		cbs.setMaxRetryAttempts(3);
		cbs.setRetryBackoffSecs(5);
		return cbs;
	}

	/**
	 * Creates a fully configured {@code SourceSystem} for the RBI RTGS Gateway.
	 * <p>
	 * Protocol: {@code MESSAGE_QUEUE} — consumes messages from RabbitMQ. Retry is
	 * intentionally minimal (1 attempt) because RTGS is real-time gross settlement
	 * and duplicate retries risk double-settlement.
	 *
	 * @return ready-to-use RTGS source system instance
	 */
	public static SourceSystem RTGS() {
		SourceSystem rtgs = new SourceSystem("RTGS", SourceType.RTGS, ProtocolType.MESSAGE_QUEUE, "RBI RTGS Gateway");
		rtgs.setPrimaryEndpointUrl("Demo Url for importing source transaction and file type should be amqp");
		rtgs.setOpsContactEmail("rtgs-gateway@rbi.org.in");
		rtgs.setOpsContactPhone("1800-111-0001");
		rtgs.setOpsOwnerTeam("RBI-RTGS-OPS");
		rtgs.setConnectionTimeoutSecs(10);
		rtgs.setMaxRetryAttempts(1); // Minimal: real-time gross settlement must not double-retry
		rtgs.setRetryBackoffSecs(2);
		rtgs.setMaxSingleTransactionAmount(null); // No upper cap for RTGS
		return rtgs;
	}

	/**
	 * Creates a fully configured {@code SourceSystem} for the SWIFT Alliance
	 * Gateway.
	 * <p>
	 * Protocol: {@code MESSAGE_QUEUE} — processes MT103 cross-border payment
	 * messages. Higher timeout (60 s) accommodates international network latency.
	 *
	 * @return ready-to-use SWIFT source system instance
	 */
	public static SourceSystem SWIFT() {
		SourceSystem swift = new SourceSystem("SWIFT", SourceType.SWIFT, ProtocolType.MESSAGE_QUEUE,
				"SWIFT Alliance Gateway");
		swift.setPrimaryEndpointUrl("Demo Url for importing source transaction and file type should be amqp");
		swift.setOpsContactEmail("swift-ops@iisplbank.in");
		swift.setOpsContactPhone("+91-22-6666-0003");
		swift.setOpsOwnerTeam("NOSTRO-OPS");
		swift.setConnectionTimeoutSecs(60); // Extended timeout for cross-border network latency
		swift.setMaxRetryAttempts(3);
		swift.setRetryBackoffSecs(10);
		return swift;
	}

	/**
	 * Creates a fully configured {@code SourceSystem} for the NPCI NEFT Batch
	 * Gateway.
	 * <p>
	 * Protocol: {@code FLAT_FILE} — downloads batch CSV files over SFTP. The longer
	 * timeout (120 s) accommodates large batch file transfers. Single transaction
	 * cap of ₹10,00,000 enforced as per typical NEFT limit.
	 *
	 * @return ready-to-use NEFT source system instance
	 */
	public static SourceSystem NEFT() {
		SourceSystem neft = new SourceSystem("NEFT", SourceType.NEFT, ProtocolType.FLAT_FILE,
				"NPCI NEFT Batch Gateway");
		neft.setPrimaryEndpointUrl("Demo Url for importing source transaction and file type should be sftp");
		neft.setOpsContactEmail("neft@npci.org.in");
		neft.setOpsOwnerTeam("NPCI-NEFT-OPS");
		neft.setConnectionTimeoutSecs(120); // Extended for large batch file SFTP transfers
		neft.setMaxRetryAttempts(3);
		neft.setRetryBackoffSecs(30);
		neft.setMaxSingleTransactionAmount(new BigDecimal("1000000.00")); // ₹10 lakh NEFT cap
		return neft;
	}

	/**
	 * Creates a fully configured {@code SourceSystem} for the NPCI UPI Real-Time
	 * Gateway.
	 * <p>
	 * Protocol: {@code REST_API} — receives real-time push notifications 24×7.
	 * Single transaction cap of ₹1,00,000 enforced as per RBI mandate.
	 *
	 * @return ready-to-use UPI source system instance
	 */
	public static SourceSystem UPI() {
		SourceSystem upi = new SourceSystem("UPI", SourceType.UPI, ProtocolType.REST_API, "NPCI UPI Real-Time Gateway");
		upi.setPrimaryEndpointUrl("Demo Url for importing source transaction and file type should be upi");
		upi.setOpsContactEmail("upi@npci.org.in");
		upi.setOpsOwnerTeam("NPCI-UPI-OPS");
		upi.setConnectionTimeoutSecs(15);
		upi.setMaxRetryAttempts(2);
		upi.setRetryBackoffSecs(3);
		upi.setMaxSingleTransactionAmount(new BigDecimal("100000.00")); // RBI ₹1 lakh UPI limit
		return upi;
	}

	/**
	 * Creates a fully configured {@code SourceSystem} for Fintech Partner API
	 * integrations.
	 * <p>
	 * Protocol: {@code REST_API} — accepts inbound webhook POST requests from
	 * partners such as Razorpay, PayU, etc.
	 *
	 * @return ready-to-use FINTECH source system instance
	 */
	public static SourceSystem FINTECH() {
		SourceSystem fintech = new SourceSystem("FINTECH", SourceType.FINTECH, ProtocolType.REST_API,
				"Fintech Partner API Gateway");
		fintech.setPrimaryEndpointUrl("Demo Url for importing source transaction and file type should be fintech");
		fintech.setOpsContactEmail("fintech-ops@iisplbank.in");
		fintech.setOpsContactPhone("+91-22-6666-0005");
		fintech.setOpsOwnerTeam("FINTECH-SETTLEMENTS");
		fintech.setConnectionTimeoutSecs(30);
		fintech.setMaxRetryAttempts(2);
		fintech.setRetryBackoffSecs(5);
		return fintech;
	}

	// =========================================================================
	// Constructors
	// =========================================================================

	/**
	 * Default constructor. Initialises all operational fields to safe defaults:
	 * active, healthy, 3 retries, 5 s backoff, 30 s timeout, zero failure count,
	 * and zero daily counters.
	 */
	public SourceSystem() {
		super();
		this.isActive = true;
		this.healthStatus = "UP";
		this.maxRetryAttempts = 3;
		this.retryBackoffSecs = 5;
		this.connectionTimeoutSecs = 30;
		this.consecutiveFailureCount = 0;
		this.dailyTransactionLimit = 0L;
		this.dailyTransactionCount = 0L;
	}

	/**
	 * Minimal constructor for programmatic or test-driven creation.
	 *
	 * @param systemCode short channel code (e.g., "UPI")
	 * @param protocol   communication protocol for this source
	 */
	public SourceSystem(String systemCode, ProtocolType protocol) {
		this();
		this.systemCode = systemCode;
		this.protocol = protocol;
	}

	/**
	 * Full constructor used by all static factory methods.
	 *
	 * @param systemCode short channel code (e.g., "CBS")
	 * @param sourceType enum classification of the source
	 * @param protocol   communication protocol for this source
	 * @param systemName human-readable display name
	 */
	public SourceSystem(String systemCode, SourceType sourceType, ProtocolType protocol, String systemName) {
		this(systemCode, protocol);
		this.sourceType = sourceType;
		this.systemName = systemName;
	}

	// =========================================================================
	// Business Methods
	// =========================================================================

	/**
	 * Records a successful adapter invocation for this source system.
	 * <p>
	 * Side effects:
	 * <ul>
	 * <li>Resets the circuit-breaker ({@code consecutiveFailureCount} → 0)</li>
	 * <li>Restores health status to "UP"</li>
	 * <li>Updates the last-heartbeat timestamp to now</li>
	 * <li>Increments the daily transaction counter</li>
	 * <li>Marks the entity as modified (via {@code markUpdated()})</li>
	 * </ul>
	 */
	public void recordSuccess() {
		this.consecutiveFailureCount = 0;
		this.healthStatus = "UP";
		this.lastHeartbeatAt = LocalDateTime.now();
		this.dailyTransactionCount++;
		markUpdated();
	}

	/**
	 * Records a failed adapter invocation and advances the circuit-breaker state.
	 * <p>
	 * Circuit-breaker thresholds:
	 * <ul>
	 * <li>0–1 failures → {@code "UP"} (no action)</li>
	 * <li>2–4 failures → {@code "DEGRADED"} (alert ops; processing continues)</li>
	 * <li>5+ failures → {@code "DOWN"} (stop polling; page on-call engineer)</li>
	 * </ul>
	 */
	public void recordFailure() {
		this.consecutiveFailureCount++;
		if (this.consecutiveFailureCount >= 5) {
			this.healthStatus = "DOWN"; // Circuit open — stop ingestion
		} else if (this.consecutiveFailureCount >= 2) {
			this.healthStatus = "DEGRADED"; // Circuit half-open — alert and continue
		}
		markUpdated();
	}

	/**
	 * Returns {@code true} if this source system can currently be used for
	 * transaction ingestion. A source is operational when it is active AND its
	 * health is not "DOWN".
	 *
	 * @return {@code true} if ingestion is permitted; {@code false} otherwise
	 */
	public boolean isOperational() {
		return isActive && !"DOWN".equals(healthStatus);
	}

	/**
	 * Returns {@code true} if today's transaction quota for this source has been
	 * exhausted. Always returns {@code false} when {@code dailyTransactionLimit} is
	 * 0 (unlimited).
	 *
	 * @return {@code true} if the daily limit has been reached
	 */
	public boolean isDailyLimitExceeded() {
		return dailyTransactionLimit > 0 && dailyTransactionCount >= dailyTransactionLimit;
	}

	/**
	 * Resets the daily transaction counter to zero.
	 * <p>
	 * Called by the midnight scheduler to start a fresh day's quota window.
	 */
	public void resetDailyCounter() {
		this.dailyTransactionCount = 0;
		markUpdated();
	}

	/**
	 * Returns a masked representation of the auth token safe for logging.
	 * <p>
	 * Format: first 4 characters + {@code "****"} + last 4 characters. Returns
	 * {@code "****"} if the token is null or shorter than 9 characters.
	 *
	 * @return masked token string (never returns the raw credential)
	 */
	public String getMaskedAuthToken() {
		if (encryptedAuthToken == null || encryptedAuthToken.length() < 9) {
			return "****";
		}
		return encryptedAuthToken.substring(0, 4) + "****"
				+ encryptedAuthToken.substring(encryptedAuthToken.length() - 4);
	}

	// =========================================================================
	// Getters & Setters
	// =========================================================================

	public String getSystemCode() {
		return systemCode;
	}

	public void setSystemCode(String systemCode) {
		this.systemCode = systemCode;
	}

	public SourceType getSourceType() {
		return sourceType;
	}

	public void setSourceType(SourceType sourceType) {
		this.sourceType = sourceType;
	}

	public ProtocolType getProtocol() {
		return protocol;
	}

	public void setProtocol(ProtocolType protocol) {
		this.protocol = protocol;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPrimaryEndpointUrl() {
		return primaryEndpointUrl;
	}

	public void setPrimaryEndpointUrl(String primaryEndpointUrl) {
		this.primaryEndpointUrl = primaryEndpointUrl;
	}

	public String getFailoverEndpointUrl() {
		return failoverEndpointUrl;
	}

	public void setFailoverEndpointUrl(String failoverEndpointUrl) {
		this.failoverEndpointUrl = failoverEndpointUrl;
	}

	public String getEncryptedAuthToken() {
		return encryptedAuthToken;
	}

	public void setEncryptedAuthToken(String encryptedAuthToken) {
		this.encryptedAuthToken = encryptedAuthToken;
	}

	public String getConnectionConfigJson() {
		return connectionConfigJson;
	}

	public void setConnectionConfigJson(String connectionConfigJson) {
		this.connectionConfigJson = connectionConfigJson;
	}

	public int getConnectionTimeoutSecs() {
		return connectionTimeoutSecs;
	}

	public void setConnectionTimeoutSecs(int connectionTimeoutSecs) {
		this.connectionTimeoutSecs = connectionTimeoutSecs;
	}

	public int getMaxRetryAttempts() {
		return maxRetryAttempts;
	}

	public void setMaxRetryAttempts(int maxRetryAttempts) {
		this.maxRetryAttempts = maxRetryAttempts;
	}

	public int getRetryBackoffSecs() {
		return retryBackoffSecs;
	}

	public void setRetryBackoffSecs(int retryBackoffSecs) {
		this.retryBackoffSecs = retryBackoffSecs;
	}

	public boolean isSupportsTxnStatus() {
		return supportsTxnStatus;
	}

	public void setSupportsTxnStatus(boolean supportsTxnStatus) {
		this.supportsTxnStatus = supportsTxnStatus;
	}

	public String getSuccessStatusValue() {
		return successStatusValue;
	}

	public String getFailureStatusValue() {
		return failureStatusValue;
	}

	public String getPendingStatusValue() {
		return pendingStatusValue;
	}

	public long getDailyTransactionLimit() {
		return dailyTransactionLimit;
	}

	public void setDailyTransactionLimit(long dailyTransactionLimit) {
		this.dailyTransactionLimit = dailyTransactionLimit;
	}

	public long getDailyTransactionCount() {
		return dailyTransactionCount;
	}

	public void setDailyTransactionCount(long dailyTransactionCount) {
		this.dailyTransactionCount = dailyTransactionCount;
	}

	public BigDecimal getMaxSingleTransactionAmount() {
		return maxSingleTransactionAmount;
	}

	public void setMaxSingleTransactionAmount(BigDecimal maxSingleTransactionAmount) {
		this.maxSingleTransactionAmount = maxSingleTransactionAmount;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		this.isActive = active;
	}

	public String getHealthStatus() {
		return healthStatus;
	}

	public void setHealthStatus(String healthStatus) {
		this.healthStatus = healthStatus;
	}

	public LocalDateTime getLastHeartbeatAt() {
		return lastHeartbeatAt;
	}

	public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
		this.lastHeartbeatAt = lastHeartbeatAt;
	}

	public int getConsecutiveFailureCount() {
		return consecutiveFailureCount;
	}

	public void setConsecutiveFailureCount(int consecutiveFailureCount) {
		this.consecutiveFailureCount = consecutiveFailureCount;
	}

	public String getOpsContactEmail() {
		return opsContactEmail;
	}

	public void setOpsContactEmail(String opsContactEmail) {
		this.opsContactEmail = opsContactEmail;
	}

	public String getOpsContactPhone() {
		return opsContactPhone;
	}

	public void setOpsContactPhone(String opsContactPhone) {
		this.opsContactPhone = opsContactPhone;
	}

	public String getOpsOwnerTeam() {
		return opsOwnerTeam;
	}

	public void setOpsOwnerTeam(String opsOwnerTeam) {
		this.opsOwnerTeam = opsOwnerTeam;
	}

	// =========================================================================
	// Object Overrides
	// =========================================================================

	/**
	 * Returns a concise, log-safe string representation of this source system. The
	 * auth token is intentionally excluded; use {@link #getMaskedAuthToken()}
	 * separately if needed.
	 */
	@Override
	public String toString() {
		return "SourceSystem{" + "code=" + systemCode + ", type=" + sourceType + ", protocol=" + protocol + ", name='"
				+ systemName + "'" + ", health=" + healthStatus + ", active=" + isActive + ", dailyCount="
				+ dailyTransactionCount + ", endpoint=" + primaryEndpointUrl + "}";
	}
}
package com.iispl.entity;

import com.iispl.enums.ProtocolType;
import com.iispl.enums.SourceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SourceSystem — Enterprise-grade representation of every external system that
 * sends transactions into the IISPL Bank Settlement pipeline.
 *
 * HAS-A relationship: IncomingTransaction HAS-A SourceSystem
 *
 * ─────────────────────────────────────────────────────────────────────────
 * CODE TYPE PROTOCOL DESCRIPTION ───────── ──────── ──────────────
 * ─────────────────────────────────── CBS CBS DIRECT_DB Core Banking System
 * (staging table) RTGS RTGS MESSAGE_QUEUE RBI Real-Time Gross Settlement MQ
 * SWIFT SWIFT MESSAGE_QUEUE SWIFT Alliance Gateway (MT103) NEFT NEFT FLAT_FILE
 * NPCI NEFT batch CSV via SFTP UPI UPI REST_API NPCI UPI real-time push FINTECH
 * FINTECH REST_API Fintech webhook POST (Razorpay etc.)
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Enterprise fields beyond the basic design-doc spec: endpointUrl — live
 * connection URL / MQ queue name / DB table backupEndpointUrl — failover URL
 * authToken — API key / AMQP credential (masked in logs) timeoutSeconds —
 * connection + read timeout maxRetries — retry attempts on transient failure
 * retryDelaySeconds — backoff between retries dailyTxnLimit — max transactions
 * per day (0 = unlimited) dailyTxnCount — running counter (reset midnight by
 * scheduler) maxSingleTxnAmount — ceiling for single transaction (RBI rule)
 * healthStatus — "UP" / "DEGRADED" / "DOWN" consecutiveFailures—
 * circuit-breaker counter lastHeartbeatAt — last successful ping/poll timestamp
 * supportTeam — ops team owning this integration
 */
public class SourceSystem extends BaseEntity {

	// ── Core identity ─────────────────────────────────────────────────────────
	private String systemCode; // "CBS", "RTGS", "SWIFT", "NEFT", "UPI", "FINTECH"
	private SourceType sourceType; // enum — CBS / RTGS / SWIFT / NEFT / UPI / FINTECH
	private ProtocolType protocol; // REST_API / FLAT_FILE / MESSAGE_QUEUE / DIRECT_DB
	private String systemName; // "Core Banking System", "RTGS Gateway", etc.
	private String description; // free-text description of this integration

	// ── Connection configuration ──────────────────────────────────────────────
	private String endpointUrl; // primary URL / MQ topic / DB staging table name
	private String backupEndpointUrl; // failover endpoint (used when primary is DOWN)
	private String authToken; // API key / bearer token (NEVER log raw)
	private String connectionConfig; // extra JSON params (headers, schema, queue config)
	private int timeoutSeconds; // connection + read timeout (default: 30)
	private int maxRetries; // max retry attempts on transient error (default: 3)
	private int retryDelaySeconds; // seconds between retries (default: 5)

	// ── Operational limits (RBI / business rules) ─────────────────────────────
	private long dailyTxnLimit; // 0 = unlimited; >0 = max txns per day
	private long dailyTxnCount; // incremented on each success; reset at midnight
	private BigDecimal maxSingleTxnAmount; // null = no cap; else single-txn ceiling

	// ── Health & monitoring ───────────────────────────────────────────────────
	private boolean isActive; // false = source disabled in config
	private String healthStatus; // "UP" / "DEGRADED" / "DOWN"
	private LocalDateTime lastHeartbeatAt; // last successful contact with this system
	private int consecutiveFailures; // circuit-breaker: 0-1=UP, 2-4=DEGRADED, 5+=DOWN

	// ── Contact & support ─────────────────────────────────────────────────────
	private String contactEmail; // ops contact for this source system
	private String contactPhone; // ops phone (RBI/NPCI helpdesk number)
	private String supportTeam; // internal team owning this link: "CBS-OPS", "NOSTRO-OPS"

	// ── Static factory — pre-wired instances for all 6 channels ──────────────

	/** Core Banking System — direct DB staging poll */
	public static SourceSystem CBS() {
		SourceSystem s = new SourceSystem("CBS", SourceType.CBS, ProtocolType.DIRECT_DB, "Core Banking System");
		s.setEndpointUrl("Demo Url for importing source transaction and file type should be cbs");
		s.setContactEmail("cbs-ops@iisplbank.in");
		s.setContactPhone("+91-22-6666-0001");
		s.setSupportTeam("CBS-OPS");
		s.setTimeoutSeconds(30);
		s.setMaxRetries(3);
		s.setRetryDelaySeconds(5);
		return s;
	}

	/** RTGS Gateway — RabbitMQ message queue (RBI) */
	public static SourceSystem RTGS() {
		SourceSystem s = new SourceSystem("RTGS", SourceType.RTGS, ProtocolType.MESSAGE_QUEUE, "RBI RTGS Gateway");
		s.setEndpointUrl("Demo Url for importing source transaction and file type should be amqp");
		s.setContactEmail("rtgs-gateway@rbi.org.in");
		s.setContactPhone("1800-111-0001");
		s.setSupportTeam("RBI-RTGS-OPS");
		s.setTimeoutSeconds(10);
		s.setMaxRetries(1); // RTGS: minimal retry — it's real-time gross
		s.setRetryDelaySeconds(2);
		s.setMaxSingleTxnAmount(null); // no upper cap for RTGS
		return s;
	}

	/** SWIFT Alliance Gateway — MT103 via message queue */
	public static SourceSystem SWIFT() {
		SourceSystem s = new SourceSystem("SWIFT", SourceType.SWIFT, ProtocolType.MESSAGE_QUEUE,
				"SWIFT Alliance Gateway");
		s.setEndpointUrl("Demo Url for importing source transaction and file type should be amqp");
		s.setContactEmail("swift-ops@iisplbank.in");
		s.setContactPhone("+91-22-6666-0003");
		s.setSupportTeam("NOSTRO-OPS");
		s.setTimeoutSeconds(60);
		s.setMaxRetries(3);
		s.setRetryDelaySeconds(10);
		return s;
	}

	/** NPCI NEFT Gateway — batch flat-file via SFTP */
	public static SourceSystem NEFT() {
		SourceSystem s = new SourceSystem("NEFT", SourceType.NEFT, ProtocolType.FLAT_FILE, "NPCI NEFT Batch Gateway");
		s.setEndpointUrl("Demo Url for importing source transaction and file type should be sftp");
		s.setContactEmail("neft@npci.org.in");
		s.setSupportTeam("NPCI-NEFT-OPS");
		s.setTimeoutSeconds(120);
		s.setMaxRetries(3);
		s.setRetryDelaySeconds(30);
		s.setMaxSingleTxnAmount(new BigDecimal("1000000.00")); // ₹10 lakh typical NEFT cap
		return s;
	}

	/** NPCI UPI Gateway — real-time REST push (24x7) */
	public static SourceSystem UPI() {
		SourceSystem s = new SourceSystem("UPI", SourceType.UPI, ProtocolType.REST_API, "NPCI UPI Real-Time Gateway");
		s.setEndpointUrl("Demo Url for importing source transaction and file type should be upi");
		s.setContactEmail("upi@npci.org.in");
		s.setSupportTeam("NPCI-UPI-OPS");
		s.setTimeoutSeconds(15);
		s.setMaxRetries(2);
		s.setRetryDelaySeconds(3);
		s.setMaxSingleTxnAmount(new BigDecimal("100000.00")); // RBI ₹1 lakh UPI limit
		return s;
	}

	/** Fintech Partner API — webhook REST POST */
	public static SourceSystem FINTECH() {
		SourceSystem s = new SourceSystem("FINTECH", SourceType.FINTECH, ProtocolType.REST_API,
				"Fintech Partner API Gateway");
		s.setEndpointUrl("Demo Url for importing source transaction and file type should be fintech");
		s.setContactEmail("fintech-ops@iisplbank.in");
		s.setContactPhone("+91-22-6666-0005");
		s.setSupportTeam("FINTECH-SETTLEMENTS");
		s.setTimeoutSeconds(30);
		s.setMaxRetries(2);
		s.setRetryDelaySeconds(5);
		return s;
	}

	// ── Constructors ──────────────────────────────────────────────────────────

	public SourceSystem() {
		super();
		this.isActive = true;
		this.healthStatus = "UP";
		this.maxRetries = 3;
		this.retryDelaySeconds = 5;
		this.timeoutSeconds = 30;
		this.consecutiveFailures = 0;
		this.dailyTxnLimit = 0L;
		this.dailyTxnCount = 0L;
	}

	public SourceSystem(String systemCode, ProtocolType protocol) {
		this();
		this.systemCode = systemCode;
		this.protocol = protocol;
	}

	public SourceSystem(String systemCode, SourceType sourceType, ProtocolType protocol, String systemName) {
		this(systemCode, protocol);
		this.sourceType = sourceType;
		this.systemName = systemName;
	}

	// ── Business methods ──────────────────────────────────────────────────────

	/**
	 * Called after a successful adapter invocation. Resets circuit-breaker, updates
	 * heartbeat, increments daily counter.
	 */
	public void recordSuccess() {
		this.consecutiveFailures = 0;
		this.healthStatus = "UP";
		this.lastHeartbeatAt = LocalDateTime.now();
		this.dailyTxnCount++;
		markUpdated();
	}

	/**
	 * Called after a failed adapter invocation. Increments circuit-breaker counter;
	 * degrades or downs health status. 0–1 failures → UP 2–4 failures → DEGRADED
	 * (alert ops, continue processing) 5+ failures → DOWN (stop polling, page
	 * on-call)
	 */
	public void recordFailure() {
		this.consecutiveFailures++;
		if (this.consecutiveFailures >= 5)
			this.healthStatus = "DOWN";
		else if (this.consecutiveFailures >= 2)
			this.healthStatus = "DEGRADED";
		markUpdated();
	}

	/** Returns true if this source is usable for ingestion right now. */
	public boolean isOperational() {
		return isActive && !"DOWN".equals(healthStatus);
	}

	/** Returns true if today's transaction quota has been exhausted. */
	public boolean isDailyLimitExceeded() {
		return dailyTxnLimit > 0 && dailyTxnCount >= dailyTxnLimit;
	}

	/** Scheduler calls this at midnight to reset the daily counter. */
	public void resetDailyCounter() {
		this.dailyTxnCount = 0;
		markUpdated();
	}

	/** Masked token safe for logging (shows first 4 + last 4 chars only). */
	public String getMaskedAuthToken() {
		if (authToken == null || authToken.length() < 9)
			return "****";
		return authToken.substring(0, 4) + "****" + authToken.substring(authToken.length() - 4);
	}

	// ── Getters & Setters ─────────────────────────────────────────────────────

	public String getSystemCode() {
		return systemCode;
	}

	public void setSystemCode(String v) {
		this.systemCode = v;
	}

	public SourceType getSourceType() {
		return sourceType;
	}

	public void setSourceType(SourceType v) {
		this.sourceType = v;
	}

	public ProtocolType getProtocol() {
		return protocol;
	}

	public void setProtocol(ProtocolType v) {
		this.protocol = v;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String v) {
		this.systemName = v;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String v) {
		this.description = v;
	}

	public String getEndpointUrl() {
		return endpointUrl;
	}

	public void setEndpointUrl(String v) {
		this.endpointUrl = v;
	}

	public String getBackupEndpointUrl() {
		return backupEndpointUrl;
	}

	public void setBackupEndpointUrl(String v) {
		this.backupEndpointUrl = v;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String v) {
		this.authToken = v;
	}

	public String getConnectionConfig() {
		return connectionConfig;
	}

	public void setConnectionConfig(String v) {
		this.connectionConfig = v;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int v) {
		this.timeoutSeconds = v;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int v) {
		this.maxRetries = v;
	}

	public int getRetryDelaySeconds() {
		return retryDelaySeconds;
	}

	public void setRetryDelaySeconds(int v) {
		this.retryDelaySeconds = v;
	}

	public long getDailyTxnLimit() {
		return dailyTxnLimit;
	}

	public void setDailyTxnLimit(long v) {
		this.dailyTxnLimit = v;
	}

	public long getDailyTxnCount() {
		return dailyTxnCount;
	}

	public void setDailyTxnCount(long v) {
		this.dailyTxnCount = v;
	}

	public BigDecimal getMaxSingleTxnAmount() {
		return maxSingleTxnAmount;
	}

	public void setMaxSingleTxnAmount(BigDecimal v) {
		this.maxSingleTxnAmount = v;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean v) {
		this.isActive = v;
	}

	public String getHealthStatus() {
		return healthStatus;
	}

	public void setHealthStatus(String v) {
		this.healthStatus = v;
	}

	public LocalDateTime getLastHeartbeatAt() {
		return lastHeartbeatAt;
	}

	public void setLastHeartbeatAt(LocalDateTime v) {
		this.lastHeartbeatAt = v;
	}

	public int getConsecutiveFailures() {
		return consecutiveFailures;
	}

	public void setConsecutiveFailures(int v) {
		this.consecutiveFailures = v;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String v) {
		this.contactEmail = v;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String v) {
		this.contactPhone = v;
	}

	public String getSupportTeam() {
		return supportTeam;
	}

	public void setSupportTeam(String v) {
		this.supportTeam = v;
	}

	// ── toString ──────────────────────────────────────────────────────────────

	@Override
	public String toString() {
		return "SourceSystem{" + "code=" + systemCode + ", type=" + sourceType + ", protocol=" + protocol + ", name='"
				+ systemName + "'" + ", health=" + healthStatus + ", active=" + isActive + ", dailyCnt=" + dailyTxnCount
				+ ", endpoint=" + endpointUrl + "}";
	}
}

//package com.iispl.entity;
//
//import com.iispl.enums.ProtocolType;
//
///**
// * SourceSystem — represents an external system that sends transactions. e.g.
// * CBS, RTGS, SWIFT, NEFT/UPI, Fintech
// */
//public class SourceSystem extends BaseEntity {
//
//	private String systemCode; // CBS / RTGS / SWIFT / NEFT / UPI / FINTECH
//	private ProtocolType protocol; // REST_API / FLAT_FILE / MESSAGE_QUEUE / SFTP
//	private String connectionConfig; // JSON blob: URL, credentials, headers
//	private boolean isActive;
//	private String contactEmail;
//
//	public SourceSystem() {
//		super();
//	}
//
//	public SourceSystem(String systemCode, ProtocolType protocol) {
//		super();
//		this.systemCode = systemCode;
//		this.protocol = protocol;
//		this.isActive = true;
//	}
//
//	public String getSystemCode() {
//		return systemCode;
//	}
//
//	public void setSystemCode(String systemCode) {
//		this.systemCode = systemCode;
//	}
//
//	public ProtocolType getProtocol() {
//		return protocol;
//	}
//
//	public void setProtocol(ProtocolType protocol) {
//		this.protocol = protocol;
//	}
//
//	public String getConnectionConfig() {
//		return connectionConfig;
//	}
//
//	public void setConnectionConfig(String connectionConfig) {
//		this.connectionConfig = connectionConfig;
//	}
//
//	public boolean isActive() {
//		return isActive;
//	}
//
//	public void setActive(boolean active) {
//		isActive = active;
//	}
//
//	public String getContactEmail() {
//		return contactEmail;
//	}
//
//	public void setContactEmail(String contactEmail) {
//		this.contactEmail = contactEmail;
//	}
//
//	@Override
//	public String toString() {
//		return "SourceSystem{code=" + systemCode + ", protocol=" + protocol + ", active=" + isActive + "}";
//	}
//}

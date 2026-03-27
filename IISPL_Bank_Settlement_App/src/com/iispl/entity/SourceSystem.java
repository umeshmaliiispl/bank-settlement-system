package com.iispl.entity;

import com.iispl.enums.ProtocolType;

/**
 * SourceSystem — represents an external system that sends transactions. e.g.
 * CBS, RTGS, SWIFT, NEFT/UPI, Fintech
 */
public class SourceSystem extends BaseEntity {

	private String systemCode; // CBS / RTGS / SWIFT / NEFT / UPI / FINTECH
	private ProtocolType protocol; // REST_API / FLAT_FILE / MESSAGE_QUEUE / SFTP
	private String connectionConfig; // JSON blob: URL, credentials, headers
	private boolean isActive;
	private String contactEmail;

	public SourceSystem() {
		super();
	}

	public SourceSystem(String systemCode, ProtocolType protocol) {
		super();
		this.systemCode = systemCode;
		this.protocol = protocol;
		this.isActive = true;
	}

	public String getSystemCode() {
		return systemCode;
	}

	public void setSystemCode(String systemCode) {
		this.systemCode = systemCode;
	}

	public ProtocolType getProtocol() {
		return protocol;
	}

	public void setProtocol(ProtocolType protocol) {
		this.protocol = protocol;
	}

	public String getConnectionConfig() {
		return connectionConfig;
	}

	public void setConnectionConfig(String connectionConfig) {
		this.connectionConfig = connectionConfig;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		isActive = active;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	@Override
	public String toString() {
		return "SourceSystem{code=" + systemCode + ", protocol=" + protocol + ", active=" + isActive + "}";
	}
}

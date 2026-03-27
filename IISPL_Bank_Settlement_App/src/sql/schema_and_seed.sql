-- QUERY TRUNCATED
-- =============================================================================
-- Bank Settlement Application — PostgreSQL Schema
-- Cloud DB: Neon PostgreSQL (ap-southeast-1 / Singapore)
-- Run this script ONCE on Day 1 morning before anyone starts coding.
-- Team Lead runs this via Neon SQL Editor or psql.
-- =============================================================================

-- Drop tables in reverse dependency order (for clean re-runs)
DROP TABLE IF EXISTS audit_log            CASCADE;
DROP TABLE IF EXISTS reconciliation_entry CASCADE;
DROP TABLE IF EXISTS settlement_instruction CASCADE;
DROP TABLE IF EXISTS netting_position     CASCADE;
DROP TABLE IF EXISTS settlement_record    CASCADE;
DROP TABLE IF EXISTS settlement_batch     CASCADE;
DROP TABLE IF EXISTS incoming_transaction CASCADE;
DROP TABLE IF EXISTS exchange_rate        CASCADE;
DROP TABLE IF EXISTS account              CASCADE;
DROP TABLE IF EXISTS customer             CASCADE;
DROP TABLE IF EXISTS source_system        CASCADE;

-- =============================================================================
-- 1. SOURCE_SYSTEM
-- =============================================================================
CREATE TABLE source_system (
    id               BIGSERIAL       PRIMARY KEY,
    system_code      VARCHAR(20)     NOT NULL UNIQUE,   -- CBS, RTGS, SWIFT, NEFT, UPI, FINTECH
    protocol         VARCHAR(20)     NOT NULL,           -- REST_API, FLAT_FILE, MESSAGE_QUEUE, SFTP, DIRECT_DB
    connection_config TEXT,                              -- JSON: URL, headers, credentials
    is_active        BOOLEAN         NOT NULL DEFAULT TRUE,
    contact_email    VARCHAR(100),
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(50),
    version          INT             NOT NULL DEFAULT 0
);

-- =============================================================================
-- 2. CUSTOMER
-- =============================================================================
CREATE TABLE customer (
    id               BIGSERIAL       PRIMARY KEY,
    first_name       VARCHAR(100)    NOT NULL,
    last_name        VARCHAR(100)    NOT NULL,
    email            VARCHAR(150)    UNIQUE,
    kyc_status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING',  -- PENDING, VERIFIED, REJECTED, EXPIRED, BLOCKED
    customer_tier    VARCHAR(30),
    onboarding_date  DATE            NOT NULL DEFAULT CURRENT_DATE,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(50),
    version          INT             NOT NULL DEFAULT 0
);

-- =============================================================================
-- 3. ACCOUNT
-- =============================================================================
CREATE TABLE account (
    id               BIGSERIAL       PRIMARY KEY,
    account_number   VARCHAR(30)     NOT NULL UNIQUE,
    account_type     VARCHAR(20)     NOT NULL,           -- NOSTRO, VOSTRO, CURRENT, SAVINGS, SUSPENSE, CORRESPONDENT
    balance          NUMERIC(20, 4)  NOT NULL DEFAULT 0,
    currency         CHAR(3)         NOT NULL DEFAULT 'INR',
    customer_id      BIGINT          REFERENCES customer(id),
    bank_id          BIGINT,
    status           VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, FROZEN, CLOSED
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(50),
    version          INT             NOT NULL DEFAULT 0
);

-- =============================================================================
-- 4. INCOMING_TRANSACTION
-- =============================================================================
CREATE TABLE incoming_transaction (
    id                  BIGSERIAL       PRIMARY KEY,
    source_system_id    BIGINT          NOT NULL REFERENCES source_system(id),
    source_ref          VARCHAR(100)    NOT NULL,
    raw_payload         TEXT,
    normalized_payload  TEXT,
    txn_type            VARCHAR(20)     NOT NULL,  -- CREDIT, DEBIT, REVERSAL, SWAP, FEE, INTRABANK
    amount              NUMERIC(20, 4)  NOT NULL,
    currency            CHAR(3)         NOT NULL DEFAULT 'INR',
    value_date          DATE,
    processing_status   VARCHAR(20)     NOT NULL DEFAULT 'RECEIVED',
    ingest_timestamp    TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE (source_system_id, source_ref)  -- prevent duplicate ingestion
);

-- =============================================================================
-- 5. SETTLEMENT_BATCH
-- =============================================================================
CREATE TABLE settlement_batch (
    id                  BIGSERIAL       PRIMARY KEY,
    batch_id            VARCHAR(50)     NOT NULL UNIQUE,
    batch_date          DATE            NOT NULL DEFAULT CURRENT_DATE,
    batch_status        VARCHAR(20)     NOT NULL DEFAULT 'SCHEDULED',
    total_transactions  INT             NOT NULL DEFAULT 0,
    total_amount        NUMERIC(20, 4)  NOT NULL DEFAULT 0,
    run_by              VARCHAR(50),
    run_at              TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    version             INT             NOT NULL DEFAULT 0
);

-- =============================================================================
-- 6. SETTLEMENT_RECORD  (child of SETTLEMENT_BATCH — composition)
-- =============================================================================
CREATE TABLE settlement_record (
    id                  BIGSERIAL       PRIMARY KEY,
    batch_id            VARCHAR(50)     NOT NULL REFERENCES settlement_batch(batch_id),
    incoming_txn_id     BIGINT          NOT NULL REFERENCES incoming_transaction(id),
    settled_amount      NUMERIC(20, 4)  NOT NULL,
    settled_date        TIMESTAMP,
    settled_status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    failure_reason      TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    version             INT             NOT NULL DEFAULT 0
);

-- =============================================================================
-- 7. NETTING_POSITION
-- =============================================================================
CREATE TABLE netting_position (
    id                  BIGSERIAL       PRIMARY KEY,
    counterparty_bank_id BIGINT         NOT NULL,
    currency            CHAR(3)         NOT NULL,
    gross_debit_amount  NUMERIC(20, 4)  NOT NULL DEFAULT 0,
    gross_credit_amount NUMERIC(20, 4)  NOT NULL DEFAULT 0,
    net_amount          NUMERIC(20, 4)  NOT NULL DEFAULT 0,
    direction           VARCHAR(15)     NOT NULL DEFAULT 'FLAT',  -- NET_DEBIT, NET_CREDIT, FLAT
    position_date       DATE            NOT NULL DEFAULT CURRENT_DATE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    version             INT             NOT NULL DEFAULT 0,
    UNIQUE (counterparty_bank_id, currency, position_date)
);

-- =============================================================================
-- 8. SETTLEMENT_INSTRUCTION
-- =============================================================================
CREATE TABLE settlement_instruction (
    id                  BIGSERIAL       PRIMARY KEY,
    instruction_id      VARCHAR(50)     NOT NULL UNIQUE,
    transaction_id      BIGINT          REFERENCES incoming_transaction(id),
    instruction_type    VARCHAR(50),
    channel             VARCHAR(20)     NOT NULL,  -- RTGS, NEFT, UPI, SWIFT, ACH, INTERNAL
    priority            INT             NOT NULL DEFAULT 5,
    value_date          DATE,
    sender_bank_id      BIGINT,
    receiver_bank_id    BIGINT,
    instruction_status  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(50),
    version             INT             NOT NULL DEFAULT 0
);

-- =============================================================================
-- 9. EXCHANGE_RATE
-- =============================================================================
CREATE TABLE exchange_rate (
    id                  BIGSERIAL       PRIMARY KEY,
    base_currency       CHAR(3)         NOT NULL,
    quote_currency      CHAR(3)         NOT NULL,
    rate            
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
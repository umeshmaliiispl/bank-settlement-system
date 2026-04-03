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
CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,

    customer_id VARCHAR(20) UNIQUE NOT NULL,  -- CID1001
    full_name VARCHAR(100),

    kyc_status VARCHAR(20),       -- VERIFIED / PENDING
    customer_status VARCHAR(20),  -- ACTIVE / INACTIVE

    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    version INT
);

-- =============================================================================
-- 3. ACCOUNT
-- =============================================================================
CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,

    account_number VARCHAR(30) UNIQUE NOT NULL,
    ifsc_code VARCHAR(20),
    bank_name VARCHAR(50),

    customer_id VARCHAR(20) NOT NULL,  -- FK

    account_type VARCHAR(20),
    balance NUMERIC(15,2) DEFAULT 0,
    currency VARCHAR(10),

    account_status VARCHAR(20),  -- ACTIVE / BLOCKED

    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    version INT
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






  SELECT * FROM source_system;
SELECT * FROM customer;
SELECT * FROM account;
SELECT * FROM exchange_rate;




ALTER TABLE incoming_transaction
ADD COLUMN channel_code VARCHAR(20),

ADD COLUMN checksum VARCHAR(100),

ADD COLUMN gross_amount NUMERIC(20,4),
ADD COLUMN fee_amount NUMERIC(20,4),

-- 🔥 MOST IMPORTANT (Transaction Status)
ADD COLUMN txn_status VARCHAR(20) DEFAULT 'SUCCESS',

-- BANK DETAILS
ADD COLUMN sender_ifsc VARCHAR(20),
ADD COLUMN receiver_ifsc VARCHAR(20),
ADD COLUMN sender_bank_name VARCHAR(100),
ADD COLUMN receiver_bank_name VARCHAR(100),

-- SWIFT SUPPORT
ADD COLUMN sender_bic VARCHAR(20),
ADD COLUMN receiver_bic VARCHAR(20),

-- FINTECH SUPPORT
ADD COLUM




SELECT * FROM source_system;
SELECT * FROM customer;
SELECT * FROM account;
SELECT * FROM exchange_rate;




ALTER TABLE incoming_transaction
ADD COLUMN channel_code VARCHAR(20),

ADD COLUMN checksum VARCHAR(100),

ADD COLUMN gross_amount NUMERIC(20,4),
ADD COLUMN fee_amount NUMERIC(20,4),

-- 🔥 MOST IMPORTANT (Transaction Status)
ADD COLUMN txn_status VARCHAR(20) DEFAULT 'SUCCESS',

-- BANK DETAILS
ADD COLUMN sender_ifsc VARCHAR(20),
ADD COLUMN receiver_ifsc VARCHAR(20),
ADD COLUMN sender_bank_name VARCHAR(100),
ADD COLUMN receiver_bank_name VARCHAR(100),

-- SWIFT SUPPORT
ADD COLUMN sender_bic VARCHAR(20),
ADD COLUMN receiver_bic VARCHAR(20),

-- FINTECH SUPPORT
ADD COLUMN partner_name VARCHAR(100),
ADD COLUMN merchant_id VARCHAR(50),

-- CONTROL
ADD COLUMN priority INT DEFAULT 5,
ADD COLUMN error_message TEXT;


-- First update existing rows (important)
UPDATE incoming_transaction
SET txn_status = 'SUCCESS'
WHERE txn_status IS NULL;

-- Then enforce NOT NULL
ALTER TABLE incoming_transaction
ALTER COLUMN txn_status SET NOT NULL;



CREATE INDEX idx_txn_status ON incoming_transaction(txn_status);
CREATE INDEX idx_channel_code ON incoming_transaction(channel_code);
CREATE INDEX idx_value_date ON incoming_transaction(value_date);

SELECT * FROM incoming_transaction;


-- TRUNCATE TABLE incoming_transaction RESTART IDENTITY CASCADE;




-- SELECT
--     column_name,
--     data_type,
--     is_nullable,
--     column_default
-- FROM
--     information_schema.columns
-- WHERE
--     table_name = 'incoming_transaction';





-- ALTER TABLE netting_position
-- DROP CONSTRAINT netting_position_counterparty_bank_id_currency_position_date_key;



ALTER TABLE netting_position
DROP COLUMN counterparty_bank_id;



-- -- -- ALTER TABLE netting_position
-- -- -- DROP CONSTRAINT netting_position_counterparty_bank_id_currency_position_date_key;



-- -- -- ALTER TABLE netting_position
-- -- -- DROP COLUMN counterparty_bank_id;




-- -- -- ALTER TABLE netting_position
-- -- -- ADD CONSTRAINT uk_netting_bank_currency_date
-- -- -- UNIQUE (bank_name, currency, position_date);


-- -- ALTER TABLE netting_position
-- -- ALTER COLUMN bank_name SET NOT NULL;



-- ALTER TABLE settlement_instruction
-- DROP CONSTRAINT settlement_instruction_transaction_id_fkey;

-- ALTER TABLE settlement_instruction
-- DROP COLUMN transaction_id;

ALTER TABLE settlement_instruction
DROP COLUMN sender_bank_id;

ALTER TABLE settlement_instruction
DROP COLUMN receiver_bank_id;















-- -- -- ALTER TABLE netting_position
-- -- -- DROP CONSTRAINT netting_position_counterparty_bank_id_currency_position_date_key;



-- -- -- ALTER TABLE netting_position
-- -- -- DROP COLUMN counterparty_bank_id;




-- -- -- ALTER TABLE netting_position
-- -- -- ADD CONSTRAINT uk_netting_bank_currency_date
-- -- -- UNIQUE (bank_name, currency, position_date);


-- -- ALTER TABLE netting_position
-- -- ALTER COLUMN bank_name SET NOT NULL;



-- ALTER TABLE settlement_instruction
-- DROP CONSTRAINT settlement_instruction_transaction_id_fkey;

ALTER TABLE settlement_instruction
DROP COLUMN transaction_id;






-- -- ALTER TABLE netting_position
-- -- DROP CONSTRAINT netting_position_counterparty_bank_id_currency_position_date_key;



-- -- ALTER TABLE netting_position
-- -- DROP COLUMN counterparty_bank_id;




-- -- ALTER TABLE netting_position
-- -- ADD CONSTRAINT uk_netting_bank_currency_date
-- -- UNIQUE (bank_name, currency, position_date);


-- ALTER TABLE netting_position
-- ALTER COLUMN bank_name SET NOT NULL;



ALTER TABLE settlement_instruction
DROP CONSTRAINT settlement_instruction_transaction_id_fkey;



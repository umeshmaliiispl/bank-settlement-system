-- =========================================
-- DATABASE SETUP FOR BANKING SYSTEM
-- Tables: account, customer, transaction
-- =========================================

-- ======================
-- 1. ACCOUNT TABLE
-- ======================
CREATE TABLE account (
    id SERIAL PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    account_name VARCHAR(100) NOT NULL,
    balance NUMERIC(20,2) NOT NULL DEFAULT 0
);

-- Dummy data for account
INSERT INTO account (account_number, account_name, balance) VALUES
('A101', 'Varalakshmi Savings', 5000.00),
('A102', 'Vishnu Current', 10000.00),
('A103', 'Company Payroll', 25000.00);


-- ======================
-- 2. CUSTOMER TABLE
-- ======================
CREATE TABLE customer (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- Dummy data for customer
INSERT INTO customer (name) VALUES
('Varalakshmi'),
('Vishnu'),
('Ram Charan');


-- ======================
-- 3. TRANSACTION TABLE
-- ======================
CREATE TABLE transaction (
    id SERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(20,2) NOT NULL,
    receiver_account VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dummy data for transaction
INSERT INTO transaction (account_id, type, amount, receiver_account) VALUES
(1, 'CREDIT', 2000.00, NULL),
(2, 'DEBIT', 1500.00, NULL),
(3, 'INTERBANK', 5000.00, 'A101');
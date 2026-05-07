-- ============================================================================
-- PAYMENT SYSTEM DATABASE SCHEMA
-- Interview-Ready Design with Proper Normalization & Indexing Strategy
-- ============================================================================

-- ============================================================================
-- TABLE 1: SUBSCRIBERS (Users)
-- ============================================================================
CREATE TABLE subscribers (
    subscriber_id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    date_of_birth DATE,
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(2),
    zip_code VARCHAR(10),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' 
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

-- Indexes on frequently queried columns
CREATE INDEX idx_subscribers_email ON subscribers(email);
CREATE INDEX idx_subscribers_status ON subscribers(status);
CREATE INDEX idx_subscribers_created_at ON subscribers(created_at DESC);

-- ============================================================================
-- TABLE 2: ACCOUNTS (Bank Accounts - one subscriber can have multiple)
-- ============================================================================
CREATE TABLE accounts (
    account_id SERIAL PRIMARY KEY,
    subscriber_id INT NOT NULL,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type VARCHAR(50) NOT NULL 
        CHECK (account_type IN ('CHECKING', 'SAVINGS', 'MONEY_MARKET', 'CD')),
    balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD' 
        CHECK (currency_code = 'USD'),  -- Locked to USD per requirements
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'FROZEN', 'CLOSED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_accounts_subscriber FOREIGN KEY (subscriber_id) 
        REFERENCES subscribers(subscriber_id) ON DELETE CASCADE
);

-- Composite index for subscriber's accounts + status
CREATE INDEX idx_accounts_subscriber_id ON accounts(subscriber_id);
CREATE INDEX idx_accounts_subscriber_status ON accounts(subscriber_id, status);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);

-- ============================================================================
-- TABLE 3: PAYEES (Who you can pay - other subscribers or external entities)
-- ============================================================================
CREATE TABLE payees (
    payee_id SERIAL PRIMARY KEY,
    subscriber_id INT NOT NULL,  -- Owner of this payee (for multi-subscriber systems)
    payee_name VARCHAR(255) NOT NULL,
    payee_type VARCHAR(50) NOT NULL 
        CHECK (payee_type IN ('PERSON', 'BUSINESS', 'UTILITY', 'GOVERNMENT')),
    account_number_or_identifier VARCHAR(50),  -- Could be account #, tax ID, etc.
    routing_number VARCHAR(20),  -- For ACH transfers
    payment_method VARCHAR(50) NOT NULL DEFAULT 'ACH'
        CHECK (payment_method IN ('ACH', 'WIRE', 'CHECK', 'CARD')),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'VERIFIED', 'UNVERIFIED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    
    CONSTRAINT fk_payees_subscriber FOREIGN KEY (subscriber_id) 
        REFERENCES subscribers(subscriber_id) ON DELETE CASCADE
);

-- Composite index for finding payees by subscriber
CREATE INDEX idx_payees_subscriber_id ON payees(subscriber_id);
CREATE INDEX idx_payees_subscriber_status ON payees(subscriber_id, status);
CREATE INDEX idx_payees_payee_type ON payees(payee_type);

-- ============================================================================
-- TABLE 4: PAYMENTS (Transactions - the core of the system)
-- ============================================================================
CREATE TABLE payments (
    payment_id SERIAL PRIMARY KEY,
    subscriber_id INT NOT NULL,
    from_account_id INT NOT NULL,
    to_payee_id INT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'POSTED', 'FAILED', 'REVERSED', 'CANCELLED')),
    payment_date DATE NOT NULL,
    processing_date DATE,  -- When it actually posts (if different from payment_date)
    reference_number VARCHAR(50),  -- Confirmation/trace number
    description VARCHAR(500),  -- What the payment is for
    failure_reason VARCHAR(500),  -- Why it failed (if status = FAILED)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT,  -- User who created it (could be subscriber_id or admin)
    updated_by INT,
    
    CONSTRAINT fk_payments_subscriber FOREIGN KEY (subscriber_id) 
        REFERENCES subscribers(subscriber_id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_from_account FOREIGN KEY (from_account_id) 
        REFERENCES accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_to_payee FOREIGN KEY (to_payee_id) 
        REFERENCES payees(payee_id) ON DELETE RESTRICT
);

-- Critical indexes for payment queries
CREATE INDEX idx_payments_subscriber_id ON payments(subscriber_id);
CREATE INDEX idx_payments_from_account_id ON payments(from_account_id);
CREATE INDEX idx_payments_to_payee_id ON payments(to_payee_id);
CREATE INDEX idx_payments_status ON payments(status);  -- Find pending/failed payments
CREATE INDEX idx_payments_payment_date ON payments(payment_date DESC);  -- For reporting/dashboards
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);
-- Composite index for common dashboard query: "Show me this subscriber's pending payments"
CREATE INDEX idx_payments_subscriber_status ON payments(subscriber_id, status);
CREATE INDEX idx_payments_reference_number ON payments(reference_number) 
    WHERE reference_number IS NOT NULL;  -- Partial index for lookups

-- ============================================================================
-- TABLE 5: AUDIT_LOG (Compliance & troubleshooting - optional but recommended)
-- ============================================================================
CREATE TABLE audit_log (
    log_id SERIAL PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    record_id INT NOT NULL,
    action VARCHAR(20) NOT NULL 
        CHECK (action IN ('INSERT', 'UPDATE', 'DELETE')),
    old_values JSONB,  -- Previous state (useful for debugging)
    new_values JSONB,  -- New state
    changed_by INT,  -- User/system who made change
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45)  -- IPv4 or IPv6
);

-- Index for audit trail queries
CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_changed_at ON audit_log(changed_at DESC);

-- ============================================================================
-- VIEWS (For common dashboard queries - reduces boilerplate in app)
-- ============================================================================

-- View: Subscriber's accounts with balances
CREATE VIEW v_subscriber_accounts AS
SELECT 
    s.subscriber_id,
    s.email,
    a.account_id,
    a.account_number,
    a.account_type,
    a.balance,
    a.status
FROM subscribers s
JOIN accounts a ON s.subscriber_id = a.subscriber_id
WHERE s.deleted_at IS NULL AND a.deleted_at IS NULL;

-- View: Recent payments for dashboard
CREATE VIEW v_recent_payments AS
SELECT 
    p.payment_id,
    p.subscriber_id,
    s.email,
    p.amount,
    p.status,
    p.payment_date,
    py.payee_name,
    a.account_number AS from_account,
    p.description
FROM payments p
JOIN subscribers s ON p.subscriber_id = s.subscriber_id
JOIN accounts a ON p.from_account_id = a.account_id
JOIN payees py ON p.to_payee_id = py.payee_id
WHERE s.deleted_at IS NULL
ORDER BY p.created_at DESC;

-- View: Subscriber dashboard summary
CREATE VIEW v_subscriber_dashboard AS
SELECT 
    s.subscriber_id,
    s.email,
    COUNT(DISTINCT a.account_id) AS total_accounts,
    COALESCE(SUM(a.balance), 0) AS total_balance,
    COUNT(DISTINCT CASE WHEN p.status = 'PENDING' THEN p.payment_id END) AS pending_payments,
    COUNT(DISTINCT CASE WHEN p.status = 'FAILED' THEN p.payment_id END) AS failed_payments
FROM subscribers s
LEFT JOIN accounts a ON s.subscriber_id = a.subscriber_id AND a.deleted_at IS NULL
LEFT JOIN payments p ON s.subscriber_id = p.subscriber_id
WHERE s.deleted_at IS NULL
GROUP BY s.subscriber_id, s.email;

-- ============================================================================
-- NOTES FOR INTERVIEWS & PRODUCTION
-- ============================================================================
/*
DESIGN DECISIONS & Why (Interview talking points):

1. NORMALIZATION:
   - 3NF design: Subscribers → Accounts → Payees → Payments
   - Avoids data duplication (payee info not repeated in payment records)
   - Easier updates (change payee details once, affects all future payments)
   
2. STATUS TRACKING:
   - PENDING (awaiting processing)
   - POSTED (successfully completed)
   - FAILED (rejected, see failure_reason)
   - REVERSED (undone after posting, compliance requirement)
   - CANCELLED (user cancelled before processing)
   
3. SOFT DELETES (deleted_at):
   - Keep historical data for audit/compliance
   - Queries must filter WHERE deleted_at IS NULL
   - Prevents FK constraint issues from deletions
   
4. INDEXING STRATEGY:
   - Single-column indexes on all FKs (join performance)
   - Composite index on (subscriber_id, status) for filtering pending payments
   - Partial index on reference_number (only non-NULL values)
   - Date indexes (DESC order) for dashboard/reporting queries
   - Status index for common WHERE status IN ('PENDING', 'FAILED') queries
   
5. CONSTRAINTS:
   - NOT NULL on required fields (no NULL balances, amounts)
   - UNIQUE on email/account_number (no duplicates)
   - CHECK constraints on enums (status, account_type, payment_method)
   - Foreign keys with CASCADE/RESTRICT (deleting subscriber cascades to accounts)
   - Amount > 0 CHECK (prevent negative/zero payments)
   
6. AUDIT_LOG (optional, but shows enterprise thinking):
   - JSONB columns store old/new values for compliance
   - Tracks WHO changed WHAT and WHEN
   - Interview point: "This is how we traced bugs at Fiserv"
   
7. PERFORMANCE CONSIDERATIONS:
   - Views pre-compute common queries (dashboard, account lists)
   - Reduces N+1 query problems in app code
   - Indexes support high-volume payment processing
   - DECIMAL type (not FLOAT) for money—accuracy matters

8. WHAT'S MISSING (know this for interviews):
   - No encryption/tokenization (real systems would hash account numbers)
   - No rate limiting tables (how many payments/day per user)
   - No transaction_id linking ACH batches (real batch processing has this)
   - No recurring payment scheduling (for bill pay, real systems have this)
   - No notification/alert preferences
   - These can be added later without breaking schema
*/

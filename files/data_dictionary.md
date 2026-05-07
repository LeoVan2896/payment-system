# PAYMENT SYSTEM DATABASE - DATA DICTIONARY

---

## TABLE: `subscribers`
**Purpose:** Core user records. One subscriber = one person using the system.

| Column | Type | Nullable | Unique | Notes | Interview Point |
|--------|------|----------|--------|-------|-----------------|
| `subscriber_id` | SERIAL | ✗ | ✓ PK | Auto-incrementing primary key | N/A |
| `email` | VARCHAR(255) | ✗ | ✓ | Unique per subscriber; used for login | "Why UNIQUE? Prevents duplicate accounts." |
| `first_name` | VARCHAR(100) | ✗ | - | User's first name | N/A |
| `last_name` | VARCHAR(100) | ✗ | - | User's last name | N/A |
| `phone_number` | VARCHAR(20) | ✓ | - | Contact number; optional | "Supports SMS verification or alerts." |
| `date_of_birth` | DATE | ✓ | - | For age verification; optional | "Compliance: age < 18 might be restricted." |
| `address` | VARCHAR(255) | ✓ | - | Mailing/billing address | "KYC (Know Your Customer) requirement." |
| `city` | VARCHAR(100) | ✓ | - | City of residence | N/A |
| `state` | VARCHAR(2) | ✓ | - | State (US); can validate CHECK(state LIKE '[A-Z]{2}') | "Regional compliance varies by state." |
| `zip_code` | VARCHAR(10) | ✓ | - | ZIP code | N/A |
| `status` | VARCHAR(50) | ✗ | - | ACTIVE / INACTIVE / SUSPENDED / CLOSED | "Why SUSPENDED separately from CLOSED? SUSPENDED is temporary (fraud hold), CLOSED is permanent." |
| `created_at` | TIMESTAMP | ✗ | - | Account creation time | "DEFAULT CURRENT_TIMESTAMP auto-populates." |
| `updated_at` | TIMESTAMP | ✗ | - | Last update time; needs trigger to auto-update | "Interview Q: 'How do you keep updated_at fresh?' → Trigger or app-level logic." |
| `deleted_at` | TIMESTAMP | ✓ | - | NULL if active; set to CURRENT_TIMESTAMP when deleted (soft delete) | "Why soft delete? Audit trail + regulatory holds." |

**Key Interview Questions:**
- *"Why is email UNIQUE?"* → Prevents duplicate accounts; is the login identifier.
- *"What's the difference between SUSPENDED and CLOSED?"* → Temporary freeze vs. permanent closure.
- *"How do you query active subscribers only?"* → `WHERE status = 'ACTIVE' AND deleted_at IS NULL`.

---

## TABLE: `accounts`
**Purpose:** Bank/payment accounts. One subscriber can have multiple accounts (checking, savings, etc.).

| Column | Type | Nullable | Unique | Notes | Interview Point |
|--------|------|----------|--------|-------|-----------------|
| `account_id` | SERIAL | ✗ | ✓ PK | Auto-incrementing primary key | N/A |
| `subscriber_id` | INT | ✗ | - | FK to subscribers; defines ownership | "Foreign key ensures referential integrity." |
| `account_number` | VARCHAR(20) | ✗ | ✓ | User-facing account identifier (e.g., *1234); must be UNIQUE | "Why UNIQUE? Can't have two accounts with same number. Masked for security." |
| `account_type` | VARCHAR(50) | ✗ | - | CHECKING / SAVINGS / MONEY_MARKET / CD | "Interview Q: 'Why enum instead of VARCHAR?' → Prevents typos like 'chekcing'." |
| `balance` | DECIMAL(12, 2) | ✗ | - | Current account balance in USD (cents) | "Why DECIMAL not FLOAT? Floating-point rounding errors kill payments. DECIMAL(12,2) = $999,999.99." |
| `currency_code` | VARCHAR(3) | ✗ | - | Always 'USD' per requirements; could extend for multi-currency | "Shows understanding of internationalization (future-proofing)." |
| `status` | VARCHAR(50) | ✗ | - | ACTIVE / INACTIVE / FROZEN / CLOSED | "FROZEN = fraud hold (can't transact). INACTIVE = user closed but keep for history." |
| `created_at` | TIMESTAMP | ✗ | - | Account opening date | N/A |
| `updated_at` | TIMESTAMP | ✗ | - | Last modified | N/A |
| `deleted_at` | TIMESTAMP | ✓ | - | Soft delete marker | "Soft delete allows query: WHERE deleted_at IS NULL." |

**Constraint:** `FOREIGN KEY (subscriber_id) REFERENCES subscribers(subscriber_id) ON DELETE CASCADE`
- Deleting a subscriber cascades and deletes all their accounts.

**Key Interview Questions:**
- *"Why balance as DECIMAL(12, 2)?"* → Financial accuracy; no rounding errors.
- *"What does FROZEN mean?"* → Temporary fraud hold; can't initiate payments.
- *"Why soft delete?"* → Keep audit trail; regulatory holds on closed accounts.

---

## TABLE: `payees`
**Purpose:** Recipients of payments (other users, businesses, utilities, govt). Normalized separately to avoid duplication.

| Column | Type | Nullable | Unique | Notes | Interview Point |
|--------|------|----------|--------|-------|-----------------|
| `payee_id` | SERIAL | ✗ | ✓ PK | Auto-incrementing primary key | N/A |
| `subscriber_id` | INT | ✗ | - | FK to subscribers; defines who owns this payee list | "One subscriber's payee list ≠ another's (privacy)." |
| `payee_name` | VARCHAR(255) | ✗ | - | Friendly name (e.g., "Mom", "Electric Company") | N/A |
| `payee_type` | VARCHAR(50) | ✗ | - | PERSON / BUSINESS / UTILITY / GOVERNMENT | "Determines routing rules: PERSON might use ACH, UTILITY might need account validation." |
| `account_number_or_identifier` | VARCHAR(50) | ✓ | - | Account # / Tax ID / Payee reference; optional if external | "Masked in UI; could be encrypted in real system." |
| `routing_number` | VARCHAR(20) | ✓ | - | ACH routing number (if applicable) | "Required for bank-to-bank transfers; validate format." |
| `payment_method` | VARCHAR(50) | ✗ | - | ACH / WIRE / CHECK / CARD | "Interview: 'Why different methods?' → Costs, speed, recipient support vary." |
| `status` | VARCHAR(50) | ✗ | - | ACTIVE / INACTIVE / VERIFIED / UNVERIFIED | "UNVERIFIED = needs validation (e.g., micro-deposit test). VERIFIED = trusted." |
| `created_at` | TIMESTAMP | ✗ | - | When payee was added | N/A |
| `updated_at` | TIMESTAMP | ✗ | - | Last modified | N/A |
| `deleted_at` | TIMESTAMP | ✓ | - | Soft delete marker | "User can remove payee; we keep record." |

**Constraint:** `FOREIGN KEY (subscriber_id) REFERENCES subscribers(subscriber_id) ON DELETE CASCADE`

**Key Interview Questions:**
- *"Why is payee a separate table?"* → One payee might receive payments from multiple subscribers; data normalized.
- *"What's UNVERIFIED vs VERIFIED?"* → UNVERIFIED = pending micro-deposit test; VERIFIED = confirmed real account.
- *"Which payment_method do you use for ACH?"* → ACH is cheapest, slowest (~2 days); WIRE is instant but expensive.

---

## TABLE: `payments`
**Purpose:** Core transaction records. The main table for the system.

| Column | Type | Nullable | Unique | Notes | Interview Point |
|--------|------|----------|--------|-------|-----------------|
| `payment_id` | SERIAL | ✗ | ✓ PK | Auto-incrementing primary key | N/A |
| `subscriber_id` | INT | ✗ | - | FK; who initiated the payment | "Links payment to the payer (not payee)." |
| `from_account_id` | INT | ✗ | - | FK; which account money comes from | "Verify account belongs to subscriber (FK enforces this)." |
| `to_payee_id` | INT | ✗ | - | FK; who receives the money | "Payee contains account #/routing to reach recipient." |
| `amount` | DECIMAL(12, 2) | ✗ | - | Payment amount; CHECK (amount > 0) | "Must be > 0 (no zero payments). DECIMAL for precision." |
| `currency_code` | VARCHAR(3) | ✗ | - | Always USD; future-proofs for multi-currency | N/A |
| `status` | VARCHAR(50) | ✗ | - | PENDING / POSTED / FAILED / REVERSED / CANCELLED | "Interview: 'What's REVERSED?' → Payment posted, then user asked for refund (refund is new REVERSED payment)." |
| `payment_date` | DATE | ✗ | - | User-requested payment date | "May differ from processing_date (user schedules for future)." |
| `processing_date` | DATE | ✓ | - | Actual date payment processed (NULL until posted) | "Useful for reconciliation: payment_date vs processing_date." |
| `reference_number` | VARCHAR(50) | ✓ | - | Confirmation/trace number (e.g., ACH trace ID) | "Generated by backend after posting; user sees this for support." |
| `description` | VARCHAR(500) | ✓ | - | User notes ("Rent for June", "Dinner refund") | "Helps subscriber remember what payment was for." |
| `failure_reason` | VARCHAR(500) | ✓ | - | Why payment failed (e.g., "Insufficient funds", "Invalid account") | "Shown to user in dashboard; interview: 'How do you notify users of failures?' → Email + dashboard flag." |
| `created_at` | TIMESTAMP | ✗ | - | When payment was created | N/A |
| `updated_at` | TIMESTAMP | ✗ | - | Last modified (e.g., status changed) | N/A |
| `created_by` | INT | ✓ | - | User/system who created (could be subscriber_id or admin_id) | "Audit trail: who initiated." |
| `updated_by` | INT | ✓ | - | User/system who updated | "Audit trail: who changed status." |

**Constraints:**
- `CHECK (amount > 0)` → No zero or negative payments.
- `FK (from_account_id) ... ON DELETE RESTRICT` → Can't delete account with payment history (audit trail protection).
- `FK (to_payee_id) ... ON DELETE RESTRICT` → Can't delete payee with payment history.

**Key Interview Questions:**
- *"Walk me through a payment lifecycle."* → User creates (PENDING) → backend validates (funds check) → posts (POSTED) or fails (FAILED) → optionally reversed (REVERSED).
- *"Why is payment_date separate from processing_date?"* → User schedules for future; actual post date may be next business day.
- *"What if user disputes a payment?"* → Create new payment with REVERSED status (audit trail of both original + reversal).
- *"Why ON DELETE RESTRICT for accounts?"* → Prevents losing payment history.

---

## TABLE: `audit_log` (Optional but Recommended)
**Purpose:** Compliance & debugging. Records every INSERT/UPDATE/DELETE on critical tables.

| Column | Type | Nullable | Unique | Notes | Interview Point |
|--------|------|----------|--------|-------|-----------------|
| `log_id` | SERIAL | ✗ | ✓ PK | Auto-incrementing primary key | N/A |
| `table_name` | VARCHAR(50) | ✗ | - | Which table changed (e.g., 'payments') | "Used for filtering audit trails by table." |
| `record_id` | INT | ✗ | - | Which record changed (e.g., payment_id = 12345) | N/A |
| `action` | VARCHAR(20) | ✗ | - | INSERT / UPDATE / DELETE | N/A |
| `old_values` | JSONB | ✓ | - | Previous state (null if INSERT) | "JSONB allows efficient queries like old_values->>'status'." |
| `new_values` | JSONB | ✓ | - | New state (null if DELETE) | "Compare old vs new to see what changed." |
| `changed_by` | INT | ✓ | - | User/system ID who made change | "Who did this? Trace to subscriber or admin." |
| `changed_at` | TIMESTAMP | ✗ | - | When change occurred | "Immutable record; never update." |
| `ip_address` | VARCHAR(45) | ✓ | - | IP address of requester (IPv4 or IPv6) | "Geolocation fraud detection." |

**Example Query:**
```sql
-- Show all changes to payment 12345 in chronological order
SELECT action, old_values, new_values, changed_by, changed_at
FROM audit_log
WHERE table_name = 'payments' AND record_id = 12345
ORDER BY changed_at ASC;
```

**Interview Point:** *"Why JSONB instead of separate columns?"* → Flexible schema; can store any column changes without modifying audit_log schema.

---

## INDEXES - Performance & Interview Talking Points

| Index Name | Table | Columns | Purpose | Interview Value |
|------------|-------|---------|---------|-----------------|
| `idx_subscribers_email` | subscribers | email | Lookup user by email (login) | "Email is unique; index speeds login queries." |
| `idx_subscribers_status` | subscribers | status | Find active/suspended subscribers | "Common reporting query: 'How many ACTIVE subscribers?'" |
| `idx_accounts_subscriber_id` | accounts | subscriber_id | Find all accounts for a user | "When user logs in: 'SELECT * FROM accounts WHERE subscriber_id = ?' — index this." |
| `idx_accounts_subscriber_status` | accounts | subscriber_id, status | Find active accounts for a user | "Composite index for dashboard: 'Show my active accounts only.'" |
| `idx_payments_subscriber_status` | payments | subscriber_id, status | Find pending/failed payments | "High-cardinality index; critical for dashboard performance." |
| `idx_payments_status` | payments | status | Find all pending/failed payments | "Batch processing: 'Process all PENDING payments.'" |
| `idx_payments_payment_date` | payments | payment_date DESC | Recent transactions | "Dashboard: 'Show last 30 days of payments' — ordered index." |
| `idx_payments_reference_number` | payments | reference_number | Lookup by confirmation # | "Partial index: only 20% of rows have ref #; saves space." |

**Interview Question:** *"Why did you choose these indexes?"* 
→ "Most queries filter by subscriber_id (FK lookup), status (dashboard filtering), or date (reporting). Composite indexes like (subscriber_id, status) avoid full table scans."

---

## VIEWS - Pre-Computed Queries

### `v_subscriber_accounts`
Shows a subscriber's accounts with balances.
```sql
-- Usage: SELECT * FROM v_subscriber_accounts WHERE subscriber_id = 1;
-- Instead of: JOIN subscribers + accounts manually
```
**Interview Value:** "Views hide complexity; the REST API can just query the view instead of writing JOINs in every endpoint."

### `v_recent_payments`
Shows last payments with payee names, account numbers, amounts.
```sql
-- Usage: SELECT * FROM v_recent_payments WHERE subscriber_id = 1 LIMIT 10;
-- For dashboard: latest transaction list
```

### `v_subscriber_dashboard`
Aggregates: total accounts, total balance, pending/failed payment counts.
```sql
-- Used by dashboard endpoint to show summary in one query (no N+1 problem)
```

---

## COMMON QUERIES (Interview Prep)

### Find a subscriber's pending payments:
```sql
SELECT p.payment_id, p.amount, p.payee_id, py.payee_name, p.payment_date
FROM payments p
JOIN payees py ON p.to_payee_id = py.payee_id
WHERE p.subscriber_id = ? AND p.status = 'PENDING'
ORDER BY p.payment_date ASC;
```
**Index Used:** `idx_payments_subscriber_status` ← Composite index on (subscriber_id, status).

### Get total balance for a subscriber (dashboard):
```sql
SELECT SUM(balance) as total_balance
FROM accounts
WHERE subscriber_id = ? AND status = 'ACTIVE' AND deleted_at IS NULL;
```
**Index Used:** `idx_accounts_subscriber_status`.

### Find recently failed payments (for support team):
```sql
SELECT p.payment_id, p.subscriber_id, s.email, p.amount, p.failure_reason, p.created_at
FROM payments p
JOIN subscribers s ON p.subscriber_id = s.subscriber_id
WHERE p.status = 'FAILED' AND p.created_at > NOW() - INTERVAL '24 hours'
ORDER BY p.created_at DESC;
```
**Index Used:** `idx_payments_status` (filter by FAILED) + order by created_at.

### Audit trail: Show all changes to a payment:
```sql
SELECT action, old_values->>'status' as old_status, new_values->>'status' as new_status, changed_by, changed_at
FROM audit_log
WHERE table_name = 'payments' AND record_id = ?
ORDER BY changed_at DESC;
```

---

## INTERVIEW CHECKLIST

Before showing this schema in an interview, be ready to answer:

✓ *"Why did you normalize payees separately?"* 
→ "One payee can receive payments from many users. Normalization avoids duplication."

✓ *"Walk me through a payment failure scenario."*
→ "User submits PENDING → backend validates balance → if fail, update status to FAILED + set failure_reason → frontend alerts user."

✓ *"How do you handle soft deletes?"*
→ "Set deleted_at timestamp; always query WHERE deleted_at IS NULL. Keeps audit trail."

✓ *"What's the difference between processing_date and payment_date?"*
→ "User schedules payment for Friday. Due to ACH T+1 settlement, it processes Monday. payment_date = Friday, processing_date = Monday."

✓ *"Why these specific indexes?"*
→ "Most queries filter by subscriber_id (FK lookup) or status (pending/failed). Composite index (subscriber_id, status) avoids full table scans."

✓ *"What would you add if this went to production?"*
→ "Encryption for account numbers, rate limiting table, recurring payment scheduler, notification preferences, transaction reconciliation table, database replication for HA."

---

Generated for Huy Van's Full-Stack Payment System Project | Interview-ready schema design

-- Data-quality rules from the 2026 audit. NOT VALID: legacy rows are cleaned up separately,
-- but every new or updated row must comply.

-- KYC results only from contracted bureaus
ALTER TABLE kyc_check ADD CONSTRAINT kyc_check_provider_chk
    CHECK (provider IN ('EXPERIAN', 'EQUIFAX', 'TRANSUNION', 'SOCURE')) NOT VALID;

-- payment method fingerprints are SHA-256 hex digests from the vault
ALTER TABLE payment_method ADD CONSTRAINT payment_method_fingerprint_chk
    CHECK (fingerprint ~ '^[0-9a-f]{64}$') NOT VALID;

-- credit models are versioned with full semantic versions (v1.4.2)
ALTER TABLE credit_decision DROP CONSTRAINT credit_decision_model_version_check;
ALTER TABLE credit_decision ADD CONSTRAINT credit_decision_model_version_chk
    CHECK (model_version ~ '^v[0-9]+\.[0-9]+\.[0-9]+$') NOT VALID;

-- collection agents use corporate accounts
ALTER TABLE collection_agent ADD CONSTRAINT collection_agent_corporate_email_chk
    CHECK (email ~ '^[a-z0-9._-]+@collections\.lender\.example$') NOT VALID;

-- a customer has at most one active loan per store
CREATE UNIQUE INDEX loan_one_active_per_customer_store ON loan (customer_id, store_id) WHERE status = 'ACTIVE';

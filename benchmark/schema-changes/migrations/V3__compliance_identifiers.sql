-- Compliance onboarding data that every new record must carry:
--  * merchants: federal tax id (EIN),
--  * stores: IANA time zone (cut-off times for same-day funding),
--  * customers: last four digits of the SSN (identity matching),
--  * payment methods: account holder name (NACHA / card-network rules).
-- Legacy rows are backfilled with placeholders; new rows must provide real values (no defaults).

ALTER TABLE merchant ADD COLUMN tax_id varchar(10) CHECK (tax_id ~ '^[0-9]{2}-[0-9]{7}$');
UPDATE merchant SET tax_id = '00-' || lpad(id::text, 7, '0');
ALTER TABLE merchant ALTER COLUMN tax_id SET NOT NULL;
ALTER TABLE merchant ADD CONSTRAINT merchant_tax_id_key UNIQUE (tax_id);

ALTER TABLE store ADD COLUMN timezone text
    CHECK (timezone IN ('America/New_York', 'America/Chicago', 'America/Denver', 'America/Phoenix', 'America/Los_Angeles'));
UPDATE store SET timezone = 'America/Chicago';
ALTER TABLE store ALTER COLUMN timezone SET NOT NULL;

ALTER TABLE customer ADD COLUMN ssn_last4 char(4) CHECK (ssn_last4 ~ '^[0-9]{4}$');
UPDATE customer SET ssn_last4 = '0000';
ALTER TABLE customer ALTER COLUMN ssn_last4 SET NOT NULL;

ALTER TABLE payment_method ADD COLUMN holder_name text CHECK (length(btrim(holder_name)) > 1);
UPDATE payment_method pm SET holder_name = (SELECT c.first_name || ' ' || c.last_name FROM customer c WHERE c.id = pm.customer_id);
ALTER TABLE payment_method ALTER COLUMN holder_name SET NOT NULL;

-- A compact lending schema used by core tests.
CREATE TYPE loan_status AS ENUM ('PENDING', 'ACTIVE', 'PAID_OFF', 'CHARGED_OFF');
CREATE DOMAIN money_amount AS numeric(12,2) CHECK (VALUE >= 0);

CREATE TABLE merchant (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name text NOT NULL,
  country char(2) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE store (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  merchant_id bigint NOT NULL REFERENCES merchant(id),
  name text NOT NULL,
  city text NOT NULL
);
CREATE TABLE customer (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  first_name text NOT NULL,
  last_name text NOT NULL,
  email varchar(120) NOT NULL UNIQUE,
  phone text,
  date_of_birth date NOT NULL CHECK (date_of_birth < DATE '2010-01-01'),
  segment text
);
CREATE TABLE loan (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  customer_id bigint NOT NULL REFERENCES customer(id),
  store_id bigint NOT NULL REFERENCES store(id),
  principal money_amount NOT NULL CHECK (principal > 0),
  term_months int NOT NULL CHECK (term_months BETWEEN 1 AND 60),
  status loan_status NOT NULL,
  originated_on date NOT NULL,
  closed_at timestamptz,
  metadata jsonb NOT NULL DEFAULT '{}'
);
CREATE UNIQUE INDEX one_pending_loan_per_customer ON loan(customer_id) WHERE status = 'PENDING';
CREATE TABLE installment (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  loan_id bigint NOT NULL REFERENCES loan(id),
  seq int NOT NULL,
  due_date date NOT NULL,
  amount numeric(12,2) NOT NULL CHECK (amount > 0),
  paid_amount numeric(12,2) NOT NULL DEFAULT 0,
  status text NOT NULL CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID')),
  paid_at timestamptz,
  outstanding numeric(12,2) GENERATED ALWAYS AS (amount - paid_amount) STORED,
  UNIQUE (loan_id, seq),
  CHECK (paid_amount <= amount)
);
CREATE TABLE dispute (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  loan_id bigint NOT NULL REFERENCES loan(id),
  status text NOT NULL,
  reason text NOT NULL,
  CONSTRAINT dispute_status_chk CHECK (status IN ('OPEN', 'CLOSED'))
);
CREATE TABLE customer_kyc (
  customer_id bigint PRIMARY KEY REFERENCES customer(id),
  level text NOT NULL CHECK (level IN ('BASIC', 'FULL'))
);

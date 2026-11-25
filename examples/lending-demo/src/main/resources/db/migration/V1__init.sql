-- Lending domain: merchants sell through stores; customers take installment loans.
CREATE TYPE loan_status AS ENUM ('PENDING', 'ACTIVE', 'PAID_OFF', 'CHARGED_OFF', 'CANCELLED');

CREATE TABLE merchant (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text        NOT NULL,
    legal_name  text        NOT NULL,
    country     char(2)     NOT NULL,
    status      text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE store (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id bigint  NOT NULL REFERENCES merchant (id),
    name        text    NOT NULL,
    city        text    NOT NULL,
    state       char(2) NOT NULL
);

CREATE TABLE customer (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name    text         NOT NULL,
    last_name     text         NOT NULL,
    email         varchar(254) NOT NULL UNIQUE,
    phone         varchar(32),
    date_of_birth date         NOT NULL CHECK (date_of_birth > DATE '1900-01-01'),
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE address (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id bigint      NOT NULL REFERENCES customer (id),
    line1       text        NOT NULL,
    city        text        NOT NULL,
    state       char(2)     NOT NULL,
    postal_code varchar(10) NOT NULL,
    is_primary  boolean     NOT NULL DEFAULT false
);
-- at most one primary address per customer
CREATE UNIQUE INDEX address_one_primary_per_customer ON address (customer_id) WHERE is_primary;

CREATE TABLE kyc_check (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id bigint      NOT NULL REFERENCES customer (id),
    provider    text        NOT NULL,
    status      text        NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    checked_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE loan (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id   bigint        NOT NULL REFERENCES customer (id),
    store_id      bigint        NOT NULL REFERENCES store (id),
    principal     numeric(12,2) NOT NULL CHECK (principal > 0),
    apr           numeric(5,2)  NOT NULL CHECK (apr >= 0 AND apr <= 36),
    term_months   int           NOT NULL CHECK (term_months BETWEEN 1 AND 60),
    status        loan_status   NOT NULL DEFAULT 'PENDING',
    originated_on date          NOT NULL,
    closed_on     date,
    metadata      jsonb         NOT NULL DEFAULT '{}',
    CHECK (closed_on IS NULL OR closed_on >= originated_on)
);

CREATE TABLE installment (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loan_id     bigint        NOT NULL REFERENCES loan (id),
    seq         int           NOT NULL CHECK (seq > 0),
    due_date    date          NOT NULL,
    amount      numeric(12,2) NOT NULL CHECK (amount > 0),
    paid_amount numeric(12,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    status      text          NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'WAIVED')),
    paid_at     timestamptz,
    outstanding numeric(12,2) GENERATED ALWAYS AS (amount - paid_amount) STORED,
    UNIQUE (loan_id, seq),
    CHECK (paid_amount <= amount),
    CHECK (status <> 'PAID' OR paid_amount = amount)
);

CREATE TABLE payment (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loan_id        bigint        NOT NULL REFERENCES loan (id),
    installment_id bigint REFERENCES installment (id),
    amount         numeric(12,2) NOT NULL CHECK (amount > 0),
    method         text          NOT NULL CHECK (method IN ('CARD', 'ACH')),
    status         text          NOT NULL DEFAULT 'SETTLED'
                   CHECK (status IN ('PENDING', 'SETTLED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    external_ref   varchar(40)   NOT NULL UNIQUE,
    paid_at        timestamptz   NOT NULL DEFAULT now()
);

CREATE TABLE refund (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id bigint        NOT NULL REFERENCES payment (id),
    amount     numeric(12,2) NOT NULL CHECK (amount > 0),
    reason     text          NOT NULL,
    created_at timestamptz   NOT NULL DEFAULT now()
);

CREATE TABLE late_fee (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    installment_id bigint        NOT NULL REFERENCES installment (id),
    amount         numeric(12,2) NOT NULL CHECK (amount > 0),
    assessed_on    date          NOT NULL,
    waived         boolean       NOT NULL DEFAULT false
);

CREATE TABLE dispute (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loan_id     bigint      NOT NULL REFERENCES loan (id),
    reason      text        NOT NULL,
    status      text        NOT NULL CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED')),
    opened_at   timestamptz NOT NULL DEFAULT now(),
    resolved_at timestamptz,
    CHECK ((status IN ('RESOLVED', 'REJECTED')) = (resolved_at IS NOT NULL))
);
-- at most one active dispute per loan
CREATE UNIQUE INDEX dispute_one_active_per_loan ON dispute (loan_id) WHERE status IN ('OPEN', 'UNDER_REVIEW');

CREATE TABLE merchant_payout (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id  bigint        NOT NULL REFERENCES merchant (id),
    period_start date          NOT NULL,
    period_end   date          NOT NULL,
    amount       numeric(14,2) NOT NULL CHECK (amount >= 0),
    status       text          NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID')),
    created_at   timestamptz   NOT NULL DEFAULT now(),
    CHECK (period_end >= period_start),
    UNIQUE (merchant_id, period_start)
);

CREATE TABLE payout_item (
    payout_id bigint        NOT NULL REFERENCES merchant_payout (id),
    loan_id   bigint        NOT NULL REFERENCES loan (id),
    amount    numeric(12,2) NOT NULL CHECK (amount >= 0),
    PRIMARY KEY (payout_id, loan_id)
);

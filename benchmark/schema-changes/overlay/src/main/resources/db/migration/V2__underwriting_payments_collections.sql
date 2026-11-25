-- Underwriting (application -> decision -> offers -> loan), stored payment methods with
-- automatic payment attempts, and collections (agents, cases, promises to pay).

CREATE TABLE credit_application (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id      bigint        NOT NULL REFERENCES customer (id),
    store_id         bigint        NOT NULL REFERENCES store (id),
    requested_amount numeric(12,2) NOT NULL CHECK (requested_amount BETWEEN 50 AND 25000),
    channel          text          NOT NULL CHECK (channel IN ('ONLINE', 'IN_STORE')),
    status           text          NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'DECIDED', 'WITHDRAWN')),
    submitted_at     timestamptz   NOT NULL DEFAULT now()
);
-- at most one undecided application per customer and store
CREATE UNIQUE INDEX credit_application_one_open ON credit_application (customer_id, store_id) WHERE status = 'SUBMITTED';

CREATE TABLE credit_decision (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id bigint        NOT NULL UNIQUE REFERENCES credit_application (id),
    outcome        text          NOT NULL CHECK (outcome IN ('APPROVED', 'DECLINED', 'REFERRED')),
    score          int           NOT NULL CHECK (score BETWEEN 300 AND 850),
    approved_limit numeric(12,2) CHECK (approved_limit > 0),
    decline_reason text,
    model_version  varchar(16)   NOT NULL CHECK (model_version ~ '^v[0-9]+\.[0-9]+$'),
    decided_at     timestamptz   NOT NULL DEFAULT now(),
    CHECK ((outcome = 'APPROVED') = (approved_limit IS NOT NULL)),
    CHECK ((outcome = 'DECLINED') = (decline_reason IS NOT NULL))
);

CREATE TABLE offer (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    decision_id bigint        NOT NULL REFERENCES credit_decision (id),
    amount      numeric(12,2) NOT NULL CHECK (amount > 0),
    term_months int           NOT NULL CHECK (term_months IN (3, 6, 12, 24)),
    apr         numeric(5,2)  NOT NULL CHECK (apr >= 0 AND apr <= 36),
    status      text          NOT NULL DEFAULT 'OFFERED' CHECK (status IN ('OFFERED', 'ACCEPTED', 'WITHDRAWN', 'EXPIRED')),
    expires_at  timestamptz   NOT NULL,
    accepted_at timestamptz,
    UNIQUE (decision_id, term_months),
    CHECK ((status = 'ACCEPTED') = (accepted_at IS NOT NULL))
);
-- a decision is accepted at most once
CREATE UNIQUE INDEX offer_one_accepted_per_decision ON offer (decision_id) WHERE status = 'ACCEPTED';

-- loans originated through underwriting point at their offer (legacy loans have none yet)
ALTER TABLE loan ADD COLUMN offer_id bigint UNIQUE REFERENCES offer (id);

CREATE TABLE payment_method (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id    bigint      NOT NULL REFERENCES customer (id),
    type           text        NOT NULL CHECK (type IN ('CARD', 'ACH')),
    fingerprint    varchar(64) NOT NULL,
    last4          char(4)     NOT NULL CHECK (last4 ~ '^[0-9]{4}$'),
    card_brand     text        CHECK (card_brand IN ('VISA', 'MASTERCARD', 'AMEX', 'DISCOVER')),
    exp_month      int         CHECK (exp_month BETWEEN 1 AND 12),
    exp_year       int         CHECK (exp_year BETWEEN 2000 AND 2100),
    routing_number char(9)     CHECK (routing_number ~ '^[0-9]{9}$'),
    is_default     boolean     NOT NULL DEFAULT false,
    status         text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'REMOVED')),
    created_at     timestamptz NOT NULL DEFAULT now(),
    UNIQUE (customer_id, fingerprint),
    CHECK (type <> 'CARD' OR (card_brand IS NOT NULL AND exp_month IS NOT NULL AND exp_year IS NOT NULL AND routing_number IS NULL)),
    CHECK (type <> 'ACH' OR (routing_number IS NOT NULL AND card_brand IS NULL AND exp_month IS NULL AND exp_year IS NULL)),
    CHECK (NOT is_default OR status = 'ACTIVE')
);
-- one default payment method per customer
CREATE UNIQUE INDEX payment_method_one_default ON payment_method (customer_id) WHERE is_default;

CREATE TABLE payment_attempt (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    installment_id    bigint        NOT NULL REFERENCES installment (id),
    payment_method_id bigint        NOT NULL REFERENCES payment_method (id),
    attempt_no        int           NOT NULL CHECK (attempt_no BETWEEN 1 AND 5),
    amount            numeric(12,2) NOT NULL CHECK (amount > 0),
    status            text          NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    scheduled_for     date          NOT NULL,
    attempted_at      timestamptz,
    failure_code      text          CHECK (failure_code IN ('INSUFFICIENT_FUNDS', 'DO_NOT_HONOR', 'EXPIRED_CARD', 'ACCOUNT_CLOSED', 'STOLEN_CARD')),
    payment_id        bigint        UNIQUE REFERENCES payment (id),
    UNIQUE (installment_id, attempt_no),
    CHECK ((status IN ('SUCCEEDED', 'FAILED')) = (attempted_at IS NOT NULL)),
    CHECK ((status = 'FAILED') = (failure_code IS NOT NULL)),
    CHECK ((status = 'SUCCEEDED') = (payment_id IS NOT NULL))
);
-- at most one pending attempt per installment
CREATE UNIQUE INDEX payment_attempt_one_scheduled ON payment_attempt (installment_id) WHERE status = 'SCHEDULED';

CREATE TABLE collection_agent (
    id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email     varchar(254) NOT NULL UNIQUE,
    full_name text         NOT NULL,
    team      text         NOT NULL CHECK (team IN ('EARLY', 'LATE', 'RECOVERY')),
    active    boolean      NOT NULL DEFAULT true
);

CREATE TABLE collection_case (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loan_id      bigint NOT NULL REFERENCES loan (id),
    agent_id     bigint NOT NULL REFERENCES collection_agent (id),
    stage        text   NOT NULL CHECK (stage IN ('EARLY', 'LATE', 'RECOVERY')),
    status       text   NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'PROMISE_TO_PAY', 'CLOSED')),
    opened_on    date   NOT NULL,
    closed_on    date,
    close_reason text   CHECK (close_reason IN ('PAID', 'SETTLED', 'CHARGED_OFF', 'ERROR')),
    CHECK ((status = 'CLOSED') = (closed_on IS NOT NULL)),
    CHECK ((status = 'CLOSED') = (close_reason IS NOT NULL)),
    CHECK (closed_on IS NULL OR closed_on >= opened_on)
);
-- at most one case that is not closed per loan
CREATE UNIQUE INDEX collection_case_one_open_per_loan ON collection_case (loan_id) WHERE status <> 'CLOSED';

CREATE TABLE promise_to_pay (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    case_id        bigint        NOT NULL REFERENCES collection_case (id),
    installment_id bigint        NOT NULL REFERENCES installment (id),
    amount         numeric(12,2) NOT NULL CHECK (amount > 0),
    promised_on    date          NOT NULL,
    status         text          NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'KEPT', 'BROKEN')),
    created_at     timestamptz   NOT NULL DEFAULT now()
);
-- at most one pending promise per installment
CREATE UNIQUE INDEX promise_one_pending_per_installment ON promise_to_pay (installment_id) WHERE status = 'PENDING';

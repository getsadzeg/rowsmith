-- Stores now operate under a merchant contract (pricing and terms), which sits between store and merchant:
-- store -> merchant_contract -> merchant. A store's contract must belong to the store's own merchant.

CREATE TABLE merchant_contract (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id   bigint       NOT NULL REFERENCES merchant (id),
    contract_no   varchar(16)  NOT NULL UNIQUE CHECK (contract_no ~ '^MC-[0-9]{6}$'),
    discount_rate numeric(5,4) NOT NULL CHECK (discount_rate > 0 AND discount_rate < 0.15),
    status        text         NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'TERMINATED')),
    signed_on     date,
    terminated_on date,
    UNIQUE (id, merchant_id),
    CHECK ((status = 'DRAFT') = (signed_on IS NULL)),
    CHECK ((status = 'TERMINATED') = (terminated_on IS NOT NULL)),
    CHECK (terminated_on IS NULL OR terminated_on >= signed_on)
);
-- at most one active contract per merchant
CREATE UNIQUE INDEX merchant_contract_one_active ON merchant_contract (merchant_id) WHERE status = 'ACTIVE';

INSERT INTO merchant_contract (merchant_id, contract_no, discount_rate, status, signed_on)
SELECT id, 'MC-' || lpad(id::text, 6, '0'), 0.0300, 'ACTIVE', created_at::date FROM merchant;

ALTER TABLE store ADD COLUMN contract_id bigint;
UPDATE store s SET contract_id = (SELECT c.id FROM merchant_contract c WHERE c.merchant_id = s.merchant_id AND c.status = 'ACTIVE');
ALTER TABLE store ALTER COLUMN contract_id SET NOT NULL;
ALTER TABLE store ADD CONSTRAINT store_contract_fk FOREIGN KEY (contract_id, merchant_id) REFERENCES merchant_contract (id, merchant_id);

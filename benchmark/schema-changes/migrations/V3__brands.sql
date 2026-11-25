-- Multi-brand lending: every customer and every merchant belongs to exactly one brand.

CREATE TABLE brand (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code          varchar(8)   NOT NULL UNIQUE CHECK (code ~ '^[A-Z]{2,8}$'),
    name          text         NOT NULL,
    support_email varchar(254) NOT NULL,
    launched_on   date         NOT NULL,
    active        boolean      NOT NULL DEFAULT true
);
INSERT INTO brand (code, name, support_email, launched_on) VALUES ('CORE', 'Core Lending', 'support@core.example', DATE '2020-01-01');

ALTER TABLE customer ADD COLUMN brand_id bigint REFERENCES brand (id);
UPDATE customer SET brand_id = (SELECT id FROM brand WHERE code = 'CORE');
ALTER TABLE customer ALTER COLUMN brand_id SET NOT NULL;

ALTER TABLE merchant ADD COLUMN brand_id bigint REFERENCES brand (id);
UPDATE merchant SET brand_id = (SELECT id FROM brand WHERE code = 'CORE');
ALTER TABLE merchant ALTER COLUMN brand_id SET NOT NULL;

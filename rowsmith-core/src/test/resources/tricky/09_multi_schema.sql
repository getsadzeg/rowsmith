-- Several schemas, cross-schema FKs and the same table name in two schemas.
CREATE SCHEMA billing;
CREATE SCHEMA audit;
CREATE TABLE customer (id serial PRIMARY KEY, name text NOT NULL);
CREATE TABLE billing.invoice (
  id serial PRIMARY KEY,
  customer_id int NOT NULL REFERENCES public.customer(id),
  total numeric(10,2) NOT NULL CHECK (total >= 0)
);
CREATE TABLE event (id serial PRIMARY KEY, kind text NOT NULL);
CREATE TABLE audit.event (
  id serial PRIMARY KEY,
  invoice_id int NOT NULL REFERENCES billing.invoice(id),
  happened_at timestamptz NOT NULL
);

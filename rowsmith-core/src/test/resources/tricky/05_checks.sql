-- CHECK constraints of many shapes.
-- Cross-column CHECKs that generated values miss fail clearly: the test sets those columns.
-- expect payment: CANNOT_GENERATE
CREATE TABLE product (
  id serial PRIMARY KEY,
  code varchar(12) NOT NULL CHECK (code = upper(code)),
  name text NOT NULL CHECK (length(trim(name)) > 0),
  price numeric(8,2) NOT NULL CHECK (price > 0 AND price < 1000),
  discount_pct int NOT NULL CHECK (discount_pct BETWEEN 0 AND 50),
  size text NOT NULL CHECK (size IN ('S', 'M', 'L')),
  color text CHECK (color NOT IN ('', 'none')),
  sku text NOT NULL CHECK (sku LIKE 'SKU-%'),
  barcode text NOT NULL CHECK (barcode ~ '^[0-9]{8}$'),
  weight_kg real NOT NULL CHECK (weight_kg >= 0.1),
  launched_on date NOT NULL CHECK (launched_on >= DATE '2020-01-01'),
  retired_on date,
  CHECK (retired_on IS NULL OR retired_on > launched_on)
);
CREATE TABLE payment (
  id serial PRIMARY KEY,
  amount numeric(10,2) NOT NULL CHECK (amount > 0),
  refunded numeric(10,2) NOT NULL CHECK (refunded >= 0),
  card_token text,
  iban text,
  status text NOT NULL CHECK (status IN ('PENDING', 'SETTLED', 'REFUNDED')),
  settled_at timestamptz,
  CHECK (refunded <= amount),
  CHECK (num_nonnulls(card_token, iban) = 1),
  CHECK (status <> 'SETTLED' OR settled_at IS NOT NULL)
);
CREATE TABLE reservation (
  id serial PRIMARY KEY,
  starts_at timestamp NOT NULL,
  ends_at timestamp NOT NULL,
  guests smallint NOT NULL CHECK (guests >= 1 AND guests <= 8),
  CHECK (ends_at > starts_at)
);

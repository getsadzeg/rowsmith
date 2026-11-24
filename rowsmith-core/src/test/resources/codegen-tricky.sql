-- Names and types that need care in generated Java code.
CREATE EXTENSION IF NOT EXISTS citext;
CREATE SCHEMA audit;
CREATE TYPE mood AS ENUM ('sad', 'ok', 'very happy', '1st', 'OK');
CREATE TYPE audit.mood AS ENUM ('calm', 'angry');
CREATE DOMAIN shirt_size AS text CHECK (VALUE IN ('S', 'M', 'L', 'XL'));
CREATE DOMAIN positive_int AS integer CHECK (VALUE > 0);

-- a table named like java.lang.String and another like the generated class
CREATE TABLE string (id serial PRIMARY KEY, value text NOT NULL);
CREATE TABLE db (id smallint PRIMARY KEY);

CREATE TABLE "Order" (
  "Id" serial PRIMARY KEY,
  "user" text NOT NULL,
  "Created At" timestamp NOT NULL,
  "createdAt" timestamp,
  "orderStatus" varchar(20) NOT NULL CHECK ("orderStatus" IN ('NEW', 'PAID', 'SHIPPED')),
  "1st_try" boolean NOT NULL
);

CREATE TABLE event (id bigserial PRIMARY KEY, kind text NOT NULL CHECK (kind = 'a' OR kind = 'b'));
CREATE TABLE audit.event (
  id bigserial PRIMARY KEY,
  order_id int REFERENCES "Order"("Id"),
  mood audit.mood NOT NULL,
  at timestamptz NOT NULL
);

CREATE TABLE profile (
  id uuid PRIMARY KEY,
  handle citext NOT NULL,
  current_mood mood NOT NULL,
  past_moods mood[] NOT NULL,
  tags text[],
  scores int[],
  small_list smallint[],
  ratios real[],
  dates date[],
  stamps timestamptz[],
  small smallint,
  ratio real,
  weight double precision,
  price numeric(12,2),
  cash money,
  size shirt_size,
  seats positive_int,
  settings jsonb,
  raw json,
  avatar bytea,
  last_ip inet,
  quiet_from time,
  quiet_tz timetz,
  session_length interval,
  birthday date,
  code char(3) CHECK (code IN ('ABC', 'XYZ')),
  only_value text CHECK (only_value = 'x'),
  free_text text CHECK (free_text <> 'forbidden'),
  full_name text GENERATED ALWAYS AS (handle || '!') STORED
);

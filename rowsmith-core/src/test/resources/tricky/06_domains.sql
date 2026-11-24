-- Domains: with checks, NOT NULL, over varchar, and a domain of a domain.
-- A domain CHECK with OR fails clearly: the test sets that column.
-- expect plan: CANNOT_GENERATE
CREATE DOMAIN positive_int AS integer CHECK (VALUE > 0);
CREATE DOMAIN percentage AS numeric(5,2) CHECK (VALUE >= 0 AND VALUE <= 100);
CREATE DOMAIN short_code AS varchar(5) NOT NULL CHECK (VALUE ~ '^[A-Z]+$');
CREATE DOMAIN small_positive AS positive_int CHECK (VALUE < 100);
CREATE DOMAIN us_postal AS text CHECK (VALUE ~ '^\d{5}$' OR VALUE ~ '^\d{5}-\d{4}$');
CREATE TABLE plan (
  id serial PRIMARY KEY,
  code short_code,
  seats positive_int NOT NULL,
  rate percentage NOT NULL,
  max_projects small_positive NOT NULL,
  zip us_postal NOT NULL
);

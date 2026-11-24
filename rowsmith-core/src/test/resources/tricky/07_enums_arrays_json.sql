-- Enums, arrays (incl. enum arrays), json/jsonb, uuid, bytea, inet, citext, interval, time.
CREATE EXTENSION IF NOT EXISTS citext;
CREATE TYPE mood AS ENUM ('sad', 'ok', 'happy');
CREATE TABLE profile (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  handle citext NOT NULL UNIQUE,
  current_mood mood NOT NULL,
  past_moods mood[] NOT NULL,
  tags text[] NOT NULL,
  scores int[] NOT NULL,
  settings jsonb NOT NULL,
  raw json NOT NULL,
  avatar bytea NOT NULL,
  last_ip inet NOT NULL,
  quiet_from time NOT NULL,
  session_length interval NOT NULL,
  external_id uuid NOT NULL UNIQUE
);

-- Partial unique indexes: uniqueness only where the predicate holds.
CREATE TABLE app_user (
  id serial PRIMARY KEY,
  email text NOT NULL,
  deleted_at timestamptz
);
CREATE UNIQUE INDEX app_user_email_live ON app_user (lower(email)) WHERE deleted_at IS NULL;
CREATE TABLE subscription (
  id serial PRIMARY KEY,
  user_id int NOT NULL REFERENCES app_user(id),
  plan text NOT NULL CHECK (plan IN ('FREE', 'PRO')),
  active boolean NOT NULL
);
CREATE UNIQUE INDEX one_active_subscription ON subscription (user_id) WHERE active;

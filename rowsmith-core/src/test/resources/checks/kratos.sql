-- case: kratos/identity_recovery_tokens.identity_recovery_tokens_token_type_ck
-- source: https://github.com/ory/kratos/tree/b86338da04a040247a07f46100a86dcfb3875909/persistence/sql/migrations/sql
-- license: Apache-2.0
-- expect: CLEAR_ERROR
CREATE TABLE identity_recovery_tokens (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  token_type integer NOT NULL DEFAULT 0,
  CONSTRAINT identity_recovery_tokens_token_type_ck CHECK (((token_type = 1) OR (token_type = 2)))
);

-- case: kratos/identities.identities_external_id_check
-- source: https://github.com/ory/kratos/tree/b86338da04a040247a07f46100a86dcfb3875909/persistence/sql/migrations/sql
-- license: Apache-2.0
CREATE TABLE identities (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  external_id character varying(64),
  CONSTRAINT identities_external_id_check CHECK (((external_id IS NULL) OR ((external_id)::text <> ''::text)))
);

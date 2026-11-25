-- case: postgrest/domain.color
-- source: https://github.com/PostgREST/postgrest/blob/623c74438b6cf6983d1fc05bb8f97843f5d25c31/test/spec/fixtures/schema.sql
-- license: MIT
CREATE DOMAIN color AS integer CHECK (((VALUE >= 0) AND (VALUE <= 16777215)));
CREATE TABLE datarep_todos (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  label_color color
);

-- case: postgrest/domain.tsvector_not_empty
-- source: https://github.com/PostgREST/postgrest/blob/623c74438b6cf6983d1fc05bb8f97843f5d25c31/test/spec/fixtures/schema.sql
-- license: MIT
CREATE DOMAIN tsvector_not_null AS tsvector CHECK ((VALUE IS NOT NULL));
CREATE DOMAIN tsvector_not_empty AS tsvector_not_null CHECK (((VALUE)::tsvector <> ''::tsvector));
CREATE TABLE tsearch_to_tsvector (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  text_search_rec_domain tsvector_not_empty
);

-- case: postgrest/domain.tsvector_not_null
-- source: https://github.com/PostgREST/postgrest/blob/623c74438b6cf6983d1fc05bb8f97843f5d25c31/test/spec/fixtures/schema.sql
-- license: MIT
CREATE DOMAIN tsvector_not_null AS tsvector CHECK ((VALUE IS NOT NULL));
CREATE TABLE tsearch_to_tsvector (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  text_search_domain tsvector_not_null
);

-- case: postgrest/public_consumers.public_consumers_name_check
-- source: https://github.com/PostgREST/postgrest/blob/623c74438b6cf6983d1fc05bb8f97843f5d25c31/test/spec/fixtures/schema.sql
-- license: MIT
CREATE TABLE public_consumers (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name text NOT NULL,
  CONSTRAINT public_consumers_name_check CHECK ((name <> ''::text))
);

-- case: postgrest-big/pop.zulaessige_x_koordinate
-- source: https://github.com/PostgREST/postgrest/blob/623c74438b6cf6983d1fc05bb8f97843f5d25c31/test/io/fixtures/big_schema.sql
-- license: MIT
CREATE TABLE pop (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  x integer,
  CONSTRAINT zulaessige_x_koordinate CHECK (((x IS NULL) OR ((x > 2485071) AND (x < 2828516))))
);

-- case: postgrest-big/user.proper_email
-- source: https://github.com/PostgREST/postgrest/blob/623c74438b6cf6983d1fc05bb8f97843f5d25c31/test/io/fixtures/big_schema.sql
-- license: MIT
CREATE TABLE "user" (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email text,
  CONSTRAINT proper_email CHECK ((email ~* '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+[.][A-Za-z]+$'::text))
);

-- case: postgrest-big/user.user_role_check
-- source: https://github.com/PostgREST/postgrest/blob/623c74438b6cf6983d1fc05bb8f97843f5d25c31/test/io/fixtures/big_schema.sql
-- license: MIT
CREATE TABLE "user" (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  role name,
  CONSTRAINT user_role_check CHECK ((length((role)::text) < 512))
);

-- case: postgrest-big/user.user_pass_check
-- source: https://github.com/PostgREST/postgrest/blob/623c74438b6cf6983d1fc05bb8f97843f5d25c31/test/io/fixtures/big_schema.sql
-- license: MIT
CREATE TABLE "user" (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pass text,
  CONSTRAINT user_pass_check CHECK ((length(pass) > 5))
);

-- Range- and list-partitioned tables (no default partition): values must land in a partition.
-- expect measurement: CANNOT_GENERATE
-- expect ticket: CANNOT_GENERATE
CREATE TABLE measurement (
  id bigint GENERATED ALWAYS AS IDENTITY,
  taken_on date NOT NULL,
  value numeric NOT NULL,
  PRIMARY KEY (id, taken_on)
) PARTITION BY RANGE (taken_on);
CREATE TABLE measurement_2023 PARTITION OF measurement FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');
CREATE TABLE measurement_2024 PARTITION OF measurement FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE ticket (
  id serial,
  region text NOT NULL,
  subject text NOT NULL,
  PRIMARY KEY (id, region)
) PARTITION BY LIST (region);
CREATE TABLE ticket_eu PARTITION OF ticket FOR VALUES IN ('eu-west', 'eu-central');
CREATE TABLE ticket_us PARTITION OF ticket FOR VALUES IN ('us-east');
CREATE TABLE audit_event (
  id serial,
  kind text NOT NULL,
  PRIMARY KEY (id, kind)
) PARTITION BY LIST (kind);
CREATE TABLE audit_event_login PARTITION OF audit_event FOR VALUES IN ('login');
CREATE TABLE audit_event_rest PARTITION OF audit_event DEFAULT;
CREATE TABLE session_token (
  id serial,
  token text NOT NULL,
  PRIMARY KEY (id, token)
) PARTITION BY HASH (token);
CREATE TABLE session_token_0 PARTITION OF session_token FOR VALUES WITH (MODULUS 2, REMAINDER 0);
CREATE TABLE session_token_1 PARTITION OF session_token FOR VALUES WITH (MODULUS 2, REMAINDER 1);

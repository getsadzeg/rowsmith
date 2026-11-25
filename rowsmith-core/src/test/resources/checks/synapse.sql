-- case: synapse/appservice_stream_position.appservice_stream_position_lock_check
-- source: https://github.com/matrix-org/synapse/blob/be65a8ec0195955c15fdb179c9158b187638e39a/synapse/storage/schema/main/full_schemas/72/full.sql.postgres
-- license: Apache-2.0
CREATE TABLE appservice_stream_position (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  "lock" character(1) NOT NULL DEFAULT 'X'::bpchar,
  CONSTRAINT appservice_stream_position_lock_check CHECK ((lock = 'X'::bpchar))
);

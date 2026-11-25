-- case: pg_partman/part_config.control_constraint_col_chk
-- source: https://github.com/pgpartman/pg_partman/blob/e0a6c384a6c77846d2560f9251089339c9637814/sql/tables/tables.sql
-- license: PostgreSQL
CREATE TABLE part_config (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  control text NOT NULL,
  constraint_cols text[],
  CONSTRAINT control_constraint_col_chk CHECK (((constraint_cols @> ARRAY[control]) <> true))
);

-- case: pg_partman/part_config.retention_schema_not_empty_chk
-- source: https://github.com/pgpartman/pg_partman/blob/e0a6c384a6c77846d2560f9251089339c9637814/sql/tables/tables.sql
-- license: PostgreSQL
CREATE TABLE part_config (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  retention_schema text,
  CONSTRAINT retention_schema_not_empty_chk CHECK ((retention_schema <> ''::text))
);

-- case: pg_partman/part_config.positive_premake_check
-- source: https://github.com/pgpartman/pg_partman/blob/e0a6c384a6c77846d2560f9251089339c9637814/sql/tables/tables.sql
-- license: PostgreSQL
CREATE TABLE part_config (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  premake integer NOT NULL DEFAULT 4,
  CONSTRAINT positive_premake_check CHECK ((premake > 0))
);

-- case: pg_partman/part_config.part_config_automatic_maintenance_check
-- source: https://github.com/pgpartman/pg_partman/blob/e0a6c384a6c77846d2560f9251089339c9637814/sql/tables/tables.sql
-- license: PostgreSQL
CREATE OR REPLACE FUNCTION check_automatic_maintenance_value(p_automatic_maintenance text)
 RETURNS boolean
 LANGUAGE plpgsql
 IMMUTABLE
 SET search_path TO 'partman', 'pg_catalog', 'pg_temp'
AS $function$
DECLARE
v_result    boolean;
BEGIN
    SELECT p_automatic_maintenance IN ('on', 'off') INTO v_result;
    RETURN v_result;
END
$function$;
CREATE TABLE part_config (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  automatic_maintenance text NOT NULL DEFAULT 'on'::text,
  CONSTRAINT part_config_automatic_maintenance_check CHECK (check_automatic_maintenance_value(automatic_maintenance))
);

-- case: pg_partman/part_config.part_config_epoch_check
-- source: https://github.com/pgpartman/pg_partman/blob/e0a6c384a6c77846d2560f9251089339c9637814/sql/tables/tables.sql
-- license: PostgreSQL
CREATE OR REPLACE FUNCTION check_epoch_type(p_type text)
 RETURNS boolean
 LANGUAGE plpgsql
 IMMUTABLE
 SET search_path TO 'partman', 'pg_catalog', 'pg_temp'
AS $function$
DECLARE
v_result    boolean;
BEGIN
    SELECT p_type IN ('none', 'seconds', 'milliseconds', 'microseconds', 'nanoseconds') INTO v_result;
    RETURN v_result;
END
$function$;
CREATE TABLE part_config (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  epoch text NOT NULL DEFAULT 'none'::text,
  CONSTRAINT part_config_epoch_check CHECK (check_epoch_type(epoch))
);

-- case: pg_partman/part_config.part_config_type_check
-- source: https://github.com/pgpartman/pg_partman/blob/e0a6c384a6c77846d2560f9251089339c9637814/sql/tables/tables.sql
-- license: PostgreSQL
-- expect: CLEAR_ERROR
CREATE OR REPLACE FUNCTION check_partition_type(p_type text)
 RETURNS boolean
 LANGUAGE plpgsql
 IMMUTABLE
 SET search_path TO 'partman', 'pg_catalog', 'pg_temp'
AS $function$
DECLARE
v_result    boolean;
BEGIN
    SELECT p_type IN ('range', 'list') INTO v_result;
    RETURN v_result;
END
$function$;
CREATE TABLE part_config (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  partition_type text NOT NULL,
  CONSTRAINT part_config_type_check CHECK (check_partition_type(partition_type))
);

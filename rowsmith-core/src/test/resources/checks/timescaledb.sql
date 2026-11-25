-- case: timescaledb/dimension.dimension_check
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
CREATE TABLE dimension (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  partitioning_func_schema name,
  partitioning_func name,
  CONSTRAINT dimension_check CHECK ((((partitioning_func_schema IS NULL) AND (partitioning_func IS NULL)) OR ((partitioning_func_schema IS NOT NULL) AND (partitioning_func IS NOT NULL))))
);

-- case: timescaledb/dimension.dimension_compress_interval_length_check
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
CREATE TABLE dimension (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  compress_interval_length bigint,
  CONSTRAINT dimension_compress_interval_length_check CHECK (((compress_interval_length IS NULL) OR (compress_interval_length > 0)))
);

-- case: timescaledb/chunk_column_stats.chunk_column_stats_range_check
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
CREATE TABLE chunk_column_stats (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  range_start bigint NOT NULL,
  range_end bigint NOT NULL,
  CONSTRAINT chunk_column_stats_range_check CHECK ((range_start <= range_end))
);

-- case: timescaledb/dimension.dimension_check1
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
-- expect: CLEAR_ERROR
CREATE TABLE dimension (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  num_slices smallint,
  interval_length bigint,
  CONSTRAINT dimension_check1 CHECK ((((num_slices IS NULL) AND (interval_length IS NOT NULL)) OR ((num_slices IS NOT NULL) AND (interval_length IS NULL))))
);

-- case: timescaledb/compression_settings.compression_settings_check_orderby_null
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
CREATE TABLE compression_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  orderby text[],
  orderby_desc boolean[],
  orderby_nullsfirst boolean[],
  CONSTRAINT compression_settings_check_orderby_null CHECK ((((orderby IS NULL) AND (orderby_desc IS NULL) AND (orderby_nullsfirst IS NULL)) OR ((orderby IS NOT NULL) AND (orderby_desc IS NOT NULL) AND (orderby_nullsfirst IS NOT NULL))))
);

-- case: timescaledb/compression_settings.compression_settings_check_orderby_cardinality
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
CREATE TABLE compression_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  orderby text[],
  orderby_desc boolean[],
  orderby_nullsfirst boolean[],
  CONSTRAINT compression_settings_check_orderby_cardinality CHECK (((array_ndims(orderby) = 1) AND (array_ndims(orderby_desc) = 1) AND (array_ndims(orderby_nullsfirst) = 1) AND (cardinality(orderby) = cardinality(orderby_desc)) AND (cardinality(orderby) = cardinality(orderby_nullsfirst))))
);

-- case: timescaledb/continuous_aggs_bucket_function.continuous_aggs_bucket_function_func_check
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
-- requires: PostgreSQL 16
-- Before PostgreSQL 16, to_regprocedure raises an error on text that is not a function signature instead of returning NULL.
CREATE TABLE continuous_aggs_bucket_function (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  bucket_func text NOT NULL,
  CONSTRAINT continuous_aggs_bucket_function_func_check CHECK (((to_regprocedure(bucket_func))::oid IS DISTINCT FROM (0)::oid))
);

-- case: timescaledb/compression_settings.compression_settings_check_segmentby
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
CREATE TABLE compression_settings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  segmentby text[],
  CONSTRAINT compression_settings_check_segmentby CHECK ((array_ndims(segmentby) = 1))
);

-- case: timescaledb/hypertable.hypertable_schema_name_check
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
CREATE TABLE hypertable (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  schema_name name NOT NULL,
  CONSTRAINT hypertable_schema_name_check CHECK ((schema_name <> '_timescaledb_catalog'::name))
);

-- case: timescaledb/hypertable.hypertable_num_dimensions_check
-- source: https://github.com/timescale/timescaledb/blob/0905a7638d4cff33f0fdb84c8aa0b6ef9ee5c6f9/sql/pre_install/tables.sql
-- license: Apache-2.0
CREATE TABLE hypertable (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  num_dimensions smallint NOT NULL,
  CONSTRAINT hypertable_num_dimensions_check CHECK ((num_dimensions > 0))
);

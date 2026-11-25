-- case: postgres/check2_tbl.sequence_con
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/constraints.sql
-- license: PostgreSQL
CREATE TABLE check2_tbl (x int, y text, z int,
  CONSTRAINT sequence_con CHECK (x > 3 and y <> 'check failed' and z < 8));

-- case: postgres/insert_tbl.insert_tbl_con
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/constraints.sql
-- license: PostgreSQL
CREATE TABLE insert_tbl (
  x int DEFAULT 7,
  y text DEFAULT '-NULL-',
  z int DEFAULT -7,
  CONSTRAINT insert_tbl_con CHECK (x >= 3 AND y <> 'check failed' AND x < 8),
  CHECK (x + z = 0));

-- case: postgres/copy_tbl.copy_con
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/constraints.sql
-- license: PostgreSQL
CREATE TABLE copy_tbl (x int, y text, z int,
  CONSTRAINT copy_con CHECK (x > 3 AND y <> 'check failed' AND x < 7));

-- case: postgres/check_tbl.check_con
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/constraints.sql
-- license: PostgreSQL
CREATE TABLE check_tbl (x int NOT NULL, CONSTRAINT check_con CHECK (x > 3));

-- case: postgres/sys_col_check_tbl.no_capital
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/constraints.sql
-- license: PostgreSQL
CREATE TABLE sys_col_check_tbl (city text NOT NULL, state text NOT NULL, is_capital bool NOT NULL, altitude int,
  CHECK (NOT (is_capital AND tableoid::regclass::text = 'sys_col_check_tbl')));

-- case: postgres/atacc1.atacc_test1
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE atacc1 (test int NOT NULL, test2 int NOT NULL, test3 int NOT NULL,
  CONSTRAINT atacc_test1 CHECK (test + test2 < test3 * 4));

-- case: postgres/atacc1.atacc1_constr_or
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE atacc1 (test_a int, test_b int,
  CONSTRAINT atacc1_constr_or CHECK (test_a IS NOT NULL OR test_b < 10));

-- case: postgres/atacc1.test2_gt_test
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE atacc1 (test int NOT NULL, test2 int NOT NULL, CHECK (test2 > test));

-- case: postgres/tt8.f_between
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE tt8 (f int NOT NULL, CHECK (f BETWEEN 0 AND 10));

-- case: postgres/nv_parent.d_between_dates
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE nv_parent (d date NOT NULL,
  CHECK (d BETWEEN '2001-01-01'::date AND '2099-12-31'::date),
  CHECK (d BETWEEN '2011-01-01'::date AND '2011-12-31'::date));

-- case: postgres/mlparted.check_b
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE mlparted (a int NOT NULL, b int NOT NULL, CONSTRAINT check_b CHECK (a = 1 AND b < 45));

-- case: postgres/part_5.check_a
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE part_5 (a int NOT NULL, b char NOT NULL, CONSTRAINT check_a CHECK (a IS NOT NULL AND a = 5));

-- case: postgres/list_parted2.check_b
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE list_parted2 (a int NOT NULL, b char(2) NOT NULL,
  CONSTRAINT check_b CHECK (b <> 'zz'), CONSTRAINT check_a2 CHECK (a > 0), CONSTRAINT check_a CHECK (a IN (5, 6)));

-- case: postgres/defcheck_def.b_le_0
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE defcheck_def (a int NOT NULL, b int, CHECK (b <= 0 AND b IS NOT NULL));

-- case: postgres/attmp3.b_range
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/alter_table.sql
-- license: PostgreSQL
CREATE TABLE attmp3 (a int, b int NOT NULL,
  CONSTRAINT b_greater_than_ten CHECK (b > 10), CONSTRAINT b_le_20 CHECK (b <= 20));

-- case: postgres/test_inh_check.bmerged
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/inherit.sql
-- license: PostgreSQL
CREATE TABLE test_inh_check (a float NOT NULL CHECK (a > 10.2), b float NOT NULL,
  CONSTRAINT bmerged CHECK (b > 1), CONSTRAINT blocal CHECK (b < 1000));

-- case: postgres/domain.positiveint
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
CREATE DOMAIN positiveint int4 CHECK (value > 0);
CREATE DOMAIN weirdfloat float8 CHECK ((1 / value) < 10);
CREATE TABLE thethable (x positiveint NOT NULL, y weirdfloat NOT NULL);

-- case: postgres/domain.dcheck
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
CREATE DOMAIN dnotnull varchar(15) NOT NULL;
CREATE DOMAIN dcheck varchar(15) NOT NULL CHECK (VALUE = 'a' OR VALUE = 'c' OR VALUE = 'd');
CREATE TABLE nulltest (col1 dnotnull, col3 dnotnull NOT NULL, col4 dcheck, col5 dcheck CHECK (col5 IN ('c', 'd')));

-- case: postgres/domain.dtop
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
CREATE DOMAIN vchar4 varchar(4);
CREATE DOMAIN dinter vchar4 CHECK (substring(VALUE, 1, 1) = 'x');
CREATE DOMAIN dtop dinter CHECK (substring(VALUE, 2, 1) = '1');
CREATE TABLE dtest (f1 dtop NOT NULL);

-- case: postgres/domain.posint2
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
-- expect: CLEAR_ERROR
CREATE DOMAIN posint AS int4 CHECK (value > 0);
CREATE DOMAIN posint2 AS posint CHECK (value % 2 = 0);
CREATE TABLE ddtest2 (f1 posint2 NOT NULL);

-- case: postgres/domain.orderedpair
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
CREATE DOMAIN orderedpair AS int[2] CHECK (value[1] < value[2]);
CREATE TABLE op (f1 orderedpair NOT NULL);

-- case: postgres/domain.str_domain2
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
CREATE DOMAIN str_domain2 AS text CHECK (value <> 'foo') DEFAULT 'bar';
CREATE TABLE domain_test (a int NOT NULL, b str_domain2 NOT NULL);

-- case: postgres/domain.pos_int_not_null
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
CREATE DOMAIN pos_int AS int4 CHECK (value > 0) NOT NULL;
CREATE TABLE pos_int_holder (f1 pos_int);

-- case: postgres/domain.con_range
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
CREATE DOMAIN con AS integer;
ALTER DOMAIN con ADD CHECK (VALUE > 0);
ALTER DOMAIN con ADD CONSTRAINT t CHECK (VALUE < 34);
CREATE TABLE domcontest (col1 con NOT NULL);

-- case: postgres/domain.things_meow
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/domain.sql
-- license: PostgreSQL
CREATE DOMAIN things AS int;
ALTER DOMAIN things ADD CONSTRAINT meow CHECK (VALUE < 11);
CREATE TABLE thethings (stuff things NOT NULL);

-- case: postgres/create_table.check_a_length
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/create_table.sql
-- license: PostgreSQL
CREATE TABLE part_b (a text NOT NULL, b int NOT NULL,
  CONSTRAINT check_a CHECK (length(a) > 0), CONSTRAINT check_b CHECK (b >= 0));

-- case: postgres/create_table.check_b_null_or_a
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/create_table.sql
-- license: PostgreSQL
CREATE TABLE parted_check (a int NOT NULL, b text, CONSTRAINT check_b CHECK (b IS NULL OR b = 'a'));

-- case: postgres/create_table.check_1
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/create_table.sql
-- license: PostgreSQL
CREATE TABLE part_1 (a int, b int NOT NULL CHECK (b >= 1 AND b <= 10), CONSTRAINT check_1 CHECK (a IS NOT NULL AND a = 1));

-- case: postgres/create_table.b_10_to_18
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/create_table.sql
-- license: PostgreSQL
CREATE TABLE part_2 (a int NOT NULL CHECK (a IN (3)), b int NOT NULL CHECK (b >= 10 AND b < 18));

-- case: postgres/insert.shdata_small
-- source: https://github.com/postgres/postgres/blob/2bdbde8b3bd782ec18168e19b70ccaa945ab0177/src/test/regress/sql/insert.sql
-- license: PostgreSQL
CREATE TABLE insertconflicttest (key int NOT NULL, fruit text NOT NULL, shdata int NOT NULL, CONSTRAINT shdata_small CHECK (shdata < 3));

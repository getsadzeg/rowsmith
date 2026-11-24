-- expect shape: UNSUPPORTED_TYPE
-- A NOT NULL column of a type Rowsmith cannot generate (geometric point) and no default.
-- Nullable unsupported columns are left NULL.
CREATE TABLE shape (id serial PRIMARY KEY, center point NOT NULL);
CREATE TABLE marker (id serial PRIMARY KEY, location point, label text NOT NULL);

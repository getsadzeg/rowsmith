-- expect a: REQUIRED_FK_CYCLE
-- expect b: REQUIRED_FK_CYCLE
-- expect c: REQUIRED_FK_CYCLE
-- A required, non-deferrable FK cycle: no insert order exists. Rowsmith must say so clearly.
CREATE TABLE a (id serial PRIMARY KEY, b_id int NOT NULL);
CREATE TABLE b (id serial PRIMARY KEY, a_id int NOT NULL REFERENCES a(id));
ALTER TABLE a ADD CONSTRAINT a_b_fk FOREIGN KEY (b_id) REFERENCES b(id);
CREATE TABLE c (id serial PRIMARY KEY, a_id int NOT NULL REFERENCES a(id));

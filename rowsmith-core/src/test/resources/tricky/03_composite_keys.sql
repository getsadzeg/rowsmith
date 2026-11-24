-- Composite primary, foreign and unique keys, and a join table made only of FKs.
CREATE TABLE sales_order (
  region char(2) NOT NULL,
  order_no int NOT NULL,
  placed_at timestamp NOT NULL,
  PRIMARY KEY (region, order_no)
);
CREATE TABLE order_line (
  region char(2) NOT NULL,
  order_no int NOT NULL,
  line_no int NOT NULL,
  sku text NOT NULL,
  quantity int NOT NULL CHECK (quantity > 0),
  PRIMARY KEY (region, order_no, line_no),
  FOREIGN KEY (region, order_no) REFERENCES sales_order (region, order_no)
);
CREATE TABLE post (id serial PRIMARY KEY, title text NOT NULL);
CREATE TABLE tag (id serial PRIMARY KEY, name text NOT NULL UNIQUE);
CREATE TABLE post_tag (
  post_id int NOT NULL REFERENCES post(id),
  tag_id int NOT NULL REFERENCES tag(id),
  PRIMARY KEY (post_id, tag_id)
);
CREATE TABLE price (
  currency char(3) NOT NULL,
  valid_from date NOT NULL,
  amount numeric(10,2) NOT NULL,
  UNIQUE (currency, valid_from)
);

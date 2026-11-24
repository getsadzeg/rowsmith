-- Quoted identifiers, reserved words, mixed case, wide tables.
CREATE TABLE "Order" (
  "Id" serial PRIMARY KEY,
  "user" text NOT NULL,
  "select" int NOT NULL,
  "Created At" timestamp NOT NULL
);
CREATE TABLE "order item" (
  id serial PRIMARY KEY,
  "orderId" int NOT NULL REFERENCES "Order"("Id"),
  qty int NOT NULL CHECK (qty > 0)
);

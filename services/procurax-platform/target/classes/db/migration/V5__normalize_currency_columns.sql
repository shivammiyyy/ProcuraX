-- Use VARCHAR to match Java String mappings without PostgreSQL CHAR padding semantics.
ALTER TABLE products ALTER COLUMN currency TYPE VARCHAR(3) USING btrim(currency);
ALTER TABLE rfqs ALTER COLUMN currency TYPE VARCHAR(3) USING btrim(currency);
ALTER TABLE quotations ALTER COLUMN currency TYPE VARCHAR(3) USING btrim(currency);

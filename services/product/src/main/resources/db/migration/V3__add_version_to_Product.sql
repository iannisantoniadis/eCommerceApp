ALTER TABLE product ADD COLUMN version BIGINT DEFAULT 0;
UPDATE product SET version = 0 WHERE version IS NULL;
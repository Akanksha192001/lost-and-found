-- Update image_data column to support larger base64 encoded images
-- Run this migration if you have existing data in your database

-- For MySQL/MariaDB
-- If you already have image_url column, rename it first
ALTER TABLE lost_items CHANGE COLUMN image_url image_data TEXT;
ALTER TABLE found_items CHANGE COLUMN image_url image_data TEXT;
ALTER TABLE returned_items CHANGE COLUMN image_url image_data TEXT;

-- If starting fresh or image_url doesn't exist, just modify:
-- ALTER TABLE lost_items MODIFY COLUMN image_data TEXT;
-- ALTER TABLE found_items MODIFY COLUMN image_data TEXT;
-- ALTER TABLE returned_items MODIFY COLUMN image_data TEXT;

-- For PostgreSQL (if using PostgreSQL instead)
-- ALTER TABLE lost_items RENAME COLUMN image_url TO image_data;
-- ALTER TABLE lost_items ALTER COLUMN image_data TYPE TEXT;
-- ALTER TABLE found_items RENAME COLUMN image_url TO image_data;
-- ALTER TABLE found_items ALTER COLUMN image_data TYPE TEXT;
-- ALTER TABLE returned_items RENAME COLUMN image_url TO image_data;
-- ALTER TABLE returned_items ALTER COLUMN image_data TYPE TEXT;

-- For H2 Database (if using H2 for testing)
-- ALTER TABLE lost_items ALTER COLUMN image_url RENAME TO image_data;
-- ALTER TABLE lost_items ALTER COLUMN image_data SET DATA TYPE TEXT;
-- ALTER TABLE found_items ALTER COLUMN image_url RENAME TO image_data;
-- ALTER TABLE found_items ALTER COLUMN image_data SET DATA TYPE TEXT;
-- ALTER TABLE returned_items ALTER COLUMN image_url RENAME TO image_data;
-- ALTER TABLE returned_items ALTER COLUMN image_data SET DATA TYPE TEXT;

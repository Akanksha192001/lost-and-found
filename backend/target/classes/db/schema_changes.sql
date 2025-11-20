-- Schema changes for ADMIN reporter/owner fields functionality
-- These changes will be automatically applied by Hibernate with ddl-auto=update

-- Remove address fields from both tables (no longer needed)
-- ALTER TABLE lost_items DROP COLUMN owner_address;
-- ALTER TABLE found_items DROP COLUMN reporter_address;

-- Field usage:
-- For lost_items:
-- - owner_name/owner_email: Person who actually lost the item
-- - reported_by: The authenticated user who submitted the form (ADMIN or regular user)

-- For found_items:
-- - reporter_name/reporter_email: Person who found the item (may be different from authenticated user if ADMIN reports on behalf)
-- - reported_by: The authenticated user who submitted the form (ADMIN or regular user)
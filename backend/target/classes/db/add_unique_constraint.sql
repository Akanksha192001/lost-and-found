-- Clean up duplicate matches (keep the most recent one for each lost/found pair)
DELETE FROM item_matches 
WHERE id NOT IN (
    SELECT MAX(id) 
    FROM item_matches 
    GROUP BY lost_item_id, found_item_id
);

-- Add unique constraint to prevent duplicate matches for the same lost/found pair
-- Note: This allows multiple TENTATIVE matches but only one CONFIRMED match per found item
ALTER TABLE item_matches 
ADD CONSTRAINT unique_lost_found_pair 
UNIQUE (lost_item_id, found_item_id);

-- Optional: Add index on found_item_id and status for faster lookups
CREATE INDEX IF NOT EXISTS idx_found_item_status 
ON item_matches(found_item_id, status);

-- Migration: Remove redundant FK columns from handoff_queue
-- These columns are redundant because items can be accessed through the match relationship
-- handoff_queue.match_id -> item_matches -> (lost_item_id, found_item_id)

-- Step 1: Make match_id NOT NULL (handoff must have a match)
-- Note: Ensure all existing handoffs have a match_id before running this
UPDATE handoff_queue SET match_id = (
    SELECT id FROM item_matches 
    WHERE item_matches.lost_item_id = handoff_queue.lost_item_id 
      AND item_matches.found_item_id = handoff_queue.found_item_id 
    LIMIT 1
) WHERE match_id IS NULL;

-- Step 2: Remove the foreign key constraints
ALTER TABLE handoff_queue DROP CONSTRAINT IF EXISTS fk_handoff_lost_item;
ALTER TABLE handoff_queue DROP CONSTRAINT IF EXISTS fk_handoff_found_item;

-- Step 3: Drop the redundant columns
ALTER TABLE handoff_queue DROP COLUMN IF EXISTS lost_item_id;
ALTER TABLE handoff_queue DROP COLUMN IF EXISTS found_item_id;

-- Step 4: Make match_id NOT NULL
ALTER TABLE handoff_queue ALTER COLUMN match_id SET NOT NULL;

-- Step 5: Add foreign key constraint for match_id (if not already exists)
ALTER TABLE handoff_queue 
ADD CONSTRAINT fk_handoff_match 
FOREIGN KEY (match_id) 
REFERENCES item_matches(id) 
ON DELETE CASCADE;

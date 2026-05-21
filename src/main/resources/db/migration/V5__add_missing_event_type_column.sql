-- Ensure event_type column exists in fire_events table
-- Redundant if V4 ran successfully, but kept for safety in a cross-DB compatible way
ALTER TABLE fire_events ADD COLUMN IF NOT EXISTS event_type VARCHAR(50);

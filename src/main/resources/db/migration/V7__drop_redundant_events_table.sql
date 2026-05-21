-- Drop the redundant events table that was likely created by Hibernate auto-DDL
-- This table is not used by the current backend models and contains legacy/inconsistent columns.
DROP TABLE IF EXISTS events;

-- Fix potential invalid check constraint on fire_events if it was created incorrectly by Hibernate
-- Ensure water_level_pct constraint is present and correct
ALTER TABLE fire_events DROP CONSTRAINT IF EXISTS fire_events_water_level_pct_check;
ALTER TABLE fire_events ADD CONSTRAINT fire_events_water_level_pct_check CHECK (water_level_pct BETWEEN 0 AND 100);

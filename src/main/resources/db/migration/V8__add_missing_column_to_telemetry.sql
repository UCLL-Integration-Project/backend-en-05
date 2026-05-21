-- Explicitly add the missing column to telemetry table to resolve Hibernate validation errors
-- This column was supposed to be added in V4, but appears to be missing in some environments.
ALTER TABLE telemetry ADD COLUMN IF NOT EXISTS water_level_pct SMALLINT;
ALTER TABLE telemetry DROP CONSTRAINT IF EXISTS telemetry_water_level_pct_check;
ALTER TABLE telemetry ADD CONSTRAINT telemetry_water_level_pct_check CHECK (water_level_pct BETWEEN 0 AND 100);

-- Also ensure it exists in fire_events to prevent a second crash
ALTER TABLE fire_events ADD COLUMN IF NOT EXISTS water_level_pct SMALLINT;
ALTER TABLE fire_events DROP CONSTRAINT IF EXISTS fire_events_water_level_pct_check;
ALTER TABLE fire_events ADD CONSTRAINT fire_events_water_level_pct_check CHECK (water_level_pct BETWEEN 0 AND 100);

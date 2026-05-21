-- Consolidation migration to ensure all columns exist across all environments
-- This fixes any issues where migrations might have been skipped or the DB state is inconsistent.

-- Users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS token VARCHAR(255);

-- Telemetry table
ALTER TABLE telemetry ADD COLUMN IF NOT EXISTS water_level_pct SMALLINT;
ALTER TABLE telemetry ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE telemetry ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE telemetry ADD COLUMN IF NOT EXISTS accuracy_m DOUBLE PRECISION;

-- Fire Events table
ALTER TABLE fire_events ADD COLUMN IF NOT EXISTS event_type VARCHAR(50);
ALTER TABLE fire_events ADD COLUMN IF NOT EXISTS water_level_pct SMALLINT;
ALTER TABLE fire_events ADD COLUMN IF NOT EXISTS battery_pct SMALLINT;
ALTER TABLE fire_events ADD COLUMN IF NOT EXISTS duration_s INTEGER;
ALTER TABLE fire_events ADD COLUMN IF NOT EXISTS is_extinguished BOOLEAN;
ALTER TABLE fire_events ADD COLUMN IF NOT EXISTS temperature REAL;

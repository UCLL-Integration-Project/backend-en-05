ALTER TABLE telemetry
    ADD COLUMN IF NOT EXISTS water_level_pct SMALLINT CHECK (water_level_pct BETWEEN 0 AND 100);

ALTER TABLE fire_events
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(50) CHECK (event_type IN ('fire', 'low_water'));

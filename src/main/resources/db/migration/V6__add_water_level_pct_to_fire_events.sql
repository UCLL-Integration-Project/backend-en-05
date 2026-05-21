ALTER TABLE fire_events
    ADD COLUMN IF NOT EXISTS water_level_pct SMALLINT CHECK (water_level_pct BETWEEN 0 AND 100);

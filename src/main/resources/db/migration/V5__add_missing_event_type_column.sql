-- Ensure event_type column exists (no-op if V4 already ran)
ALTER TABLE fire_events
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(50);

-- V4 added event_type with a CHECK constraint; V5 added the column without one.
-- Ensure the CHECK constraint exists regardless of which migration ran first.
ALTER TABLE fire_events
    DROP CONSTRAINT IF EXISTS fire_events_event_type_check;

ALTER TABLE fire_events
    ADD CONSTRAINT fire_events_event_type_check
        CHECK (event_type IN ('fire', 'low_water'));

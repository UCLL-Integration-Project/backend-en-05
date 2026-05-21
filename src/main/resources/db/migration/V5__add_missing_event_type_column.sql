-- Ensure event_type column exists in fire_events table
ALTER TABLE fire_events 
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(50);

-- Optional: Add the check constraint if not present
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fire_events_event_type_check') THEN
        ALTER TABLE fire_events ADD CONSTRAINT fire_events_event_type_check CHECK (event_type IN ('fire', 'low_water'));
    END IF;
END $$;

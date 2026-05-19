CREATE TABLE IF NOT EXISTS fire_events (
    incident_id      UUID         PRIMARY KEY,
    timestamp        TIMESTAMP WITH TIME ZONE  NOT NULL,
    temperature      REAL,
    battery_pct      SMALLINT     CHECK (battery_pct BETWEEN 0 AND 100),
    duration_s       INTEGER,
    is_extinguished  BOOLEAN,
    device_id        VARCHAR(255) REFERENCES devices(device_id)
);

CREATE INDEX IF NOT EXISTS fire_events_timestamp_idx ON fire_events (timestamp DESC);

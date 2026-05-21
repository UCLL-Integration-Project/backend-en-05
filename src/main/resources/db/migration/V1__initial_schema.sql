-- Clean schema version after the other broken migration versions
-- This file replaces V1 through V10 to provide a clean starting point.

CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    token      VARCHAR(255),
    password   VARCHAR(255),
    role       VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS devices (
    device_id     VARCHAR(255) PRIMARY KEY,
    device_token  VARCHAR(255) NOT NULL,
    name          VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS telemetry (
    id               BIGSERIAL    PRIMARY KEY,
    time             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    battery_voltage  REAL,
    temperature_c    REAL,
    flame_left       SMALLINT,
    flame_center     SMALLINT,
    flame_right      SMALLINT,
    pump_active      BOOLEAN      NOT NULL DEFAULT FALSE,
    motor_left_pwm   SMALLINT,
    motor_right_pwm  SMALLINT,
    water_level_pct  SMALLINT     CHECK (water_level_pct BETWEEN 0 AND 100),
    latitude         DOUBLE PRECISION,
    longitude        DOUBLE PRECISION,
    accuracy_m       DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS fire_events (
    incident_id      UUID         PRIMARY KEY,
    timestamp        TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    temperature      REAL,
    battery_pct      SMALLINT     CHECK (battery_pct BETWEEN 0 AND 100),
    duration_s       INTEGER,
    is_extinguished  BOOLEAN,
    device_id        VARCHAR(255) REFERENCES devices(device_id),
    event_type       VARCHAR(50),
    water_level_pct  SMALLINT     CHECK (water_level_pct BETWEEN 0 AND 100)
);

CREATE INDEX IF NOT EXISTS telemetry_time_desc_idx ON telemetry (time DESC);
CREATE INDEX IF NOT EXISTS fire_events_timestamp_idx ON fire_events (timestamp DESC);

CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(255) UNIQUE,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    token      VARCHAR(255),
    password   VARCHAR(255)
);

CREATE TABLE devices (
    device_id    VARCHAR(255) PRIMARY KEY,
    device_token VARCHAR(255),
    name         VARCHAR(255)
);

CREATE TABLE telemetry (
    id               BIGSERIAL    PRIMARY KEY,
    time             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    battery_voltage  REAL,
    temperature_c    REAL,
    flame_left       SMALLINT,
    flame_center     SMALLINT,
    flame_right      SMALLINT,
    pump_active      BOOLEAN      NOT NULL DEFAULT FALSE,
    motor_left_pwm   SMALLINT,
    motor_right_pwm  SMALLINT
);

CREATE INDEX telemetry_time_desc_idx ON telemetry (time DESC);

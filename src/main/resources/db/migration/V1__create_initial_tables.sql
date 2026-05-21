CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(255) UNIQUE,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    token      VARCHAR(255),
    password   VARCHAR(255)
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
    motor_right_pwm  SMALLINT
);

CREATE INDEX IF NOT EXISTS telemetry_time_desc_idx ON telemetry (time DESC);

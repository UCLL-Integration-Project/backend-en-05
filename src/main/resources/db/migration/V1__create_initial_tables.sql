CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    token VARCHAR(255)
);

CREATE TABLE devices (
    device_id VARCHAR(255) PRIMARY KEY,
    device_token VARCHAR(255),
    name VARCHAR(255)
);

CREATE TABLE events (
    incident_id UUID PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE,
    temperature FLOAT4,
    battery_pct INT4,
    duration_s INT4,
    is_extinguished BOOLEAN,
    device_id VARCHAR(255) REFERENCES devices(device_id)
);

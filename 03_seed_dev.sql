-- =============================================================================
-- Fire-Fighting Robot — Seed Data (optional)
-- =============================================================================
-- Run this only in dev / when you want the dashboard to have something to
-- show before the real robot is sending packets. Safe to skip in production.
-- =============================================================================

-- A short fake "mission": boot → idle → driving → fire → extinguish → idle.
INSERT INTO events (time, event_type, severity, message) VALUES
    (NOW() - INTERVAL '5 minutes',  'boot',          0, 'Robot powered on'),
    (NOW() - INTERVAL '4 minutes',  'idle',          0, 'Waiting for command'),
    (NOW() - INTERVAL '3 minutes',  'driving',       0, 'Patrol mode started'),
    (NOW() - INTERVAL '2 minutes',  'fire_detected', 2, 'Flame on center sensor'),
    (NOW() - INTERVAL '110 seconds','approaching',   1, 'Aligning with target'),
    (NOW() - INTERVAL '90 seconds', 'extinguishing', 2, 'Pump active'),
    (NOW() - INTERVAL '80 seconds', 'extinguished',  1, 'No flame detected'),
    (NOW() - INTERVAL '60 seconds', 'idle',          0, 'Returning to standby');

-- A few minutes of fake telemetry at 1 Hz. Battery drains gently,
-- temperature spikes around the "fire" event.
INSERT INTO telemetry (
    time, battery_voltage, temperature_c,
    flame_left, flame_center, flame_right,
    pump_active, motor_left_pwm, motor_right_pwm
)
SELECT
    NOW() - (s || ' seconds')::INTERVAL,
    11.8 - (s * 0.001),                                 -- slow drain
    22 + CASE WHEN s BETWEEN 80 AND 120 THEN 15 ELSE 0 END,
    CASE WHEN s BETWEEN 100 AND 130 THEN 800 ELSE 50 END,
    CASE WHEN s BETWEEN 95 AND 130 THEN 950 ELSE 50 END,
    CASE WHEN s BETWEEN 100 AND 125 THEN 700 ELSE 50 END,
    s BETWEEN 80 AND 100,                               -- pump on briefly
    CASE WHEN s BETWEEN 150 AND 200 THEN 180 ELSE 0 END,
    CASE WHEN s BETWEEN 150 AND 200 THEN 180 ELSE 0 END
FROM generate_series(0, 300) AS s;

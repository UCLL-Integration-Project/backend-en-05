-- =============================================================================
-- Fire-Fighting Robot — Cleanup helper
-- =============================================================================
-- TimescaleDB's retention policy is gone, so old rows accumulate forever
-- unless something deletes them. For a demo this barely matters (you'll
-- generate maybe a few MB total), but here's a one-line cleanup you can
-- run by hand or wire to a cron job inside the FastAPI service.
--
-- Run as often as you like. Safe to run when there's nothing to delete.
-- =============================================================================

DELETE FROM events    WHERE time < NOW() - INTERVAL '7 days';
DELETE FROM telemetry WHERE time < NOW() - INTERVAL '7 days';

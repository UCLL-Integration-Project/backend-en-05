-- Drop the redundant events table that was likely created by Hibernate auto-DDL
-- This table is not used by the current backend models and contains legacy/inconsistent columns.
DROP TABLE IF EXISTS events;

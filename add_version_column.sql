-- Script to add version column to existing event table
-- This script handles the optimistic locking fix

-- Add version column to event table
ALTER TABLE event ADD COLUMN version BIGINT DEFAULT 0;

-- Update existing records to have version = 0
UPDATE event SET version = 0 WHERE version IS NULL;

-- Make version column NOT NULL after setting default values
ALTER TABLE event MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;

-- For MySQL specifically, you might need:
-- ALTER TABLE event MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;

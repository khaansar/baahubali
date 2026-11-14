-- Attempts are never soft-deleted; deleted_at was never written.
ALTER TABLE attempts DROP COLUMN deleted_at;

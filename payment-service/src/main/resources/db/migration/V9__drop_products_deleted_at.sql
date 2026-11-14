-- Products are deactivated via status; deleted_at was never written.
ALTER TABLE products DROP COLUMN deleted_at;

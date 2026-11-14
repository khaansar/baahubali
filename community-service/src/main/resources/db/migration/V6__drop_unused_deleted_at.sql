-- Reviews and FAQs are never soft-deleted; deleted_at was never written.
ALTER TABLE reviews DROP COLUMN deleted_at;
ALTER TABLE faqs DROP COLUMN deleted_at;

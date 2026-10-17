ALTER TABLE reviews
    ADD COLUMN show_on_homepage BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_reviews_show_on_homepage ON reviews(show_on_homepage);

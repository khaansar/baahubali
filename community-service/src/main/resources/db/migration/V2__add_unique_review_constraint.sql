ALTER TABLE reviews
ADD CONSTRAINT uk_user_target_review UNIQUE (user_id, target_id, target_type);
ALTER TABLE questions ADD COLUMN topic VARCHAR(100) NULL AFTER explanation;
CREATE INDEX idx_questions_topic ON questions (topic);

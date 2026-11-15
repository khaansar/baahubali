-- Keeps a question's "marked for review" flag with the attempt's saved answer,
-- so the flag survives a reload of the exam.
ALTER TABLE `attempt_answers`
  ADD COLUMN `marked_for_review` tinyint(1) NOT NULL DEFAULT '0';

ALTER TABLE question_translations
    ADD COLUMN question_images JSON NULL,
    ADD COLUMN option_images JSON NULL;

ALTER TABLE questions
    ADD COLUMN explanation_images JSON NULL;

CREATE TABLE user_daily_activity (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    activity_date DATE NOT NULL,
    CONSTRAINT uk_user_date UNIQUE (user_id, activity_date)
);

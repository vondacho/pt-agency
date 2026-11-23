-- Optimistic locking of sessions: concurrent bookings of the same session are detected
ALTER TABLE training_session ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

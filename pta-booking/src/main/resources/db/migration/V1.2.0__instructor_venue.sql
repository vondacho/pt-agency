-- Instructors leading sessions, venues where they take place
CREATE TABLE instructor
(
    id        BIGINT       NOT NULL PRIMARY KEY,
    logicalId UUID         NOT NULL UNIQUE,
    name      VARCHAR(100) NOT NULL,
    bio       VARCHAR(1000)
);
CREATE SEQUENCE instructor_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE venue
(
    id          BIGINT       NOT NULL PRIMARY KEY,
    logicalId   UUID         NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(300),
    accessNotes VARCHAR(1000),
    mapUrl      VARCHAR(500)
);
CREATE SEQUENCE venue_seq START WITH 1 INCREMENT BY 50;

-- A session's instructor, venue and own note (all optional)
ALTER TABLE training_session ADD COLUMN instructorId UUID;
ALTER TABLE training_session ADD COLUMN venueId UUID;
ALTER TABLE training_session ADD COLUMN note VARCHAR(500);

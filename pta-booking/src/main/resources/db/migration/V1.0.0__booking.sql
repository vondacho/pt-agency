CREATE SEQUENCE training_session_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE session_booking_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE training_session
(
    id              BIGINT       NOT NULL PRIMARY KEY,
    logicalId       UUID         NOT NULL UNIQUE,
    articleId       UUID         NOT NULL,
    title           VARCHAR(100) NOT NULL,
    day             DATE         NOT NULL,
    startTime       TIME         NOT NULL,
    stopTime        TIME         NOT NULL,
    durationMinutes INTEGER      NOT NULL,
    location        VARCHAR(100) NOT NULL,
    quotaMin        INTEGER,
    quotaMax        INTEGER
);

CREATE INDEX training_session_day_idx ON training_session (day, startTime);

CREATE TABLE session_booking
(
    id             BIGINT      NOT NULL PRIMARY KEY,
    session_id     BIGINT      NOT NULL REFERENCES training_session (id) ON DELETE CASCADE,
    participantId  UUID        NOT NULL,
    subscriptionId UUID        NOT NULL,
    status         VARCHAR(20) NOT NULL,
    UNIQUE (session_id, participantId)
);

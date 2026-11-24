-- What a course (session article) is about and how to take part
ALTER TABLE article ADD COLUMN level VARCHAR(50);
ALTER TABLE article ADD COLUMN prerequisites VARCHAR(500);
ALTER TABLE article ADD COLUMN whatToBring VARCHAR(500);
ALTER TABLE article ADD COLUMN cancellationHours INTEGER CHECK (cancellationHours >= 0);

-- Main objectives of a course, in order
CREATE TABLE article_objective
(
    article_id BIGINT       NOT NULL REFERENCES article (id) ON DELETE CASCADE,
    position   INTEGER      NOT NULL,
    text       VARCHAR(200) NOT NULL,
    PRIMARY KEY (article_id, position)
);

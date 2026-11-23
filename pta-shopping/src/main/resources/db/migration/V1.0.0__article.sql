CREATE SEQUENCE article_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE article
(
    id          BIGINT         NOT NULL PRIMARY KEY,
    logicalId   UUID           NOT NULL UNIQUE,
    name        VARCHAR(100)   NOT NULL,
    description VARCHAR(500)   NOT NULL,
    validFrom   DATE           NOT NULL,
    validTo     DATE,
    price       DOUBLE PRECISION NOT NULL,
    quotaMin    INTEGER,
    quotaMax    INTEGER
);

CREATE INDEX article_name_idx ON article (name);

-- Tags describing what the article is (e.g. SESSION, COURSE, SUBSCRIPTION)
CREATE TABLE article_profile
(
    article_id BIGINT      NOT NULL REFERENCES article (id) ON DELETE CASCADE,
    tag        VARCHAR(50) NOT NULL,
    PRIMARY KEY (article_id, tag)
);

CREATE INDEX article_profile_tag_idx ON article_profile (tag);

-- Tags of the articles a subscription or pass gives access to
CREATE TABLE article_grant
(
    article_id BIGINT      NOT NULL REFERENCES article (id) ON DELETE CASCADE,
    tag        VARCHAR(50) NOT NULL,
    PRIMARY KEY (article_id, tag)
);

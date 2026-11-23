-- Repair V1.0.0: technical id as primary key, sequence expected by Hibernate, unique logical id
ALTER TABLE customer ALTER COLUMN id TYPE BIGINT;
ALTER TABLE customer ALTER COLUMN id SET NOT NULL;
ALTER TABLE customer ADD PRIMARY KEY (id);
ALTER TABLE customer ALTER COLUMN logicalId SET NOT NULL;
ALTER TABLE customer ADD CONSTRAINT customer_logicalid_key UNIQUE (logicalId);

CREATE SEQUENCE IF NOT EXISTS customer_seq START WITH 1 INCREMENT BY 50;
-- start past any existing row (Hibernate allocates ids by blocks of 50)
SELECT setval('customer_seq', COALESCE((SELECT MAX(id) FROM customer), 0) + 51, false);

-- Wider columns: names, places and e-mail addresses were too short
ALTER TABLE customer ALTER COLUMN firstName TYPE VARCHAR(50);
ALTER TABLE customer ALTER COLUMN lastName TYPE VARCHAR(50);
ALTER TABLE customer ALTER COLUMN city TYPE VARCHAR(50);
ALTER TABLE customer ALTER COLUMN region TYPE VARCHAR(50);
ALTER TABLE customer ALTER COLUMN country TYPE VARCHAR(50);
ALTER TABLE customer ALTER COLUMN email TYPE VARCHAR(254);
ALTER TABLE customer ALTER COLUMN phone TYPE VARCHAR(30);

-- Billing address and notes were not persisted
ALTER TABLE customer ADD COLUMN billingStreetNo VARCHAR(50);
ALTER TABLE customer ADD COLUMN billingZip VARCHAR(15);
ALTER TABLE customer ADD COLUMN billingCity VARCHAR(50);
ALTER TABLE customer ADD COLUMN billingRegion VARCHAR(50);
ALTER TABLE customer ADD COLUMN billingCountry VARCHAR(50);
ALTER TABLE customer ADD COLUMN notes TEXT;

-- Subscriptions of a customer to an article (pass, subscription) of pta-shopping
CREATE SEQUENCE customer_subscription_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE customer_subscription
(
    id          BIGINT  NOT NULL PRIMARY KEY,
    customer_id BIGINT  NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    logicalId   UUID    NOT NULL UNIQUE,
    articleId   UUID    NOT NULL,
    validFrom   DATE    NOT NULL,
    validTo     DATE,
    quotaMin    INTEGER,
    quotaMax    INTEGER,
    credits     INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX customer_subscription_customer_idx ON customer_subscription (customer_id);

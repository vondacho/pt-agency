-- Requests of a customer for a subscription article, confirmed by the agency (payment at the desk)
CREATE SEQUENCE customer_subscription_request_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE customer_subscription_request
(
    id             BIGINT      NOT NULL PRIMARY KEY,
    customer_id    BIGINT      NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    logicalId      UUID        NOT NULL UNIQUE,
    articleId      UUID        NOT NULL,
    requestedAt    TIMESTAMP WITH TIME ZONE NOT NULL,
    status         VARCHAR(20) NOT NULL,
    subscriptionId UUID
);

CREATE INDEX customer_subscription_request_customer_idx ON customer_subscription_request (customer_id);

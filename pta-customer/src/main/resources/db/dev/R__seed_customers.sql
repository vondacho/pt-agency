-- Development and test data only. Alice holds an "Unlimited pass" (article of pta-shopping's seed), Bob has no subscription.
-- Their ids are the customer_id attribute of the matching users in pta-www's Keycloak realm.
-- Seed rows use negative technical ids so they never collide with ids allocated by Hibernate sequences.
DELETE FROM customer_subscription WHERE id < 0;
DELETE FROM customer WHERE id < 0;

INSERT INTO customer (id, logicalId, salutation, firstName, lastName, birth, gender, streetNo, zip, city, region, country, email, phone, notes)
VALUES
    (-1, '20000000-0000-0000-0000-00000000000a', 'Ms', 'Alice', 'Martin', '1990-04-12', 'FEMALE', 'Rue du Lac 12', '1003', 'Lausanne', 'VD', 'CH', 'alice@example.ch', '+41791234567', 'Seeded for development'),
    (-2, '20000000-0000-0000-0000-00000000000b', 'Mr', 'Bob', 'Keller', '1985-09-30', 'MALE', 'Bahnhofstrasse 1', '8001', 'Zurich', 'ZH', 'CH', 'bob@example.ch', '+41797654321', 'Seeded for development');

INSERT INTO customer_subscription (id, customer_id, logicalId, articleId, validFrom, validTo, quotaMin, quotaMax, credits)
VALUES
    (-1, -1, '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', current_date - 30, current_date + 335, NULL, NULL, 0);

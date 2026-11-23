-- Development and test data only. Session article ids match the articleId of pta-booking's seeded sessions;
-- subscription article ids match the articleId of pta-customer's seeded subscriptions.
-- Seed rows use negative technical ids so they never collide with ids allocated by Hibernate sequences.
DELETE FROM article WHERE id < 0;

INSERT INTO article (id, logicalId, name, description, validFrom, validTo, price, quotaMin, quotaMax)
VALUES
    (-1, '10000000-0000-0000-0000-000000000001', 'Personal training', 'One-to-one coaching session', current_date - 365, NULL, 90.00, NULL, NULL),
    (-2, '10000000-0000-0000-0000-000000000002', 'Small group hiit', 'High-intensity interval training in a small group', current_date - 365, NULL, 25.00, NULL, NULL),
    (-3, '10000000-0000-0000-0000-000000000003', 'Bootcamp', 'Outdoor circuit training', current_date - 365, NULL, 20.00, NULL, NULL),
    (-4, '10000000-0000-0000-0000-000000000004', 'Mobility & stretching', 'Mobility, flexibility and recovery', current_date - 365, NULL, 20.00, NULL, NULL),
    (-5, '50000000-0000-0000-0000-000000000001', 'Unlimited pass', 'Unlimited access to group courses and personal training for one year', current_date - 365, NULL, 1200.00, NULL, NULL),
    (-6, '50000000-0000-0000-0000-000000000002', 'Personal training 10x', 'Ten personal training sessions', current_date - 365, NULL, 800.00, 0, 10);

INSERT INTO article_profile (article_id, tag)
VALUES
    (-1, 'SESSION'), (-1, 'PERSONAL_TRAINING'),
    (-2, 'SESSION'), (-2, 'COURSE'),
    (-3, 'SESSION'), (-3, 'COURSE'),
    (-4, 'SESSION'), (-4, 'COURSE'),
    (-5, 'SUBSCRIPTION'), (-5, 'ONE_YEAR'),
    (-6, 'PASS');

INSERT INTO article_grant (article_id, tag)
VALUES
    -- the unlimited pass gives access to every session
    (-5, 'SESSION'), (-5, 'COURSE'), (-5, 'PERSONAL_TRAINING'),
    -- the 10x pass gives access to personal training only
    (-6, 'SESSION'), (-6, 'PERSONAL_TRAINING');

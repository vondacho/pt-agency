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
    (-6, '50000000-0000-0000-0000-000000000002', 'Personal training 10x', 'Ten personal training sessions, to use within a year', current_date - 365, NULL, 800.00, 0, 10),
    (-7, '50000000-0000-0000-0000-000000000003', 'Monthly group pass', 'Unlimited group courses for one month', current_date - 365, NULL, 139.00, NULL, NULL),
    (-8, '50000000-0000-0000-0000-000000000004', '10-session group card', 'Ten group courses, to use within a year', current_date - 365, NULL, 220.00, 0, 10),
    (-9, '50000000-0000-0000-0000-000000000005', 'Trial week', 'Discover our group courses for one week', current_date - 365, NULL, 29.00, NULL, NULL),
    -- no longer on sale: must not appear in the shop
    (-10, '50000000-0000-0000-0000-000000000006', 'Summer pass', 'Group courses all summer long', current_date - 365, current_date - 30, 249.00, NULL, NULL);

INSERT INTO article_profile (article_id, tag)
VALUES
    (-1, 'SESSION'), (-1, 'PERSONAL_TRAINING'),
    (-2, 'SESSION'), (-2, 'COURSE'),
    (-3, 'SESSION'), (-3, 'COURSE'),
    (-4, 'SESSION'), (-4, 'COURSE'),
    (-5, 'SUBSCRIPTION'), (-5, 'ONE_YEAR'),
    (-6, 'PASS'), (-6, 'ONE_YEAR'),
    (-7, 'SUBSCRIPTION'), (-7, 'ONE_MONTH'),
    (-8, 'PASS'), (-8, 'ONE_YEAR'),
    (-9, 'SUBSCRIPTION'), (-9, 'ONE_WEEK'),
    (-10, 'SUBSCRIPTION'), (-10, 'THREE_MONTHS');

INSERT INTO article_grant (article_id, tag)
VALUES
    -- the unlimited pass gives access to every session
    (-5, 'SESSION'), (-5, 'COURSE'), (-5, 'PERSONAL_TRAINING'),
    -- the 10x pass gives access to personal training only
    (-6, 'SESSION'), (-6, 'PERSONAL_TRAINING'),
    -- group passes give access to group courses
    (-7, 'SESSION'), (-7, 'COURSE'),
    (-8, 'SESSION'), (-8, 'COURSE'),
    (-9, 'SESSION'), (-9, 'COURSE'),
    (-10, 'SESSION'), (-10, 'COURSE');

-- Course details of the session articles
UPDATE article SET level = 'All levels', cancellationHours = 24,
    prerequisites = 'From 16 years. Tell your coach about any injury or health condition.',
    whatToBring = 'Sports clothes and indoor shoes, a towel, water.'
WHERE id = -1;
UPDATE article SET level = 'Intermediate', cancellationHours = 12,
    prerequisites = 'From 16 years. Regular physical activity recommended: high-intensity intervals.',
    whatToBring = 'Indoor shoes, a towel, water.'
WHERE id = -2;
UPDATE article SET level = 'All levels', cancellationHours = 12,
    prerequisites = 'From 14 years. Held outdoors in any weather, except storms.',
    whatToBring = 'Clothes for the weather, running shoes, water and a mat.'
WHERE id = -3;
UPDATE article SET level = 'All levels', cancellationHours = 6,
    prerequisites = 'No prerequisite: suitable for beginners and recovery.',
    whatToBring = 'Comfortable clothes. Mats are provided.'
WHERE id = -4;

INSERT INTO article_objective (article_id, position, text)
VALUES
    (-1, 0, 'A programme built on your goals and your level'),
    (-1, 1, 'Technique and posture corrected one to one'),
    (-1, 2, 'Progress measured from session to session'),
    (-2, 0, 'Raise your cardio capacity with short intense intervals'),
    (-2, 1, 'Full-body strength with body weight and kettlebells'),
    (-2, 2, 'Burn energy efficiently in under an hour'),
    (-3, 0, 'Outdoor circuits combining cardio and strength'),
    (-3, 1, 'Team spirit and motivation in a group'),
    (-4, 0, 'Gain range of motion in hips, shoulders and spine'),
    (-4, 1, 'Recover better between intense sessions'),
    (-4, 2, 'Breathing and relaxation techniques');

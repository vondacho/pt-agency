-- Development and test data only: sessions over the coming two weeks, relative to the current date.
-- Seed rows use negative technical ids so they never collide with ids allocated by Hibernate sequences.
DELETE FROM session_booking WHERE id < 0;
DELETE FROM training_session WHERE id < 0;
DELETE FROM instructor WHERE id < 0;
DELETE FROM venue WHERE id < 0;

INSERT INTO instructor (id, logicalId, name, bio)
VALUES
    (-1, '60000000-0000-0000-0000-000000000001', 'Léa Dubois',
     'Certified personal trainer for ten years, Léa builds programmes around your goals, from strength to return after an injury.'),
    (-2, '60000000-0000-0000-0000-000000000002', 'Marco Rossi',
     'Group coach and former athlete, Marco brings energy and precision to HIIT and bootcamp sessions.');

INSERT INTO venue (id, logicalId, name, address, accessNotes, mapUrl)
VALUES
    (-1, '70000000-0000-0000-0000-000000000001', 'PT Agency studio', 'Rue du Lac 12, 1003 Lausanne',
     'Ground floor, entrance on the courtyard. Studio A on the left, Studio B at the end of the corridor. Changing rooms and showers on site.',
     'https://www.openstreetmap.org/search?query=Rue%20du%20Lac%2012%2C%201003%20Lausanne'),
    (-2, '70000000-0000-0000-0000-000000000002', 'Parc de Milan', 'Avenue de Milan, 1007 Lausanne',
     'Meeting point at the fountain near the main entrance. In case of storm, the session moves to the studio.',
     'https://www.openstreetmap.org/search?query=Parc%20de%20Milan%2C%20Lausanne');

INSERT INTO training_session (id, logicalId, articleId, title, day, startTime, stopTime, durationMinutes, location, quotaMin, quotaMax)
VALUES
    (-1,  '00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Personal training',    current_date + 1,  '07:00', '08:00', 60, 'Studio A',    1, 1),
    (-2,  '00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Small group HIIT',     current_date + 1,  '18:00', '19:00', 60, 'Studio B',    2, 5),
    (-3,  '00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'Bootcamp',             current_date + 2,  '12:15', '13:00', 45, 'City park',   3, 20),
    (-4,  '00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'Small group HIIT',     current_date + 3,  '18:00', '19:00', 60, 'Studio B',    2, 5),
    (-5,  '00000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000004', 'Mobility & stretching', current_date + 4,  '09:00', '10:00', 60, 'Studio A',    3, 12),
    (-6,  '00000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'Personal training',    current_date + 5,  '07:00', '08:00', 60, 'Studio A',    1, 1),
    (-7,  '00000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', 'Bootcamp',             current_date + 6,  '10:00', '11:00', 60, 'City park',   3, 20),
    (-8,  '00000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000002', 'Small group HIIT',     current_date + 8,  '18:00', '19:00', 60, 'Studio B',    2, 5),
    (-9,  '00000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000004', 'Mobility & stretching', current_date + 11, '09:00', '10:00', 60, 'Studio A',    3, 12),
    (-10, '00000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000003', 'Bootcamp',             current_date + 13, '10:00', '11:00', 60, 'City park',   3, 20);

INSERT INTO session_booking (id, session_id, participantId, subscriptionId, status)
VALUES
    -- personal training on day +1: full
    (-1,  -1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'DONE'),
    -- small group on day +1: full, one participant waiting
    (-2,  -2, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'DONE'),
    (-3,  -2, '20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'DONE'),
    (-4,  -2, '20000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 'DONE'),
    (-5,  -2, '20000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', 'DONE'),
    (-6,  -2, '20000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000005', 'DONE'),
    (-7,  -2, '20000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000006', 'WAITING'),
    -- bootcamp on day +2: minimum not reached yet
    (-8,  -3, '20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'PREBOOKED'),
    (-9,  -3, '20000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', 'PREBOOKED'),
    -- small group on day +3: confirmed, seats left
    (-10, -4, '20000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', 'DONE'),
    (-11, -4, '20000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000005', 'DONE'),
    (-12, -4, '20000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000006', 'DONE');

-- Who leads each session and where: personal training with Léa at the studio, group courses with Marco
UPDATE training_session SET instructorId = '60000000-0000-0000-0000-000000000001', venueId = '70000000-0000-0000-0000-000000000001'
WHERE id < 0 AND articleId = '10000000-0000-0000-0000-000000000001';
UPDATE training_session SET instructorId = '60000000-0000-0000-0000-000000000002', venueId = '70000000-0000-0000-0000-000000000001'
WHERE id < 0 AND articleId IN ('10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004');
UPDATE training_session SET instructorId = '60000000-0000-0000-0000-000000000002', venueId = '70000000-0000-0000-0000-000000000002'
WHERE id < 0 AND articleId = '10000000-0000-0000-0000-000000000003';
UPDATE training_session SET note = 'Focus on core strength this week.' WHERE id = -4;
UPDATE training_session SET note = 'New playlist and a partner challenge at the end.' WHERE id = -3;

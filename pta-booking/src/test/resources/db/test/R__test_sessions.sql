-- Test-only sessions for the booking flow tests, 30 days ahead: outside the periods the list tests look at.
-- Ids below -100 never collide with the dev seed (-1..-10) or with Hibernate sequences.
DELETE FROM session_booking WHERE session_id <= -100;
DELETE FROM training_session WHERE id <= -100;

INSERT INTO training_session (id, logicalId, articleId, title, day, startTime, stopTime, durationMinutes, location, quotaMin, quotaMax)
VALUES
    (-101, 'f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Private (book, wait, cancel)', current_date + 30, '07:00', '08:00', 60, 'Studio A', 1, 1),
    (-102, 'f0000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Group (no subscription)', current_date + 30, '18:00', '19:00', 60, 'Studio B', 2, 5),
    (-103, 'f0000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'Group (customer down)', current_date + 30, '19:00', '20:00', 60, 'Studio B', 2, 5),
    (-104, 'f0000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000003', 'Bootcamp (pre-booking)', current_date + 30, '10:00', '11:00', 60, 'City park', 3, 20),
    (-105, 'f0000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', 'Bootcamp (cancellation)', current_date + 30, '12:00', '13:00', 60, 'City park', 1, 10);

import { axisFor, DEFAULT_AXIS, hasStarted, hoursOf, layoutDay } from './schedule';
import { Session } from './session.model';

const at = (start: string, stop: string, id = `${start}-${stop}`): Session => ({
  id,
  title: id,
  day: '2026-11-26',
  start: `${start}:00`,
  stop: `${stop}:00`,
  location: 'Studio A',
  bookedSeats: 0,
  waitlisted: 0,
  confirmed: true,
});

describe('axis', () => {
  it('covers the usual opening hours', () => {
    expect(axisFor([at('09:00', '10:00')])).toEqual(DEFAULT_AXIS);
  });

  it('extends to sessions starting earlier or ending later', () => {
    expect(axisFor([at('06:15', '07:00'), at('21:00', '22:30')])).toEqual({ startHour: 6, endHour: 23 });
  });

  it('labels each hour', () => {
    expect(hoursOf({ startHour: 7, endHour: 10 })).toEqual([7, 8, 9]);
  });
});

describe('day layout', () => {
  const axis = { startHour: 7, endHour: 21 };

  it('places a session by its start and duration', () => {
    const [placed] = layoutDay([at('09:30', '10:45')], axis, 30);
    expect(placed).toMatchObject({ top: 150, height: 75, lane: 0, lanes: 1 });
  });

  it('gives short sessions a readable minimum height', () => {
    const [placed] = layoutDay([at('09:00', '09:20')], axis, 40);
    expect(placed.height).toBe(40);
  });

  it('keeps sessions that follow each other in one lane', () => {
    const placed = layoutDay([at('09:00', '10:00', 'a'), at('10:00', '11:00', 'b')], axis, 30);
    expect(placed.map(p => [p.session.id, p.lane, p.lanes])).toEqual([['a', 0, 1], ['b', 0, 1]]);
  });

  it('puts overlapping sessions side by side', () => {
    const placed = layoutDay([at('09:00', '10:30', 'a'), at('10:00', '11:00', 'b'), at('12:00', '13:00', 'c')], axis, 30);
    expect(placed.map(p => [p.session.id, p.lane, p.lanes])).toEqual([['a', 0, 2], ['b', 1, 2], ['c', 0, 1]]);
  });

  it('reuses a lane freed within a group of overlapping sessions', () => {
    const placed = layoutDay(
      [at('09:00', '12:00', 'long'), at('09:00', '10:00', 'first'), at('10:00', '11:00', 'second')], axis, 30);
    expect(placed.map(p => [p.session.id, p.lane, p.lanes])).toEqual([['long', 0, 2], ['first', 1, 2], ['second', 1, 2]]);
  });

  it('counts the minimum height when deciding what overlaps', () => {
    // 09:00-09:15 needs 45 minutes of room: it visually overlaps a session starting at 09:30
    const placed = layoutDay([at('09:00', '09:15', 'short'), at('09:30', '10:30', 'next')], axis, 45);
    expect(placed.map(p => p.lanes)).toEqual([2, 2]);
  });
});

describe('started sessions', () => {
  const session = at('18:00', '19:00');

  it('are those of past days or which started today', () => {
    expect(hasStarted(session, '2026-11-27', 0)).toBe(true);
    expect(hasStarted(session, '2026-11-26', 18 * 60)).toBe(true);
    expect(hasStarted(session, '2026-11-26', 17 * 60 + 59)).toBe(false);
    expect(hasStarted(session, '2026-11-25', 23 * 60)).toBe(false);
  });
});

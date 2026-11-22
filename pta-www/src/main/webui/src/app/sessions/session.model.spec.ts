import { availabilityLabel, availabilityOf, Session, shortTime } from './session.model';

const session = (overrides: Partial<Session>): Session => ({
  id: 'id',
  title: 'Small group HIIT',
  day: '2026-11-23',
  start: '18:00:00',
  stop: '19:00:00',
  location: 'Studio B',
  minSeats: 2,
  maxSeats: 5,
  bookedSeats: 0,
  availableSeats: 5,
  waitlisted: 0,
  confirmed: false,
  ...overrides,
});

describe('availability', () => {
  it('is full when no seat is left, mentioning the waiting list', () => {
    const full = availabilityOf(session({ bookedSeats: 5, availableSeats: 0, waitlisted: 2, confirmed: true }));
    expect(full).toEqual({ kind: 'full', waitlisted: 2 });
    expect(availabilityLabel(full)).toBe('Full · 2 on the waiting list');
    expect(availabilityLabel({ kind: 'full', waitlisted: 0 })).toBe('Full');
  });

  it('tells how many more participants are needed while the minimum is not reached', () => {
    const pending = availabilityOf(session({ minSeats: 3, maxSeats: 20, bookedSeats: 2, availableSeats: 18 }));
    expect(pending).toEqual({ kind: 'needs-more', missing: 1, available: 18 });
    expect(availabilityLabel(pending)).toBe('Needs 1 more to take place');
  });

  it('counts the seats left once confirmed', () => {
    expect(availabilityLabel(availabilityOf(session({ bookedSeats: 3, availableSeats: 2, confirmed: true }))))
      .toBe('2 seats left');
    expect(availabilityLabel(availabilityOf(session({ bookedSeats: 4, availableSeats: 1, confirmed: true }))))
      .toBe('1 seat left');
  });

  it('is open when seats are unlimited', () => {
    const open = availabilityOf(session({ minSeats: undefined, maxSeats: undefined, availableSeats: undefined, confirmed: true }));
    expect(availabilityLabel(open)).toBe('Open');
  });

  it('a private session not booked yet is not full but needs its participant', () => {
    const empty = availabilityOf(session({ minSeats: 1, maxSeats: 1, bookedSeats: 0, availableSeats: 1 }));
    expect(availabilityLabel(empty)).toBe('Needs 1 more to take place');
  });
});

describe('shortTime', () => {
  it('drops the seconds', () => expect(shortTime('07:30:00')).toBe('07:30'));
});

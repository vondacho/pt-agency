import { availabilityLabel, availabilityOf, bookingActionFor, participantBookingLabel, Session, shortTime } from './session.model';

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

describe('booking action', () => {
  const signedIn = { authenticated: true, name: 'Alice', linked: true };

  it('invites anonymous visitors to sign in', () => {
    expect(bookingActionFor(session({}), { authenticated: false, linked: false })).toEqual({ kind: 'sign-in' });
  });

  it('explains when the account is not linked to a participant', () => {
    expect(bookingActionFor(session({}), { authenticated: true, name: 'Carol', linked: false })).toEqual({ kind: 'not-linked' });
  });

  it('offers to book a session with seats left, or to join the waiting list of a full one', () => {
    expect(bookingActionFor(session({ availableSeats: 3 }), signedIn)).toEqual({ kind: 'book', label: 'Book' });
    expect(bookingActionFor(session({ availableSeats: 0 }), signedIn)).toEqual({ kind: 'book', label: 'Join waiting list' });
    expect(bookingActionFor(session({ availableSeats: undefined, maxSeats: undefined }), signedIn)).toEqual({ kind: 'book', label: 'Book' });
  });

  it('offers to cancel an own booking or to leave the waiting list', () => {
    expect(bookingActionFor(session({ participantBooking: 'DONE' }), signedIn)).toEqual({ kind: 'cancel', label: 'Cancel booking' });
    expect(bookingActionFor(session({ participantBooking: 'PREBOOKED' }), signedIn)).toEqual({ kind: 'cancel', label: 'Cancel booking' });
    expect(bookingActionFor(session({ participantBooking: 'WAITING', availableSeats: 0 }), signedIn)).toEqual({ kind: 'cancel', label: 'Leave waiting list' });
  });
});

describe('participant booking label', () => {
  it('tells the participant where they stand', () => {
    expect(participantBookingLabel(session({}))).toBeNull();
    expect(participantBookingLabel(session({ participantBooking: 'DONE' }))).toBe("You're booked");
    expect(participantBookingLabel(session({ participantBooking: 'WAITING' }))).toBe('On the waiting list');
    expect(participantBookingLabel(session({ participantBooking: 'PREBOOKED', minSeats: 3, bookedSeats: 1 })))
      .toBe('Pre-booked · confirmed once 2 more join');
    expect(participantBookingLabel(session({ participantBooking: 'PREBOOKED', minSeats: 3, bookedSeats: 2 })))
      .toBe('Pre-booked · confirmed once 1 more joins');
  });
});

/** A training session as served by `GET /api/sessions` (source of truth: pta-booking). */
export interface Session {
  id: string;
  title: string;
  /** ISO local date, e.g. `2026-11-23` */
  day: string;
  /** ISO local time, e.g. `18:00:00` */
  start: string;
  stop: string;
  location: string;
  /** Bookings required for the session to take place; absent when there is no minimum. */
  minSeats?: number;
  /** Seats offered; absent when unlimited. */
  maxSeats?: number;
  bookedSeats: number;
  /** Seats still free; absent when unlimited. */
  availableSeats?: number;
  waitlisted: number;
  confirmed: boolean;
  /** The signed-in participant's booking of this session, if any. */
  participantBooking?: ParticipantBooking;
  /** Name of the instructor leading the session, if any. */
  instructor?: string;
}

/** DONE: seat booked · PREBOOKED: seat held until the session's minimum is reached · WAITING: on the waiting list */
export type ParticipantBooking = 'DONE' | 'PREBOOKED' | 'WAITING';

/** Who is signed in, as told by `GET /api/me`. */
export interface Me {
  authenticated: boolean;
  name?: string;
  /** The account is linked to a participant, i.e. may book. */
  linked: boolean;
}

export type BookingAction =
  | { kind: 'sign-in' }
  | { kind: 'not-linked' }
  | { kind: 'book'; label: 'Book' | 'Join waiting list' }
  | { kind: 'cancel'; label: 'Cancel booking' | 'Leave waiting list' };

/** What the participant can do with a session, given who they are and their booking. */
export function bookingActionFor(session: Session, me: Me): BookingAction {
  if (!me.authenticated) return { kind: 'sign-in' };
  if (!me.linked) return { kind: 'not-linked' };
  switch (session.participantBooking) {
    case 'DONE':
    case 'PREBOOKED':
      return { kind: 'cancel', label: 'Cancel booking' };
    case 'WAITING':
      return { kind: 'cancel', label: 'Leave waiting list' };
    default:
      return { kind: 'book', label: session.availableSeats === 0 ? 'Join waiting list' : 'Book' };
  }
}

/** How the participant's own booking reads, e.g. "You're booked". */
export function participantBookingLabel(session: Session): string | null {
  switch (session.participantBooking) {
    case 'DONE':
      return "You're booked";
    case 'PREBOOKED': {
      const missing = Math.max(1, (session.minSeats ?? 0) - session.bookedSeats);
      return `Pre-booked · confirmed once ${missing} more ${missing === 1 ? 'joins' : 'join'}`;
    }
    case 'WAITING':
      return 'On the waiting list';
    default:
      return null;
  }
}

export type Availability =
  | { kind: 'full'; waitlisted: number }
  | { kind: 'needs-more'; missing: number; available?: number }
  | { kind: 'open'; available?: number };

/** What a participant needs to know at a glance: can I join, and will it take place? */
export function availabilityOf(session: Session): Availability {
  if (session.availableSeats === 0) {
    return { kind: 'full', waitlisted: session.waitlisted };
  }
  if (!session.confirmed && session.minSeats != null) {
    return {
      kind: 'needs-more',
      missing: Math.max(1, session.minSeats - session.bookedSeats),
      available: session.availableSeats,
    };
  }
  return { kind: 'open', available: session.availableSeats };
}

export function availabilityLabel(availability: Availability): string {
  switch (availability.kind) {
    case 'full':
      return availability.waitlisted > 0
        ? `Full · ${availability.waitlisted} on the waiting list`
        : 'Full';
    case 'needs-more':
      return `Needs ${availability.missing} more to take place`;
    case 'open':
      return availability.available == null
        ? 'Open'
        : `${availability.available} ${availability.available === 1 ? 'seat' : 'seats'} left`;
  }
}

/** `18:00:00` → `18:00` */
export function shortTime(isoTime: string): string {
  return isoTime.slice(0, 5);
}

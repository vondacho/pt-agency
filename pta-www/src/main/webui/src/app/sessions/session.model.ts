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

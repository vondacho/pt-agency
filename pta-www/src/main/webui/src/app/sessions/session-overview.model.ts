import { httpResource } from '@angular/common/http';
import { Injectable, Signal } from '@angular/core';

import { Session } from './session.model';

/** Everything to know about a session before booking it, as served by `GET /api/sessions/{id}/overview`. */
export interface SessionOverview {
  session: Session;
  /** What the course is about; absent when the catalogue is unavailable. */
  course?: { objectives: string[]; level?: string; prerequisites?: string; whatToBring?: string };
  instructor?: { id: string; name: string; bio?: string };
  venue?: { id: string; name: string; address?: string; accessNotes?: string; mapUrl?: string };
  /** The session's own note, e.g. "Focus on core strength this week." */
  note?: string;
  /** A booked seat can be cancelled until this many hours before the start; absent: until the start. */
  cancellationHours?: number;
  /** ISO local date-time until when a booked seat can be cancelled. */
  cancellableUntil?: string;
  cancellationOpen: boolean;
  /** Subscriptions and passes giving access: those on sale, and those the participant holds. */
  access: { id: string; name: string; onSale: boolean; held: boolean }[];
  /** Whether the signed-in participant holds one valid on the session's day; absent when not signed in. */
  youHoldOne?: boolean;
  /** A part could not be fetched (catalogue or subscriptions). */
  incomplete: boolean;
}

@Injectable({ providedIn: 'root' })
export class SessionOverviewService {
  /** The overview of the session, reloaded whenever the id changes; idle without an id. */
  overviewOf(id: Signal<string | undefined>) {
    return httpResource<SessionOverview>(() => {
      const session = id();
      return session ? `/api/sessions/${encodeURIComponent(session)}/overview` : undefined;
    });
  }
}

/** "Free cancellation until Tue 24 Nov, 06:00" */
export function cancellationLabel(overview: SessionOverview): string {
  if (!overview.cancellableUntil) return 'Free cancellation until the session starts.';
  const until = new Date(overview.cancellableUntil);
  const date = until.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' });
  const time = until.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  return overview.cancellationOpen
    ? `Free cancellation until ${date}, ${time} (${overview.cancellationHours} h before the start).`
    : `Cancellations closed on ${date}, ${time} (${overview.cancellationHours} h before the start).`;
}

/** How seats, the minimum, pre-booking and the waiting list work for this session. */
export function seatsRules(session: Session): string[] {
  const rules: string[] = [];
  if (session.maxSeats != null) {
    rules.push(`${session.maxSeats} ${session.maxSeats === 1 ? 'seat' : 'seats'}, ${session.bookedSeats} taken.`);
  } else {
    rules.push(`No seat limit, ${session.bookedSeats} booked.`);
  }
  if (session.minSeats != null && session.minSeats > 1) {
    rules.push(session.confirmed
      ? `It takes place: the minimum of ${session.minSeats} participants is reached.`
      : `It takes place with at least ${session.minSeats} participants. Until then your seat is pre-booked, and confirmed as soon as the minimum is reached.`);
  }
  if (session.maxSeats != null) {
    rules.push('When it is full, you can join the waiting list: a seat freed by a cancellation goes to the first person waiting.');
  }
  return rules;
}

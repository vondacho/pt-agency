import { Component, computed, inject, input, output, signal } from '@angular/core';

import { BookingActions, problemDetail } from './booking-actions.service';
import {
  availabilityLabel,
  availabilityOf,
  bookingActionFor,
  Me,
  participantBookingLabel,
  Session,
  shortTime,
} from './session.model';

@Component({
  selector: 'pta-session-card',
  templateUrl: './session-card.html',
  styleUrl: './session-card.scss',
  host: { role: 'listitem' },
})
export class SessionCard {
  readonly session = input.required<Session>();
  readonly me = input.required<Me>();
  /** Emitted once a booking or cancellation succeeded: the session should be reloaded. */
  readonly changed = output<void>();

  private readonly actions = inject(BookingActions);

  protected readonly start = computed(() => shortTime(this.session().start));
  protected readonly stop = computed(() => shortTime(this.session().stop));
  protected readonly availability = computed(() => availabilityOf(this.session()));
  protected readonly label = computed(() => availabilityLabel(this.availability()));
  protected readonly action = computed(() => bookingActionFor(this.session(), this.me()));
  protected readonly yourBooking = computed(() => participantBookingLabel(this.session()));

  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Fill ratio of the seat meter, in percent; null when seats are unlimited. */
  protected readonly fill = computed(() => {
    const { bookedSeats, maxSeats } = this.session();
    return maxSeats ? Math.min(100, (bookedSeats / maxSeats) * 100) : null;
  });

  /** Position of the minimum marker on the seat meter, in percent. */
  protected readonly minMarker = computed(() => {
    const { minSeats, maxSeats } = this.session();
    return minSeats && maxSeats && minSeats < maxSeats ? (minSeats / maxSeats) * 100 : null;
  });

  protected readonly seatsSummary = computed(() => {
    const { bookedSeats, maxSeats, minSeats } = this.session();
    const booked = maxSeats != null ? `${bookedSeats} of ${maxSeats} booked` : `${bookedSeats} booked`;
    return minSeats && minSeats > 1 ? `${booked} · min. ${minSeats}` : booked;
  });

  protected async run(kind: 'book' | 'cancel') {
    this.pending.set(true);
    this.error.set(null);
    try {
      if (kind === 'book') {
        await this.actions.book(this.session().id);
      } else {
        await this.actions.cancel(this.session().id);
      }
      this.changed.emit();
    } catch (e) {
      this.error.set(problemDetail(e));
    } finally {
      this.pending.set(false);
    }
  }
}

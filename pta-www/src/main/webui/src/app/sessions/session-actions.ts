import { Component, computed, inject, input, output, signal } from '@angular/core';

import { BookingActions, problemDetail } from './booking-actions.service';
import { bookingActionFor, Me, participantBookingLabel, Session } from './session.model';

/**
 * What the participant can do with a session: their own booking status, and Book / Join waiting list /
 * Cancel booking / Leave waiting list, or an invitation to sign in.
 */
@Component({
  selector: 'pta-session-actions',
  templateUrl: './session-actions.html',
  styleUrl: './session-actions.scss',
})
export class SessionActions {
  readonly session = input.required<Session>();
  readonly me = input.required<Me>();
  /** A session that has started can no longer be booked or cancelled. */
  readonly started = input(false);
  /** False once the course's cancellation deadline has passed: a booked seat can no longer be cancelled. */
  readonly cancellationOpen = input(true);
  /** Emitted once a booking or cancellation succeeded: the sessions should be reloaded. */
  readonly changed = output<void>();

  private readonly actions = inject(BookingActions);

  protected readonly action = computed(() => bookingActionFor(this.session(), this.me()));
  /** A booked seat past the deadline; pre-bookings and the waiting list can still be left. */
  protected readonly cancellationClosed = computed(() =>
    !this.cancellationOpen() && this.session().participantBooking === 'DONE');
  protected readonly yourBooking = computed(() => participantBookingLabel(this.session()));
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);

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

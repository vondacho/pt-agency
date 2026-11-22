import { Component, computed, input } from '@angular/core';

import { availabilityLabel, availabilityOf, Session, shortTime } from './session.model';

@Component({
  selector: 'pta-session-card',
  templateUrl: './session-card.html',
  styleUrl: './session-card.scss',
  host: { role: 'listitem' },
})
export class SessionCard {
  readonly session = input.required<Session>();

  protected readonly start = computed(() => shortTime(this.session().start));
  protected readonly stop = computed(() => shortTime(this.session().stop));
  protected readonly availability = computed(() => availabilityOf(this.session()));
  protected readonly label = computed(() => availabilityLabel(this.availability()));

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
}

import { Component, computed, input, output } from '@angular/core';

import { longDay, shortDay } from './calendar';
import { Axis, hasStarted, hoursOf, layoutDay, Placed } from './schedule';
import { availabilityLabel, availabilityOf, participantBookingLabel, Session, shortTime } from './session.model';

/** Height of one hour on the week grid, in pixels. */
const HOUR_HEIGHT = 56;
/** Minimum visual duration of a block, in minutes, so its time and title stay readable. */
const MIN_BLOCK_MINUTES = 45;

interface DayColumn {
  day: string;
  weekday: string;
  date: string;
  isToday: boolean;
  isPast: boolean;
  blocks: Placed[];
}

/**
 * The sessions of a week on a time grid: one column per day, sessions placed by their start and end time.
 * Selecting a session opens its day.
 */
@Component({
  selector: 'pta-week-view',
  templateUrl: './week-view.html',
  styleUrl: './week-view.scss',
})
export class WeekView {
  readonly days = input.required<string[]>();
  readonly sessions = input.required<Session[]>();
  readonly axis = input.required<Axis>();
  readonly today = input.required<string>();
  /** Minutes since midnight, for the "now" line and started sessions. */
  readonly now = input.required<number>();
  readonly open = output<{ day: string; session: string }>();

  protected readonly hourHeight = HOUR_HEIGHT;
  protected readonly hours = computed(() => hoursOf(this.axis()));
  protected readonly gridHeight = computed(() => this.hours().length * HOUR_HEIGHT);

  protected readonly columns = computed<DayColumn[]>(() =>
    this.days().map(day => {
      const { weekday, day: date } = shortDay(day);
      return {
      day,
      weekday,
      date,
      isToday: day === this.today(),
      isPast: day < this.today(),
      blocks: layoutDay(this.sessions().filter(s => s.day === day), this.axis(), MIN_BLOCK_MINUTES),
      };
    }));

  /** Position of the "now" line on today's column, in pixels, when within the axis. */
  protected readonly nowTop = computed(() => {
    const minutes = this.now() - this.axis().startHour * 60;
    return minutes >= 0 && minutes <= this.hours().length * 60 ? (minutes * HOUR_HEIGHT) / 60 : null;
  });

  protected px(minutes: number): number {
    return (minutes * HOUR_HEIGHT) / 60;
  }

  protected time(session: Session): string {
    return shortTime(session.start);
  }

  protected kind(session: Session): string {
    return availabilityOf(session).kind;
  }

  protected started(session: Session): boolean {
    return hasStarted(session, this.today(), this.now());
  }

  protected seats(session: Session): string {
    return session.maxSeats != null ? `${session.bookedSeats}/${session.maxSeats}` : `${session.bookedSeats} booked`;
  }

  /** Everything the block conveys visually, for screen readers. */
  protected describe(session: Session): string {
    return [
      session.title,
      `${longDay(session.day)}, ${shortTime(session.start)} to ${shortTime(session.stop)}`,
      session.location,
      availabilityLabel(availabilityOf(session)),
      participantBookingLabel(session),
    ].filter(Boolean).join(', ');
  }

}

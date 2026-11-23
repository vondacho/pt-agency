import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { longDay, shortDay } from './calendar';
import { hasStarted } from './schedule';
import { availabilityLabel, availabilityOf, participantBookingLabel, Session, shortTime } from './session.model';

/** Sessions listed in a day cell before "+N more". */
const VISIBLE_PER_DAY = 3;

interface DayCell {
  day: string;
  date: string;
  inMonth: boolean;
  isToday: boolean;
  isPast: boolean;
  sessions: Session[];
  more: number;
  count: number;
  mine: boolean;
}

/**
 * The sessions of a month on a calendar grid, weeks from Monday to Sunday.
 * Selecting a day or a session opens the day view.
 */
@Component({
  selector: 'pta-month-view',
  imports: [RouterLink],
  templateUrl: './month-view.html',
  styleUrl: './month-view.scss',
})
export class MonthView {
  /** Weeks of the grid, from Monday to Sunday. */
  readonly weeks = input.required<string[][]>();
  /** Any day of the displayed month: days of other months are shown dimmed. */
  readonly month = input.required<string>();
  readonly sessions = input.required<Session[]>();
  readonly today = input.required<string>();
  readonly now = input.required<number>();
  readonly open = output<{ day: string; session: string }>();

  protected readonly weekdays = computed(() => this.weeks()[0].map(day => shortDay(day).weekday));

  protected readonly rows = computed<DayCell[][]>(() => {
    const byDay = new Map<string, Session[]>();
    for (const session of [...this.sessions()].sort((a, b) => a.start.localeCompare(b.start))) {
      byDay.set(session.day, [...(byDay.get(session.day) ?? []), session]);
    }
    const month = this.month().slice(0, 7);
    return this.weeks().map(week => week.map(day => {
      const sessions = byDay.get(day) ?? [];
      return {
        day,
        date: shortDay(day).day,
        inMonth: day.startsWith(month),
        isToday: day === this.today(),
        isPast: day < this.today(),
        sessions: sessions.slice(0, VISIBLE_PER_DAY),
        more: Math.max(0, sessions.length - VISIBLE_PER_DAY),
        count: sessions.length,
        mine: sessions.some(s => !!s.participantBooking),
      };
    }));
  });

  protected dayParams(day: string) {
    return { view: 'day', date: day };
  }

  protected kind(session: Session): string {
    return availabilityOf(session).kind;
  }

  protected started(session: Session): boolean {
    return hasStarted(session, this.today(), this.now());
  }

  protected time(session: Session): string {
    return shortTime(session.start);
  }

  protected describe(session: Session): string {
    return [
      session.title,
      `${longDay(session.day)}, ${shortTime(session.start)} to ${shortTime(session.stop)}`,
      session.location,
      availabilityLabel(availabilityOf(session)),
      participantBookingLabel(session),
    ].filter(Boolean).join(', ');
  }

  protected dayLabel(cell: DayCell): string {
    const sessions = cell.count === 0 ? 'no session' : `${cell.count} ${cell.count === 1 ? 'session' : 'sessions'}`;
    return `${longDay(cell.day)}: ${sessions}${cell.mine ? ', with your booking' : ''}`;
  }
}

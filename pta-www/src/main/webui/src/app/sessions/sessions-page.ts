import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { addDays, dayHeading, isIsoDate, longDay, nowMinutes, periodLabel, shortDay, startOfWeek, today, weekDays } from './calendar';
import { DayView } from './day-view';
import { axisFor } from './schedule';
import { SessionService } from './session.service';
import { WeekView } from './week-view';

export type View = 'week' | 'day';

/** Below this width the week grid is not readable: the day view (with its week strip) is shown instead. */
const NARROW = '(max-width: 719px)';

/**
 * Session planning: a week on a time grid, or one day with the sessions' details and actions.
 * State lives in the URL (`?view=week|day&date=YYYY-MM-DD&focus=<session>`), so views can be bookmarked and shared.
 */
@Component({
  selector: 'pta-sessions-page',
  imports: [RouterLink, WeekView, DayView],
  templateUrl: './sessions-page.html',
  styleUrl: './sessions-page.scss',
})
export class SessionsPage {
  /** `week` (default) or `day`. */
  readonly view = input<string>();
  /** Any day of the week to show, or the day to show. */
  readonly date = input<string>();
  /** Session to highlight in the day view. */
  readonly focus = input<string>();
  /** Former parameter of the first day of the week, still honoured for existing links. */
  readonly from = input<string>();

  private readonly router = inject(Router);

  protected readonly today = today();
  protected readonly now = signal(nowMinutes());
  protected readonly narrow = signal(matches(NARROW));
  /** Earliest day participants can look at: the Monday of the current week. */
  private readonly firstDay = startOfWeek(this.today);

  /** The selected day, never before the current week. */
  protected readonly selected = computed(() => {
    const requested = [this.date(), this.from()].find(isIsoDate) ?? this.today;
    return requested < this.firstDay ? this.firstDay : requested;
  });
  protected readonly effectiveView = computed<View>(() => (this.narrow() || this.view() === 'day' ? 'day' : 'week'));
  protected readonly days = computed(() => weekDays(this.selected()));
  protected readonly period = computed(() => ({ from: this.days()[0], to: this.days()[6] }));

  protected readonly sessions = inject(SessionService).sessionsIn(this.period);
  protected readonly me = inject(AuthService).me;

  /** One time axis for the whole week, so it stays put while moving between days. */
  protected readonly axis = computed(() => axisFor(this.sessions.value()));
  protected readonly sessionCount = computed(() => this.sessions.value().length);

  protected readonly title = computed(() => {
    if (this.effectiveView() === 'week') return periodLabel(this.period().from, this.period().to);
    const relative = dayHeading(this.selected(), this.today).relative;
    return relative === 'Today' || relative === 'Tomorrow' ? `${relative}, ${longDay(this.selected())}` : longDay(this.selected());
  });

  protected readonly strip = computed(() =>
    this.days().map(day => {
      const daySessions = this.sessions.value().filter(s => s.day === day);
      const { weekday, day: date } = shortDay(day);
      return {
        day,
        weekday,
        date,
        count: daySessions.length,
        mine: daySessions.some(s => !!s.participantBooking),
        isToday: day === this.today,
        isPast: day < this.today,
        isSelected: day === this.selected(),
      };
    }));

  /** Previous week or day; nothing before the current week. */
  protected readonly previous = computed(() => {
    const week = this.effectiveView() === 'week';
    const candidate = addDays(this.selected(), week ? -7 : -1);
    const lastDayShown = week ? weekDays(candidate)[6] : candidate;
    return lastDayShown < this.firstDay ? null : candidate;
  });
  protected readonly next = computed(() => addDays(this.selected(), this.effectiveView() === 'week' ? 7 : 1));
  protected readonly isCurrent = computed(() =>
    this.effectiveView() === 'week' ? this.days().includes(this.today) : this.selected() === this.today);

  constructor() {
    const destroyRef = inject(DestroyRef);
    // keep the "now" line and the started sessions up to date
    const timer = setInterval(() => this.now.set(nowMinutes()), 60_000);
    destroyRef.onDestroy(() => clearInterval(timer));
    // follow the viewport across the narrow breakpoint
    const query = typeof window !== 'undefined' && typeof window.matchMedia === 'function' ? window.matchMedia(NARROW) : null;
    if (query) {
      const listener = (e: MediaQueryListEvent) => this.narrow.set(e.matches);
      query.addEventListener('change', listener);
      destroyRef.onDestroy(() => query.removeEventListener('change', listener));
    }
  }

  protected params(view: View, date: string, focus?: string) {
    return focus ? { view, date, focus } : { view, date };
  }

  protected openDay(event: { day: string; session: string }) {
    void this.router.navigate([], { queryParams: this.params('day', event.day, event.session) });
  }
}

function matches(query: string): boolean {
  return typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia(query).matches;
}

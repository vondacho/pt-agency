import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { addDays, addMonths, dayHeading, isIsoDate, longDay, monthLabel, monthWeeks, nowMinutes, periodLabel, shortDay, startOfWeek, today, weekDays } from './calendar';
import { DayView } from './day-view';
import { MonthView } from './month-view';
import { axisFor } from './schedule';
import { SessionOverviewDialog } from './session-overview';
import { SessionService } from './session.service';
import { WeekView } from './week-view';

export type View = 'month' | 'week' | 'day';

/** Below this width the week grid is not readable: the day view (with its week strip) is shown instead; the month stays. */
const NARROW = '(max-width: 719px)';

/**
 * Session planning: a month calendar, a week on a time grid, or one day with the sessions' details and actions.
 * Selecting a session opens its overview in a dialog.
 * State lives in the URL (`?view=month|week|day&date=YYYY-MM-DD&focus=<session>&session=<id>`), so views and
 * overviews can be bookmarked and shared.
 */
@Component({
  selector: 'pta-sessions-page',
  imports: [RouterLink, MonthView, WeekView, DayView, SessionOverviewDialog],
  templateUrl: './sessions-page.html',
  styleUrl: './sessions-page.scss',
})
export class SessionsPage {
  /** `week` (default), `month` or `day`. */
  readonly view = input<string>();
  /** Any day of the month or week to show, or the day to show. */
  readonly date = input<string>();
  /** Session to highlight in the day view. */
  readonly focus = input<string>();
  /** Session whose overview is open. */
  readonly session = input<string>();
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
  protected readonly effectiveView = computed<View>(() => {
    const view = this.view();
    if (view === 'month' || view === 'day') return view;
    return this.narrow() ? 'day' : 'week';
  });
  /** Views offered by the toggle: the week grid is not readable on narrow screens. */
  protected readonly views = computed<View[]>(() => (this.narrow() ? ['month', 'day'] : ['month', 'week', 'day']));
  protected readonly days = computed(() => weekDays(this.selected()));
  protected readonly weeks = computed(() => monthWeeks(this.selected()));
  protected readonly period = computed(() => {
    if (this.effectiveView() === 'month') {
      const weeks = this.weeks();
      return { from: weeks[0][0], to: weeks[weeks.length - 1][6] };
    }
    return { from: this.days()[0], to: this.days()[6] };
  });

  protected readonly sessions = inject(SessionService).sessionsIn(this.period);
  protected readonly me = inject(AuthService).me;

  /** One time axis for the whole week, so it stays put while moving between days. */
  protected readonly axis = computed(() => axisFor(this.sessions.value()));
  protected readonly sessionCount = computed(() => this.sessions.value().length);

  protected readonly title = computed(() => {
    if (this.effectiveView() === 'month') return monthLabel(this.selected());
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

  /** Previous month, week or day; nothing entirely before the current week. */
  protected readonly previous = computed(() => {
    const candidate = this.step(-1);
    const lastDayShown = {
      month: () => monthWeeks(candidate).at(-1)![6],
      week: () => weekDays(candidate)[6],
      day: () => candidate,
    }[this.effectiveView()]();
    return lastDayShown < this.firstDay ? null : candidate;
  });
  protected readonly next = computed(() => this.step(1));
  protected readonly isCurrent = computed(() => {
    switch (this.effectiveView()) {
      case 'month': return this.selected().slice(0, 7) === this.today.slice(0, 7);
      case 'week': return this.days().includes(this.today);
      case 'day': return this.selected() === this.today;
    }
  });
  protected readonly currentLabel = computed(() => ({ month: 'This month', week: 'This week', day: 'Today' })[this.effectiveView()]);
  protected readonly viewLabels: Record<View, string> = { month: 'Month', week: 'Week', day: 'Day' };

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

  private step(direction: 1 | -1): string {
    switch (this.effectiveView()) {
      case 'month': return addMonths(this.selected(), direction);
      case 'week': return addDays(this.selected(), 7 * direction);
      case 'day': return addDays(this.selected(), direction);
    }
  }

  protected params(view: View, date: string, focus?: string) {
    return focus ? { view, date, focus } : { view, date };
  }

  /** Opens the session's overview, staying on the current view. */
  protected openSession(event: { day: string; session: string }) {
    void this.router.navigate([], { queryParams: { session: event.session }, queryParamsHandling: 'merge' });
  }

  protected closeSession() {
    void this.router.navigate([], { queryParams: { session: null }, queryParamsHandling: 'merge' });
  }

  /** Leaves the overview for the session's day, the session highlighted. */
  protected openDay(event: { day: string; session: string }) {
    void this.router.navigate([], { queryParams: this.params('day', event.day, event.session) });
  }
}

function matches(query: string): boolean {
  return typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia(query).matches;
}

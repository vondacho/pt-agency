import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { addDays, dayHeading, daysBetween, isIsoDate, periodLabel, today, WEEK_LENGTH } from './calendar';
import { Session } from './session.model';
import { SessionCard } from './session-card';
import { SessionService } from './session.service';

interface DayGroup {
  day: string;
  relative: string;
  date: string;
  sessions: Session[];
}

@Component({
  selector: 'pta-sessions-page',
  imports: [RouterLink, SessionCard],
  templateUrl: './sessions-page.html',
  styleUrl: './sessions-page.scss',
})
export class SessionsPage {
  /** First day of the displayed week, bound from the `?from=` query parameter. */
  readonly from = input<string>();

  private readonly today = today();

  /** The week starts on the requested day, but never in the past: participants look at the coming days. */
  protected readonly period = computed(() => {
    const requested = this.from();
    const start = isIsoDate(requested) && requested > this.today ? requested : this.today;
    return { from: start, to: addDays(start, WEEK_LENGTH - 1) };
  });

  protected readonly sessions = inject(SessionService).sessionsIn(this.period);
  protected readonly me = inject(AuthService).me;

  protected readonly label = computed(() => periodLabel(this.period().from, this.period().to));
  protected readonly isCurrentWeek = computed(() => this.period().from === this.today);
  protected readonly previousFrom = computed(() => {
    const previous = addDays(this.period().from, -WEEK_LENGTH);
    return daysBetween(this.today, previous) > 0 ? previous : null;
  });
  protected readonly nextFrom = computed(() => addDays(this.period().from, WEEK_LENGTH));

  protected readonly days = computed<DayGroup[]>(() => {
    const groups = new Map<string, Session[]>();
    for (const session of this.sessions.value()) {
      groups.set(session.day, [...(groups.get(session.day) ?? []), session]);
    }
    return [...groups].map(([day, sessions]) => ({ day, ...dayHeading(day, this.today), sessions }));
  });

  protected readonly sessionCount = computed(() => this.sessions.value().length);
}

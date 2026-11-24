import { afterRenderEffect, Component, computed, ElementRef, inject, input, output } from '@angular/core';

import { Axis, hasStarted, hoursOf, layoutDay } from './schedule';
import { SessionActions } from './session-actions';
import { availabilityLabel, availabilityOf, Me, Session, shortTime } from './session.model';

/** Height of one hour on the day grid, in pixels. */
const HOUR_HEIGHT = 120;
/** Minimum visual duration of a block, in minutes, so its details and actions fit. */
const MIN_BLOCK_MINUTES = 60;

/**
 * The sessions of one day on a time axis, with their details and the participant's actions.
 */
@Component({
  selector: 'pta-day-view',
  imports: [SessionActions],
  templateUrl: './day-view.html',
  styleUrl: './day-view.scss',
})
export class DayView {
  readonly day = input.required<string>();
  /** Sessions of the day (others are ignored). */
  readonly sessions = input.required<Session[]>();
  readonly axis = input.required<Axis>();
  readonly today = input.required<string>();
  readonly now = input.required<number>();
  readonly me = input.required<Me>();
  /** Session to bring into view and highlight, e.g. the one selected on the week. */
  readonly focus = input<string>();
  /** Emitted after a booking or cancellation: the sessions should be reloaded. */
  readonly changed = output<void>();
  /** The participant wants the session's overview. */
  readonly open = output<{ day: string; session: string }>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly hourHeight = HOUR_HEIGHT;
  protected readonly hours = computed(() => hoursOf(this.axis()));
  protected readonly gridHeight = computed(() => this.hours().length * HOUR_HEIGHT);
  protected readonly daySessions = computed(() => this.sessions().filter(s => s.day === this.day()));
  protected readonly blocks = computed(() => layoutDay(this.daySessions(), this.axis(), MIN_BLOCK_MINUTES));
  protected readonly isToday = computed(() => this.day() === this.today());

  protected readonly nowTop = computed(() => {
    const minutes = this.now() - this.axis().startHour * 60;
    return this.isToday() && minutes >= 0 && minutes <= this.hours().length * 60 ? (minutes * HOUR_HEIGHT) / 60 : null;
  });

  constructor() {
    // bring the focused session into view once rendered
    afterRenderEffect(() => {
      const id = this.focus();
      if (!id || !this.blocks().length) return;
      const block = this.host.nativeElement.querySelector<HTMLElement>(`[data-session="${CSS.escape(id)}"]`);
      block?.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
    });
  }

  protected px(minutes: number): number {
    return (minutes * HOUR_HEIGHT) / 60;
  }

  protected kind(session: Session): string {
    return availabilityOf(session).kind;
  }

  protected label(session: Session): string {
    return availabilityLabel(availabilityOf(session));
  }

  protected started(session: Session): boolean {
    return hasStarted(session, this.today(), this.now());
  }

  protected times(session: Session): string {
    return `${shortTime(session.start)}–${shortTime(session.stop)}`;
  }

  protected seats(session: Session): string {
    const { bookedSeats, maxSeats, minSeats } = session;
    const booked = maxSeats != null ? `${bookedSeats} of ${maxSeats} booked` : `${bookedSeats} booked`;
    return minSeats && minSeats > 1 ? `${booked} · min. ${minSeats}` : booked;
  }
}

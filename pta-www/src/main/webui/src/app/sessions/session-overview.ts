import { afterNextRender, Component, computed, DestroyRef, ElementRef, inject, input, output, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

import { hasStarted } from './schedule';
import { SessionActions } from './session-actions';
import { availabilityLabel, availabilityOf, Me, shortTime } from './session.model';
import { cancellationLabel, seatsRules, SessionOverviewService } from './session-overview.model';
import { longDay } from './calendar';

/**
 * A session's overview in a modal dialog: what the course works on, the instructor, the venue and the conditions to
 * take part, with the booking actions. Esc, the close button or a click outside closes it; focus then returns to
 * the element that opened it.
 */
@Component({
  selector: 'pta-session-overview',
  imports: [RouterLink, SessionActions],
  templateUrl: './session-overview.html',
  styleUrl: './session-overview.scss',
})
export class SessionOverviewDialog {
  readonly sessionId = input.required<string>();
  readonly me = input.required<Me>();
  readonly today = input.required<string>();
  readonly now = input.required<number>();
  /** The dialog asks to be closed. */
  readonly closed = output<void>();
  /** The participant wants the session's day. */
  readonly showDay = output<{ day: string; session: string }>();
  /** A booking or a cancellation succeeded: the planning should be reloaded. */
  readonly changed = output<void>();

  protected readonly overview = inject(SessionOverviewService).overviewOf(this.sessionId);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly opener = typeof document !== 'undefined' ? (document.activeElement as HTMLElement | null) : null;

  protected readonly value = computed(() => (this.overview.hasValue() ? this.overview.value() : undefined));
  protected readonly session = computed(() => this.value()?.session);
  protected readonly when = computed(() => {
    const s = this.session();
    return s ? `${longDay(s.day)} · ${shortTime(s.start)}–${shortTime(s.stop)}` : '';
  });
  protected readonly availability = computed(() => {
    const s = this.session();
    return s ? { kind: availabilityOf(s).kind, label: availabilityLabel(availabilityOf(s)) } : null;
  });
  protected readonly started = computed(() => {
    const s = this.session();
    return !!s && hasStarted(s, this.today(), this.now());
  });
  protected readonly seats = computed(() => {
    const s = this.session();
    return s ? seatsRules(s) : [];
  });
  protected readonly cancellation = computed(() => {
    const v = this.value();
    return v ? cancellationLabel(v) : '';
  });
  protected readonly onSale = computed(() => this.value()?.access.some(a => a.onSale) ?? false);
  protected readonly heldNames = computed(() => (this.value()?.access ?? []).filter(a => a.held).map(a => a.name).join(', '));
  protected readonly accessNames = computed(() => (this.value()?.access ?? []).map(a => a.name).join(', '));

  constructor() {
    afterNextRender(() => {
      const dialog = this.dialog().nativeElement;
      if (dialog.open) return;
      // jsdom has no showModal
      if (typeof dialog.showModal === 'function') dialog.showModal();
      else dialog.setAttribute('open', '');
    });
    inject(DestroyRef).onDestroy(() => {
      if (this.opener?.isConnected) this.opener.focus();
    });
  }

  /** Esc: let the page remove the dialog (and the URL parameter) rather than the browser closing it. */
  protected onCancel(event: Event) {
    event.preventDefault();
    this.closed.emit();
  }

  /** A click on the backdrop (the dialog itself, outside its content) closes it. */
  protected onClick(event: MouseEvent) {
    if (event.target === this.dialog().nativeElement) this.closed.emit();
  }

  protected onChanged() {
    this.overview.reload();
    this.changed.emit();
  }
}

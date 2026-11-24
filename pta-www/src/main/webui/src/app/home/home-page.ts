import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { MySubscriptions, shortDate } from '../me/my-space.model';
import { MyBookings, MyProfile, MySubscriptionsService } from '../me/my-space.service';
import { problemDetail } from '../sessions/booking-actions.service';
import { longDay, nowMinutes, today } from '../sessions/calendar';
import { hasStarted } from '../sessions/schedule';
import { SessionActions } from '../sessions/session-actions';
import { Session, shortTime } from '../sessions/session.model';

/** How many coming bookings the home space lists. */
const NEXT_BOOKINGS = 5;

/**
 * The participant's home space: next bookings, subscriptions and profile. Visitors get a welcome.
 */
@Component({
  selector: 'pta-home-page',
  imports: [RouterLink, SessionActions],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  private readonly auth = inject(AuthService);
  private readonly mySubscriptions = inject(MySubscriptionsService);
  protected readonly space = {
    bookings: inject(MyBookings).bookings,
    subscriptions: this.mySubscriptions.subscriptions,
    profile: inject(MyProfile).profile,
  };

  protected readonly me = this.auth.me;
  protected readonly loaded = this.auth.loaded;

  protected readonly nextBookings = computed(() =>
    (this.space.bookings.hasValue() ? this.space.bookings.value() : []).slice(0, NEXT_BOOKINGS));
  protected readonly moreBookings = computed(() =>
    Math.max(0, (this.space.bookings.hasValue() ? this.space.bookings.value().length : 0) - NEXT_BOOKINGS));
  protected readonly mine = computed<MySubscriptions>(() =>
    this.space.subscriptions.hasValue() ? this.space.subscriptions.value() : { subscriptions: [], requests: [] });
  protected readonly profile = computed(() => (this.space.profile.hasValue() ? this.space.profile.value() : undefined));

  protected readonly cancelling = signal<string | null>(null);
  protected readonly requestError = signal<string | null>(null);

  private readonly today = today();

  protected when(session: Session): string {
    return `${longDay(session.day)}, ${shortTime(session.start)}–${shortTime(session.stop)}`;
  }

  protected started(session: Session): boolean {
    return hasStarted(session, this.today, nowMinutes());
  }

  protected shortDate = shortDate;

  protected planningLink(session: Session) {
    return { view: 'day', date: session.day, focus: session.id, session: session.id };
  }

  protected async cancelRequest(id: string) {
    this.cancelling.set(id);
    this.requestError.set(null);
    try {
      await this.mySubscriptions.cancelRequest(id);
    } catch (e) {
      this.requestError.set(problemDetail(e));
    } finally {
      this.cancelling.set(null);
    }
  }
}

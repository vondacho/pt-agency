import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ptaRequestHeader } from '../sessions/booking-actions.service';
import { addDays, today } from '../sessions/calendar';
import { HomePage } from './home-page';

describe('HomePage', () => {
  let http: HttpTestingController;
  const tomorrow = addDays(today(), 1);

  async function render(me: object): Promise<ComponentFixture<HomePage>> {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    http.expectOne('/api/me').flush(me);
    await settle(fixture);
    fixture.detectChanges();
    return fixture;
  }


  /** Lets pending promises and effects run, without waiting for open HTTP requests (unlike whenStable). */
  async function settle(f: ComponentFixture<unknown>) {
    await new Promise(resolve => setTimeout(resolve));
    TestBed.tick();
    f.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([ptaRequestHeader])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('welcomes visitors with a way to sign in, the sessions and the shop', async () => {
    const page = (await render({ authenticated: false, linked: false })).nativeElement as HTMLElement;
    expect(page.querySelector('h1')?.textContent).toBe('Train with PT Agency');
    expect([...page.querySelectorAll('.links a')].map(a => a.textContent?.trim())).toEqual(['Sign in', 'See the sessions', 'Subscriptions and passes']);
  });

  it('explains an account not linked to a participant, without asking for personal data', async () => {
    const page = (await render({ authenticated: true, name: 'Carol', linked: false })).nativeElement as HTMLElement;
    expect(page.querySelector('h1')?.textContent).toBe('Welcome, Carol');
    expect(page.textContent).toContain('not linked to a participant yet');
  });

  it('shows the participant\'s next bookings, subscriptions, requests and profile', async () => {
    const fixture = await render({ authenticated: true, name: 'Alice', linked: true });
    http.expectOne('/api/me/bookings').flush([{
      id: 'b1', title: 'Bootcamp', day: tomorrow, start: '10:00:00', stop: '11:00:00', location: 'City park',
      minSeats: 3, maxSeats: 20, bookedSeats: 4, availableSeats: 16, waitlisted: 0, confirmed: true, participantBooking: 'DONE',
    }]);
    http.expectOne('/api/me/subscriptions').flush({
      subscriptions: [{ id: 's1', articleId: 'unlimited', name: 'Unlimited pass', from: '2026-10-24', to: '2027-10-23', expiresSoon: false, upcoming: false }],
      requests: [{ id: 'r1', articleId: 'pt', name: 'Personal training 10x', requestedAt: '2026-11-23T10:00:00Z', status: 'PENDING' }],
    });
    http.expectOne('/api/me/profile').flush({
      firstName: 'Alice', lastName: 'Martin', email: 'alice@example.ch', phone: '+41791234567',
      delivery: { streetNo: 'Rue du Lac 12', zip: '1003', city: 'Lausanne', region: 'VD', country: 'CH' },
    });
    await settle(fixture);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;

    expect(page.querySelector('h1')?.textContent).toBe('Hello, Alice');
    const booking = page.querySelector('.booking') as HTMLElement;
    expect(booking.querySelector('.title')?.textContent).toBe('Bootcamp');
    expect(booking.querySelector('.mine')?.textContent).toBe("You're booked");
    expect(booking.querySelector('button.action')?.textContent?.trim()).toBe('Cancel booking');
    expect(page.querySelector('.subscription .title')?.textContent).toBe('Unlimited pass');
    expect(page.querySelector('.subscription .badge')?.textContent?.trim()).toBe('Active');
    expect(page.querySelector('.request .meta')?.textContent).toContain('awaiting confirmation');
    expect(page.querySelector('.profile dl')?.textContent).toContain('alice@example.ch');
  });

  it('lets the participant cancel a pending request', async () => {
    const fixture = await render({ authenticated: true, name: 'Bob', linked: true });
    http.expectOne('/api/me/bookings').flush([]);
    http.expectOne('/api/me/subscriptions').flush({
      subscriptions: [],
      requests: [{ id: 'r1', articleId: 'monthly', name: 'Monthly group pass', requestedAt: '2026-11-23T10:00:00Z', status: 'PENDING' }],
    });
    http.expectOne('/api/me/profile').flush({ firstName: 'Bob' });
    await settle(fixture);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('.bookings .empty')?.textContent).toContain('No booking yet');

    (page.querySelector('.request button') as HTMLButtonElement).click();
    const cancel = http.expectOne('/api/me/subscription-requests/r1');
    expect(cancel.request.method).toBe('DELETE');
    expect(cancel.request.headers.get('X-PTA-Request')).toBe('1');
    cancel.flush(null, { status: 204, statusText: 'No Content' });
    await settle(fixture);
    http.expectOne('/api/me/subscriptions').flush({ subscriptions: [], requests: [] });
    await settle(fixture);
    fixture.detectChanges();
    expect(page.querySelector('.request')).toBeNull();
    expect(page.querySelector('.subscriptions .empty')?.textContent).toContain('No subscription yet');
  });

  it('flags a subscription ending soon, with a way to renew', async () => {
    const fixture = await render({ authenticated: true, name: 'Alice', linked: true });
    http.expectOne('/api/me/bookings').flush([]);
    http.expectOne('/api/me/subscriptions').flush({
      subscriptions: [{ id: 's1', articleId: 'monthly', name: 'Monthly group pass', from: '2026-11-01', to: '2026-11-30', expiresSoon: true, upcoming: false }],
      requests: [],
    });
    http.expectOne('/api/me/profile').flush({ firstName: 'Alice' });
    await settle(fixture);
    fixture.detectChanges();
    const badge = (fixture.nativeElement as HTMLElement).querySelector('.subscription .badge') as HTMLElement;
    expect(badge.textContent).toContain('Ends soon');
    expect(badge.querySelector('a')?.getAttribute('href')).toBe('/shop');
  });
});

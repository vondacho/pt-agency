import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ptaRequestHeader } from './booking-actions.service';
import { SessionCard } from './session-card';
import { Me, Session } from './session.model';

describe('SessionCard', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<SessionCard>;
  let changes: number;

  const alice: Me = { authenticated: true, name: 'Alice', linked: true };
  const session: Session = {
    id: 'f0000000-0000-0000-0000-000000000001',
    title: 'Small group HIIT',
    day: '2026-11-24',
    start: '18:00:00',
    stop: '19:00:00',
    location: 'Studio B',
    minSeats: 2,
    maxSeats: 5,
    bookedSeats: 3,
    availableSeats: 2,
    waitlisted: 0,
    confirmed: true,
  };

  function render(me: Me, overrides: Partial<Session> = {}) {
    fixture = TestBed.createComponent(SessionCard);
    fixture.componentRef.setInput('session', { ...session, ...overrides });
    fixture.componentRef.setInput('me', me);
    changes = 0;
    fixture.componentInstance.changed.subscribe(() => changes++);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const button = () => (fixture.nativeElement as HTMLElement).querySelector('button.action') as HTMLButtonElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([ptaRequestHeader])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('invites anonymous visitors to sign in', () => {
    const card = render({ authenticated: false, linked: false });
    const link = card.querySelector('a.action') as HTMLAnchorElement;
    expect(link.textContent?.trim()).toBe('Sign in to book');
    expect(link.getAttribute('href')).toBe('/login');
    expect(card.querySelector('button')).toBeNull();
  });

  it('books for the signed-in participant, marked as an SPA request, then asks for a reload', async () => {
    render(alice);
    expect(button().textContent?.trim()).toBe('Book');

    button().click();
    fixture.detectChanges();
    expect(button().disabled).toBe(true);
    expect(button().textContent?.trim()).toBe('Booking…');

    const request = http.expectOne('/api/sessions/f0000000-0000-0000-0000-000000000001/booking');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-PTA-Request')).toBe('1');
    request.flush({ session: session.id, status: 'DONE' }, { status: 201, statusText: 'Created' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(changes).toBe(1);
    expect(button().disabled).toBe(false);
  });

  it('cancels an own booking', async () => {
    const card = render(alice, { participantBooking: 'DONE' });
    expect(card.querySelector('.mine')?.textContent).toBe("You're booked");
    expect(button().textContent?.trim()).toBe('Cancel booking');

    button().click();
    const request = http.expectOne('/api/sessions/f0000000-0000-0000-0000-000000000001/booking');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    expect(changes).toBe(1);
  });

  it('offers the waiting list when the session is full', () => {
    render(alice, { bookedSeats: 5, availableSeats: 0 });
    expect(button().textContent?.trim()).toBe('Join waiting list');
  });

  it('shows why a booking failed and keeps the session as is', async () => {
    const card = render(alice);
    button().click();
    http.expectOne('/api/sessions/f0000000-0000-0000-0000-000000000001/booking').flush(
      { status: 422, detail: 'You have no active subscription valid for this session.' },
      { status: 422, statusText: 'Unprocessable Content' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(card.querySelector('[role="alert"]')?.textContent).toBe('You have no active subscription valid for this session.');
    expect(changes).toBe(0);
  });

  it('asks to sign in again when the session expired', async () => {
    const card = render(alice);
    button().click();
    http.expectOne('/api/sessions/f0000000-0000-0000-0000-000000000001/booking').flush(null, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(card.querySelector('[role="alert"]')?.textContent).toBe('Your session has expired. Please sign in again.');
  });

  it('tells a participant whose account is not linked', () => {
    const card = render({ authenticated: true, name: 'Carol', linked: false });
    expect(card.querySelector('.hint')?.textContent).toBe('Your account is not linked yet');
    expect(card.querySelector('button')).toBeNull();
  });
});

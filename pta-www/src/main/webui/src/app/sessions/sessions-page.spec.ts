import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { addDays, startOfWeek, today } from './calendar';
import { Session } from './session.model';
import { SessionsPage } from './sessions-page';

describe('SessionsPage', () => {
  let http: HttpTestingController;
  // fixtures live next week, so that no session has started whatever the day the tests run
  const monday = addDays(startOfWeek(today()), 7);
  const tuesday = addDays(monday, 1);
  const thursday = addDays(monday, 3);
  const sunday = addDays(monday, 6);

  const session = (overrides: Partial<Session>): Session => ({
    id: crypto.randomUUID(),
    title: 'Bootcamp',
    day: tuesday,
    start: '10:00:00',
    stop: '11:00:00',
    location: 'City park',
    minSeats: 3,
    maxSeats: 20,
    bookedSeats: 5,
    availableSeats: 15,
    waitlisted: 0,
    confirmed: true,
    ...overrides,
  });

  const week = [
    session({ id: 'pt', title: 'Personal training', day: tuesday, start: '07:00:00', stop: '08:00:00', minSeats: 1, maxSeats: 1, bookedSeats: 1, availableSeats: 0 }),
    session({ id: 'hiit', title: 'Small group HIIT', day: tuesday, start: '18:00:00', stop: '19:00:00', participantBooking: 'DONE' }),
    session({ id: 'mobility', title: 'Mobility & stretching', day: thursday, start: '09:00:00', stop: '10:00:00' }),
  ];

  /** Renders the page with the given query parameters and answers who is signed in. */
  function render(params: Record<string, string>): ComponentFixture<SessionsPage> {
    const fixture = TestBed.createComponent(SessionsPage);
    for (const [name, value] of Object.entries(params)) fixture.componentRef.setInput(name, value);
    fixture.detectChanges();
    http.expectOne('/api/me').flush({ authenticated: true, name: 'Alice', linked: true });
    return fixture;
  }

  async function answer(fixture: ComponentFixture<SessionsPage>, sessions: Session[]) {
    http.expectOne(r => r.url === '/api/sessions').flush(sessions);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('week', () => {
    it('loads the week from Monday to Sunday and places each session in its day column', async () => {
      const fixture = render({ date: thursday });
      const request = http.expectOne(r => r.url === '/api/sessions');
      expect(request.request.params.get('from')).toBe(monday);
      expect(request.request.params.get('to')).toBe(sunday);
      request.flush(week);
      await fixture.whenStable();
      fixture.detectChanges();

      const page = fixture.nativeElement as HTMLElement;
      const columns = [...page.querySelectorAll('pta-week-view .column')];
      expect(columns.length).toBe(7);
      expect([...columns[1].querySelectorAll('.block .title')].map(t => t.textContent)).toEqual(['Personal training', 'Small group HIIT']);
      expect([...columns[3].querySelectorAll('.block .title')].map(t => t.textContent)).toEqual(['Mobility & stretching']);
      expect(columns[0].querySelectorAll('.block').length).toBe(0);
    });

    it('positions sessions by time and marks the participant\'s own', async () => {
      const page = await answer(render({ date: monday }), week);
      const [early, evening] = [...page.querySelectorAll<HTMLElement>('pta-week-view .column')[1].querySelectorAll<HTMLElement>('.block')];
      // 56 px per hour from 07:00
      expect(early.style.top).toBe('0px');
      expect(evening.style.top).toBe(`${11 * 56}px`);
      expect(evening.classList).toContain('is-mine');
      expect(early.classList).toContain('is-full');
      expect(evening.getAttribute('aria-label')).toContain("You're booked");
    });

    it('opens the day of a selected session, highlighting it', async () => {
      const fixture = render({ date: monday });
      const page = await answer(fixture, week);
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

      page.querySelector<HTMLButtonElement>('pta-week-view .column:nth-of-type(4) .block')!.click();

      expect(navigate).toHaveBeenCalledWith([], { queryParams: { view: 'day', date: thursday, focus: 'mobility' } });
    });

    it('tells when nothing is scheduled', async () => {
      const page = await answer(render({ date: monday }), []);
      expect(page.querySelector('.notice')?.textContent).toContain('No sessions scheduled this week.');
    });

    it('never goes back before the current week', async () => {
      const fixture = render({ date: addDays(today(), -60) });
      const request = http.expectOne(r => r.url === '/api/sessions');
      expect(request.request.params.get('from')).toBe(startOfWeek(today()));
      request.flush([]);
      await fixture.whenStable();
      fixture.detectChanges();
      expect((fixture.nativeElement as HTMLElement).querySelector('.step [aria-disabled="true"]')).not.toBeNull();
    });
  });

  describe('day', () => {
    it('shows the sessions of the day with their actions, and a strip of the week', async () => {
      const page = await answer(render({ view: 'day', date: tuesday }), week);

      const blocks = [...page.querySelectorAll('pta-day-view .block')];
      expect(blocks.map(b => b.querySelector('.title')?.textContent)).toEqual(['Personal training', 'Small group HIIT']);
      expect(blocks[0].querySelector('button.action')?.textContent?.trim()).toBe('Join waiting list');
      expect(blocks[1].querySelector('.mine')?.textContent).toBe("You're booked");
      expect(blocks[1].querySelector('button.action')?.textContent?.trim()).toBe('Cancel booking');

      const strip = [...page.querySelectorAll('.strip-day')];
      expect(strip.length).toBe(7);
      expect(strip[1].getAttribute('aria-current')).toBe('date');
      expect(strip[1].getAttribute('aria-label')).toContain('2 sessions, with your booking');
      expect(strip[3].getAttribute('aria-label')).toContain('1 session');
    });

    it('highlights the session opened from the week', async () => {
      const page = await answer(render({ view: 'day', date: tuesday, focus: 'hiit' }), week);
      expect(page.querySelector('[data-session="hiit"]')?.classList).toContain('is-focused');
    });

    it('tells when a day has no session', async () => {
      const page = await answer(render({ view: 'day', date: monday }), week);
      expect(page.querySelector('pta-day-view .notice')?.textContent).toContain('No sessions on this day.');
    });

    it('reloads the sessions after a booking', async () => {
      const page = await answer(render({ view: 'day', date: thursday }), week);
      page.querySelector<HTMLButtonElement>('[data-session="mobility"] button.action')!.click();
      http.expectOne(`/api/sessions/mobility/booking`).flush({ session: 'mobility', status: 'PREBOOKED' });
      await Promise.resolve();
      await new Promise(r => setTimeout(r));
      TestBed.tick();
      http.expectOne(r => r.url === '/api/sessions').flush(week);
    });
  });

  it('shows the day view on narrow screens, whatever the requested view', async () => {
    const matchMedia = vi.fn().mockReturnValue({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() });
    vi.stubGlobal('matchMedia', matchMedia);
    try {
      const page = await answer(render({ view: 'week', date: tuesday }), week);
      expect(page.querySelector('pta-week-view')).toBeNull();
      expect(page.querySelector('pta-day-view')).not.toBeNull();
      expect(page.querySelector('.toggle')).toBeNull();
    } finally {
      vi.unstubAllGlobals();
    }
  });

  it('honours the former ?from= links', () => {
    render({ from: thursday });
    expect(http.expectOne(r => r.url === '/api/sessions').request.params.get('from')).toBe(monday);
  });

  it('offers to retry when sessions cannot be loaded', async () => {
    const fixture = render({ date: monday });
    http.expectOne(r => r.url === '/api/sessions').flush(null, { status: 502, statusText: 'Bad Gateway' });
    await fixture.whenStable();
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Sessions could not be loaded.');

    (page.querySelector('[role="alert"] button') as HTMLButtonElement).click();
    fixture.detectChanges();
    http.expectOne(r => r.url === '/api/sessions').flush(week);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(page.querySelectorAll('pta-week-view .block').length).toBe(3);
  });
});

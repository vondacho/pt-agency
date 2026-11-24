import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { addDays, addMonths, monthWeeks, startOfMonth, startOfWeek, today } from './calendar';
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

    it('opens the overview of a selected session, staying on the week', async () => {
      const fixture = render({ date: monday });
      const page = await answer(fixture, week);
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

      page.querySelector<HTMLButtonElement>('pta-week-view .column:nth-of-type(4) .block')!.click();

      expect(navigate).toHaveBeenCalledWith([], { queryParams: { session: 'mobility' }, queryParamsHandling: 'merge' });
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

  describe('month', () => {
    // next month: no session of the fixtures has started
    const first = addMonths(startOfMonth(today()), 1);
    const grid = monthWeeks(first);
    const tenth = addDays(first, 9);
    const twelfth = addDays(first, 11);
    const busy = [0, 1, 2, 3, 4].map(i =>
      session({ id: `busy-${i}`, title: `Class ${i}`, day: tenth, start: `${String(8 + i).padStart(2, '0')}:00:00`, stop: `${String(9 + i).padStart(2, '0')}:00:00` }));
    const month = [...busy, session({ id: 'mine', title: 'Pilates', day: twelfth, participantBooking: 'WAITING' })];

    it('loads every day of the grid, from the Monday before the 1st to the Sunday after the last day', async () => {
      const fixture = render({ view: 'month', date: tenth });
      const request = http.expectOne(r => r.url === '/api/sessions');
      expect(request.request.params.get('from')).toBe(grid[0][0]);
      expect(request.request.params.get('to')).toBe(grid.at(-1)![6]);
      request.flush(month);
      await fixture.whenStable();
      fixture.detectChanges();

      const page = fixture.nativeElement as HTMLElement;
      expect(page.querySelectorAll('pta-month-view tbody tr').length).toBe(grid.length);
      expect([...page.querySelectorAll('pta-month-view th')].length).toBe(7);
    });

    it('lists a few sessions per day in time order, then how many more', async () => {
      const page = await answer(render({ view: 'month', date: tenth }), month);
      const cell = [...page.querySelectorAll<HTMLElement>('pta-month-view td')]
        .find(td => td.querySelector('a.day')?.getAttribute('aria-label')?.includes('5 sessions'))!;
      expect([...cell.querySelectorAll('.session .title')].map(t => t.textContent)).toEqual(['Class 0', 'Class 1', 'Class 2']);
      expect(cell.querySelector('.more')?.textContent?.trim()).toBe('+2 more');
    });

    it('marks the participant\'s bookings and dims the days of other months', async () => {
      const page = await answer(render({ view: 'month', date: tenth }), month);
      expect(page.querySelector('pta-month-view .session.is-mine .title')?.textContent).toBe('Pilates');
      expect(page.querySelector('pta-month-view .session.is-mine')?.getAttribute('aria-label')).toContain('On the waiting list');
      const otherMonthDays = grid.flat().filter(day => !day.startsWith(first.slice(0, 7))).length;
      expect(page.querySelectorAll('pta-month-view td.is-other-month').length).toBe(otherMonthDays);
    });

    it('opens the overview of a selected session, staying on the month', async () => {
      const page = await answer(render({ view: 'month', date: tenth }), month);
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      page.querySelector<HTMLButtonElement>('pta-month-view .session.is-mine')!.click();
      expect(navigate).toHaveBeenCalledWith([], { queryParams: { session: 'mine' }, queryParamsHandling: 'merge' });
    });

    it('moves by whole months', async () => {
      const page = await answer(render({ view: 'month', date: tenth }), month);
      const next = page.querySelector('.step a[aria-label="Next month"]')!.getAttribute('href');
      expect(next).toContain(`date=${addMonths(tenth, 1)}`);
      expect(next).toContain('view=month');
      expect(page.querySelector('.period')?.textContent).toMatch(/\d{4}$/);
    });

    it('stays a month on narrow screens', async () => {
      vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() }));
      try {
        const page = await answer(render({ view: 'month', date: tenth }), month);
        expect(page.querySelector('pta-month-view')).not.toBeNull();
      } finally {
        vi.unstubAllGlobals();
      }
    });
  });

  it('shows the day view on narrow screens, whatever the requested view', async () => {
    const matchMedia = vi.fn().mockReturnValue({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() });
    vi.stubGlobal('matchMedia', matchMedia);
    try {
      const page = await answer(render({ view: 'week', date: tuesday }), week);
      expect(page.querySelector('pta-week-view')).toBeNull();
      expect(page.querySelector('pta-day-view')).not.toBeNull();
      expect([...page.querySelectorAll('.toggle .segment')].map(a => a.textContent?.trim())).toEqual(['Month', 'Day']);
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

  describe('session overview', () => {
    it('opens from a day view title', async () => {
      const page = await answer(render({ view: 'day', date: tuesday }), week);
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      page.querySelector<HTMLButtonElement>('pta-day-view [data-session="hiit"] .title-button')!.click();
      expect(navigate).toHaveBeenCalledWith([], { queryParams: { session: 'hiit' }, queryParamsHandling: 'merge' });
    });

    it('is shown for the session in the URL, closes without it, and can lead to the day', async () => {
      const fixture = render({ date: monday, session: 'hiit' });
      // both requests answered before waiting: a pending request keeps the fixture unstable
      http.expectOne(r => r.url === '/api/sessions').flush(week);
      http.expectOne('/api/sessions/hiit/overview').flush({ session: week[1], cancellationOpen: true, access: [], incomplete: false });
      await fixture.whenStable();
      const page = fixture.nativeElement as HTMLElement;
      fixture.detectChanges();

      expect(page.querySelector('pta-session-overview h2')?.textContent).toContain('Small group HIIT');
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

      page.querySelector<HTMLButtonElement>('pta-session-overview .close')!.click();
      expect(navigate).toHaveBeenCalledWith([], { queryParams: { session: null }, queryParamsHandling: 'merge' });

      page.querySelector<HTMLButtonElement>('pta-session-overview .day-link')!.click();
      expect(navigate).toHaveBeenCalledWith([], { queryParams: { view: 'day', date: tuesday, focus: 'hiit' } });
    });
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { addDays, today } from './calendar';
import { Session } from './session.model';
import { SessionsPage } from './sessions-page';

describe('SessionsPage', () => {
  let http: HttpTestingController;
  const now = today();

  const session = (overrides: Partial<Session>): Session => ({
    id: crypto.randomUUID(),
    title: 'Bootcamp',
    day: addDays(now, 1),
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

  /**
   * Renders the page and lets it issue its request. Not `whenStable()` here: a pending httpResource request
   * keeps the application unstable until the test flushes it.
   */
  async function render(from?: string) {
    const fixture = TestBed.createComponent(SessionsPage);
    if (from) {
      fixture.componentRef.setInput('from', from);
    }
    fixture.detectChanges();
    http.expectOne('/api/me').flush({ authenticated: false, linked: false });
    return fixture;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the coming week and groups the sessions by day', async () => {
    const fixture = await render();
    const request = http.expectOne(r => r.url === '/api/sessions');
    expect(request.request.params.get('from')).toBe(now);
    expect(request.request.params.get('to')).toBe(addDays(now, 6));

    request.flush([
      session({ title: 'Personal training', day: addDays(now, 1), start: '07:00:00', minSeats: 1, maxSeats: 1, bookedSeats: 1, availableSeats: 0 }),
      session({ title: 'Small group HIIT', day: addDays(now, 1), start: '18:00:00', minSeats: 2, maxSeats: 5, bookedSeats: 1, availableSeats: 4, confirmed: false }),
      session({ title: 'Bootcamp', day: addDays(now, 3) }),
    ]);
    await fixture.whenStable();

    const page = fixture.nativeElement as HTMLElement;
    const days = [...page.querySelectorAll('.day h2')].map(h => h.textContent?.trim());
    expect(days.length).toBe(2);
    expect(days[0]).toMatch(/^Tomorrow/);
    expect([...page.querySelectorAll('.title')].map(t => t.textContent)).toEqual(['Personal training', 'Small group HIIT', 'Bootcamp']);
    expect([...page.querySelectorAll('.badge')].map(b => b.textContent)).toEqual(['Full', 'Needs 1 more to take place', '15 seats left']);
  });

  it('never goes back before today', async () => {
    await render(addDays(now, -30));
    expect(http.expectOne(r => r.url === '/api/sessions').request.params.get('from')).toBe(now);
  });

  it('shows the requested week', async () => {
    const fixture = await render(addDays(now, 7));
    http.expectOne(r => r.url === '/api/sessions' && r.params.get('from') === addDays(now, 7)).flush([]);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('.notice')?.textContent).toContain('No sessions scheduled this week.');
  });

  it('offers to retry when sessions cannot be loaded', async () => {
    const fixture = await render();
    http.expectOne(r => r.url === '/api/sessions').flush(null, { status: 502, statusText: 'Bad Gateway' });
    await fixture.whenStable();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Sessions could not be loaded.');

    (page.querySelector('[role="alert"] button') as HTMLButtonElement).click();
    fixture.detectChanges();
    http.expectOne(r => r.url === '/api/sessions').flush([session({})]);
    await fixture.whenStable();
    expect(page.querySelector('[role="alert"]')).toBeNull();
    expect(page.querySelectorAll('pta-session-card').length).toBe(1);
  });
});

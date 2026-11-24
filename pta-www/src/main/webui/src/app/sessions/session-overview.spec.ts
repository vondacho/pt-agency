import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { addDays, startOfWeek, today } from './calendar';
import { SessionOverviewDialog } from './session-overview';
import { SessionOverview } from './session-overview.model';
import { Me, Session } from './session.model';

describe('SessionOverviewDialog', () => {
  let http: HttpTestingController;
  const day = addDays(startOfWeek(today()), 9);
  const alice: Me = { authenticated: true, name: 'Alice', linked: true };

  const session: Session = {
    id: 'bootcamp', title: 'Bootcamp', day, start: '10:00:00', stop: '11:00:00', location: 'Parc de Milan',
    minSeats: 3, maxSeats: 20, bookedSeats: 2, availableSeats: 18, waitlisted: 0, confirmed: false,
    instructor: 'Marco Rossi',
  };

  const overview = (overrides: Partial<SessionOverview> = {}): SessionOverview => ({
    session,
    course: { objectives: ['Outdoor circuits', 'Team spirit'], level: 'All levels', prerequisites: 'From 14 years.', whatToBring: 'Water and a mat.' },
    instructor: { id: 'i', name: 'Marco Rossi', bio: 'Group coach.' },
    venue: { id: 'v', name: 'Parc de Milan', address: 'Avenue de Milan, 1007 Lausanne', accessNotes: 'At the fountain.', mapUrl: 'https://map.example/milan' },
    note: 'Partner challenge',
    cancellationHours: 12,
    cancellableUntil: `${addDays(day, -1)}T22:00:00`,
    cancellationOpen: true,
    access: [
      { id: 'u', name: 'Unlimited pass', onSale: true, held: false },
      { id: 'g', name: '10-session group card', onSale: true, held: false },
    ],
    youHoldOne: false,
    incomplete: false,
    ...overrides,
  });

  async function render(answer: SessionOverview | 'error', me: Me = alice) {
    const fixture: ComponentFixture<SessionOverviewDialog> = TestBed.createComponent(SessionOverviewDialog);
    fixture.componentRef.setInput('sessionId', 'bootcamp');
    fixture.componentRef.setInput('me', me);
    fixture.componentRef.setInput('today', today());
    fixture.componentRef.setInput('now', 0);
    fixture.detectChanges();
    const request = http.expectOne('/api/sessions/bootcamp/overview');
    if (answer === 'error') request.flush({ detail: 'down' }, { status: 502, statusText: 'Bad Gateway' });
    else request.flush(answer);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, dialog: fixture.nativeElement.querySelector('dialog') as HTMLDialogElement };
  }

  const sectionText = (dialog: HTMLElement, heading: string) =>
    [...dialog.querySelectorAll('section')].find(s => s.querySelector('h3')?.textContent === heading)?.textContent ?? '';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('opens as a dialog named after the session', async () => {
    const { dialog } = await render(overview());
    expect(dialog.open).toBe(true);
    expect(dialog.getAttribute('aria-labelledby')).toBe('overview-title');
    expect(dialog.querySelector('#overview-title')?.textContent).toBe('Bootcamp');
    expect(dialog.querySelector('.when')?.textContent).toContain('10:00–11:00');
  });

  it('presents the objectives, the instructor and the venue', async () => {
    const { dialog } = await render(overview());
    expect([...dialog.querySelectorAll('.objectives li')].map(li => li.textContent)).toEqual(['Outdoor circuits', 'Team spirit']);
    expect(sectionText(dialog, "What you'll work on")).toContain('All levels');
    expect(sectionText(dialog, 'Instructor')).toContain('Marco Rossi');
    expect(sectionText(dialog, 'Instructor')).toContain('Group coach.');
    expect(sectionText(dialog, 'Venue')).toContain('Avenue de Milan, 1007 Lausanne');
    expect(sectionText(dialog, 'Venue')).toContain('At the fountain.');
    const map = dialog.querySelector<HTMLAnchorElement>('a[href="https://map.example/milan"]')!;
    expect(map.target).toBe('_blank');
    expect(map.rel).toContain('noopener');
    expect(dialog.querySelector('.note')?.textContent).toBe('Partner challenge');
  });

  it('tells the conditions to take part', async () => {
    const { dialog } = await render(overview());
    const conditions = sectionText(dialog, 'To take part');
    expect(conditions).toContain('Included in: Unlimited pass, 10-session group card.');
    expect(conditions).toContain("You don't hold one yet.");
    expect(dialog.querySelector('a[href="/shop"]')?.textContent).toContain('See the shop');
    expect(conditions).toContain('20 seats, 2 taken.');
    expect(conditions).toContain('at least 3 participants');
    expect(conditions).toContain('waiting list');
    expect(conditions).toMatch(/Free cancellation until .*12 h before the start/);
    expect(conditions).toContain('From 14 years.');
    expect(conditions).toContain('Water and a mat.');
  });

  it('tells a participant which subscription they hold', async () => {
    const { dialog } = await render(overview({
      youHoldOne: true,
      access: [{ id: 's', name: 'Summer pass', onSale: false, held: true }],
    }));
    expect(sectionText(dialog, 'To take part')).toContain('You have one: Summer pass.');
    expect(dialog.querySelector('a[href="/shop"]')).toBeNull();
  });

  it('offers the booking actions, and closes the cancellation of a booked seat past the deadline', async () => {
    const { dialog } = await render(overview());
    expect(dialog.querySelector('pta-session-actions button')?.textContent).toContain('Book');

    http.verify();
    const closed = await render(overview({ session: { ...session, participantBooking: 'DONE' }, cancellationOpen: false }));
    expect(closed.dialog.querySelector('pta-session-actions button')).toBeNull();
    expect(closed.dialog.querySelector('pta-session-actions .hint')?.textContent).toContain('Cancellation closed');
    expect(sectionText(closed.dialog, 'To take part')).toContain('Cancellations closed');
  });

  it('invites to sign in when nobody is signed in', async () => {
    const { dialog } = await render(overview({ youHoldOne: undefined }), { authenticated: false, linked: false });
    expect(dialog.querySelector('pta-session-actions a[href="/login"]')?.textContent).toContain('Sign in to book');
    expect(sectionText(dialog, 'To take part')).not.toContain("You don't hold one");
  });

  it('says when details are missing, and offers to retry on error', async () => {
    const { dialog } = await render(overview({ course: undefined, access: [], youHoldOne: undefined, incomplete: true }));
    expect(dialog.querySelector('.incomplete')?.textContent).toContain('temporarily unavailable');
    expect(sectionText(dialog, "What you'll work on")).toBe('');

    const failed = await render('error');
    expect(failed.dialog.querySelector('[role="alert"]')?.textContent).toContain('could not be loaded');
    failed.dialog.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    TestBed.tick();
    http.expectOne('/api/sessions/bootcamp/overview').flush(overview());
  });

  it('asks to close on Esc, on the close button and on the backdrop', async () => {
    const { fixture, dialog } = await render(overview());
    let closes = 0;
    fixture.componentInstance.closed.subscribe(() => closes++);

    const esc = new Event('cancel', { cancelable: true });
    dialog.dispatchEvent(esc);
    expect(esc.defaultPrevented).toBe(true);
    dialog.querySelector<HTMLButtonElement>('.close')!.click();
    dialog.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    dialog.querySelector<HTMLElement>('.body')!.click();
    expect(closes).toBe(3);
  });

  it('gives the focus back to what opened it', async () => {
    const opener = document.createElement('button');
    document.body.appendChild(opener);
    opener.focus();
    try {
      const { fixture } = await render(overview());
      fixture.destroy();
      expect(document.activeElement).toBe(opener);
    } finally {
      opener.remove();
    }
  });
});

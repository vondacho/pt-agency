import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CatalogueItem, MySubscriptions } from '../me/my-space.model';
import { ptaRequestHeader } from '../sessions/booking-actions.service';
import { ShopPage } from './shop-page';

describe('ShopPage', () => {
  let http: HttpTestingController;

  const catalogue: CatalogueItem[] = [
    { id: 'monthly', kind: 'SUBSCRIPTION', name: 'Monthly group pass', price: 139, duration: '1 month', opens: ['Bootcamp', 'Small group hiit'] },
    { id: 'card', kind: 'PASS', name: '10-session group card', price: 220, duration: '1 year', sessions: 10, opens: ['Bootcamp'] },
    { id: 'pt', kind: 'PASS', name: 'Personal training 10x', price: 800, duration: '1 year', sessions: 10, opens: ['Personal training'] },
  ];

  async function render(me: object, mine?: MySubscriptions): Promise<ComponentFixture<ShopPage>> {
    const fixture = TestBed.createComponent(ShopPage);
    fixture.detectChanges();
    http.expectOne('/api/catalogue').flush(catalogue);
    http.expectOne('/api/me').flush(me);
    await settle(fixture);
    if (mine) {
      http.expectOne('/api/me/subscriptions').flush(mine);
      await settle(fixture);
    }
    fixture.detectChanges();
    return fixture;
  }

  const card = (fixture: ComponentFixture<ShopPage>, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-article="${id}"]`) as HTMLElement;


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

  it('groups subscriptions and passes, with what each one gives access to', async () => {
    const fixture = await render({ authenticated: false, linked: false });
    const page = fixture.nativeElement as HTMLElement;
    expect([...page.querySelectorAll('.group h2')].map(h => h.textContent)).toEqual(['Subscriptions', 'Passes']);
    const monthly = card(fixture, 'monthly');
    expect(monthly.querySelector('.facts')?.textContent).toContain('1 month');
    expect(monthly.querySelector('.facts')?.textContent).toContain('Unlimited sessions');
    expect([...monthly.querySelectorAll('.opens li')].map(l => l.textContent)).toEqual(['Bootcamp', 'Small group hiit']);
    expect(card(fixture, 'card').querySelector('.facts')?.textContent).toContain('10 sessions');
    expect(monthly.querySelector('a.action')?.textContent?.trim()).toBe('Sign in to request');
  });

  it('lets a participant request a subscription, which then shows as requested', async () => {
    const fixture = await render({ authenticated: true, name: 'Bob', linked: true }, { subscriptions: [], requests: [] });
    const button = card(fixture, 'monthly').querySelector('button') as HTMLButtonElement;
    expect(button.textContent?.trim()).toBe('Request this subscription');

    button.click();
    const request = http.expectOne('/api/me/subscription-requests');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ articleId: 'monthly' });
    expect(request.request.headers.get('X-PTA-Request')).toBe('1');
    request.flush({ id: 'r1', articleId: 'monthly', name: 'Monthly group pass', requestedAt: '2026-11-23T10:00:00Z', status: 'PENDING' });
    await settle(fixture);

    http.expectOne('/api/me/subscriptions').flush({
      subscriptions: [],
      requests: [{ id: 'r1', articleId: 'monthly', name: 'Monthly group pass', requestedAt: '2026-11-23T10:00:00Z', status: 'PENDING' }],
    });
    await settle(fixture);
    fixture.detectChanges();
    expect(card(fixture, 'monthly').querySelector('.state')?.textContent).toBe('Requested · awaiting confirmation');
    expect(card(fixture, 'monthly').querySelector('button')?.textContent?.trim()).toBe('Cancel request');
  });

  it('shows the subscriptions the participant already holds', async () => {
    const fixture = await render({ authenticated: true, name: 'Alice', linked: true }, {
      subscriptions: [{ id: 's', articleId: 'card', name: '10-session group card', from: '2026-11-01', to: '2027-10-31', sessions: 10, expiresSoon: false, upcoming: false }],
      requests: [],
    });
    expect(card(fixture, 'card').querySelector('.state.is-owned')?.textContent).toMatch(/^You have it until .*2027/);
    expect(card(fixture, 'card').querySelector('button')).toBeNull();
  });

  it('explains a refused request', async () => {
    const fixture = await render({ authenticated: true, name: 'Bob', linked: true }, { subscriptions: [], requests: [] });
    (card(fixture, 'pt').querySelector('button') as HTMLButtonElement).click();
    http.expectOne('/api/me/subscription-requests').flush(
      { status: 409, detail: 'You have already requested this subscription.' }, { status: 409, statusText: 'Conflict' });
    await settle(fixture);
    fixture.detectChanges();
    expect(card(fixture, 'pt').querySelector('[role="alert"]')?.textContent).toBe('You have already requested this subscription.');
  });
});

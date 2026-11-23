import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';

import { legacyPlanningLinks, routes } from './app.routes';

describe('routes', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter(routes)] }));

  const run = (queryParams: Record<string, string>) =>
    TestBed.runInInjectionContext(() =>
      legacyPlanningLinks({ queryParams } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

  it('send former planning links to the sessions, with their parameters', () => {
    const result = run({ view: 'day', date: '2026-11-26' });
    expect(result instanceof UrlTree).toBe(true);
    expect((result as UrlTree).toString()).toBe('/sessions?view=day&date=2026-11-26');
    expect((run({ from: '2026-11-23' }) as UrlTree).toString()).toBe('/sessions?from=2026-11-23');
  });

  it('keep the home page otherwise', () => {
    expect(run({})).toBe(true);
  });

  it('offer home, sessions, shop and profile', () => {
    expect(routes.map(r => r.path)).toEqual(['', 'sessions', 'shop', 'me/profile', '**']);
  });
});

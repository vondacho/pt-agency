import { CatalogueItem, MySubscriptions, offerFor, price, shortDate } from './my-space.model';

describe('shop offer', () => {
  const item: CatalogueItem = { id: 'monthly', kind: 'SUBSCRIPTION', name: 'Monthly group pass', duration: '1 month', opens: [] };
  const alice = { authenticated: true, linked: true };
  const none: MySubscriptions = { subscriptions: [], requests: [] };

  it('invites visitors to sign in and explains unlinked accounts', () => {
    expect(offerFor(item, { authenticated: false, linked: false }, none)).toEqual({ kind: 'sign-in' });
    expect(offerFor(item, { authenticated: true, linked: false }, none)).toEqual({ kind: 'not-linked' });
  });

  it('offers to request an article the participant does not hold', () => {
    expect(offerFor(item, alice, none)).toEqual({ kind: 'request' });
  });

  it('shows a pending request instead', () => {
    const request = { id: 'r', articleId: 'monthly', name: 'Monthly group pass', requestedAt: '2026-11-23T10:00:00Z', status: 'PENDING' as const };
    expect(offerFor(item, alice, { subscriptions: [], requests: [request] })).toEqual({ kind: 'requested', request });
  });

  it('shows an owned subscription, but offers to renew one ending soon', () => {
    const owned = { id: 's', articleId: 'monthly', name: 'Monthly group pass', from: '2026-11-01', to: '2026-12-31', expiresSoon: false, upcoming: false };
    expect(offerFor(item, alice, { subscriptions: [owned], requests: [] })).toEqual({ kind: 'owned', until: '2026-12-31' });
    expect(offerFor(item, alice, { subscriptions: [{ ...owned, expiresSoon: true }], requests: [] })).toEqual({ kind: 'request' });
  });
});

describe('formats', () => {
  it('shows dates and prices for people', () => {
    expect(shortDate('2027-11-12')).toMatch(/12.*2027/);
    expect(shortDate('2026-11-23T10:15:00Z')).toMatch(/23.*2026/);
    expect(price(139)).toMatch(/CHF\s?139|139\s?CHF/);
    expect(price(undefined)).toBe('');
  });
});

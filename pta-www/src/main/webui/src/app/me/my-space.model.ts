/** A subscription or pass of the shop's catalogue, as served by `GET /api/catalogue`. */
export interface CatalogueItem {
  id: string;
  /** SUBSCRIPTION: unlimited for a period · PASS: a number of sessions */
  kind: 'SUBSCRIPTION' | 'PASS';
  name: string;
  description?: string;
  price?: number;
  /** e.g. "1 month" */
  duration: string;
  /** Sessions included, absent when unlimited. */
  sessions?: number;
  /** Sessions it gives access to, e.g. "Bootcamp". */
  opens: string[];
}

export interface MySubscription {
  id: string;
  articleId: string;
  name: string;
  /** ISO dates */
  from: string;
  to?: string;
  sessions?: number;
  expiresSoon: boolean;
  upcoming: boolean;
}

export interface MyRequest {
  id: string;
  articleId: string;
  name: string;
  requestedAt: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
}

export interface MySubscriptions {
  subscriptions: MySubscription[];
  requests: MyRequest[];
}

export interface Address {
  streetNo?: string;
  zip?: string;
  city?: string;
  region?: string;
  country?: string;
}

export interface Profile {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  delivery?: Address;
  billing?: Address;
}

/** What the shop offers for an article, given the participant's subscriptions and requests. */
export type Offer =
  | { kind: 'sign-in' }
  | { kind: 'not-linked' }
  | { kind: 'request' }
  | { kind: 'requested'; request: MyRequest }
  | { kind: 'owned'; until?: string };

export function offerFor(item: CatalogueItem, me: { authenticated: boolean; linked: boolean }, mine: MySubscriptions): Offer {
  if (!me.authenticated) return { kind: 'sign-in' };
  if (!me.linked) return { kind: 'not-linked' };
  const request = mine.requests.find(r => r.articleId === item.id && r.status === 'PENDING');
  if (request) return { kind: 'requested', request };
  const owned = mine.subscriptions.find(s => s.articleId === item.id && !s.expiresSoon);
  if (owned) return { kind: 'owned', until: owned.to };
  return { kind: 'request' };
}

const dateFormat = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' });

/** `2027-11-12` → `12 Nov 2027` (in the participant's locale) */
export function shortDate(isoDate: string): string {
  const [year, month, day] = isoDate.slice(0, 10).split('-').map(Number);
  return dateFormat.format(new Date(year, month - 1, day));
}

const priceFormat = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'CHF', minimumFractionDigits: 0, maximumFractionDigits: 2 });

export function price(value?: number): string {
  return value == null ? '' : priceFormat.format(value);
}

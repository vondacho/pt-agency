import { httpResource } from '@angular/common/http';
import { computed, Injectable } from '@angular/core';

import { CatalogueItem } from '../me/my-space.model';

/** The shop's catalogue (public). */
@Injectable({ providedIn: 'root' })
export class CatalogueService {
  readonly items = httpResource<CatalogueItem[]>(() => '/api/catalogue', { defaultValue: [] });

  /** Subscriptions and passes, in the catalogue's order. */
  readonly groups = computed(() => {
    const items = this.items.hasValue() ? this.items.value() : [];
    return [
      { kind: 'SUBSCRIPTION' as const, title: 'Subscriptions', intro: 'Unlimited sessions for a period.', items: items.filter(i => i.kind === 'SUBSCRIPTION') },
      { kind: 'PASS' as const, title: 'Passes', intro: 'A number of sessions, to use within the validity.', items: items.filter(i => i.kind === 'PASS') },
    ].filter(g => g.items.length > 0);
  });
}

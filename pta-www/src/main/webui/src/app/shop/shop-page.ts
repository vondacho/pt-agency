import { Component, computed, inject, signal } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { CatalogueItem, offerFor, price, shortDate } from '../me/my-space.model';
import { MySubscriptionsService } from '../me/my-space.service';
import { problemDetail } from '../sessions/booking-actions.service';
import { CatalogueService } from './catalogue.service';

/**
 * The catalogue of subscriptions and passes. Participants request one; the agency confirms it once paid.
 */
@Component({
  selector: 'pta-shop-page',
  imports: [],
  templateUrl: './shop-page.html',
  styleUrl: './shop-page.scss',
})
export class ShopPage {
  protected readonly catalogue = inject(CatalogueService);
  private readonly auth = inject(AuthService);
  private readonly mySpace = inject(MySubscriptionsService);

  protected readonly me = this.auth.me;
  private readonly mine = computed(() =>
    this.mySpace.subscriptions.hasValue() ? this.mySpace.subscriptions.value() : { subscriptions: [], requests: [] });

  /** Article being requested or cancelled, and the last error per article. */
  protected readonly pending = signal<string | null>(null);
  protected readonly errors = signal<Record<string, string>>({});

  protected offer(item: CatalogueItem) {
    return offerFor(item, this.me(), this.mine());
  }

  protected price = price;
  protected shortDate = shortDate;

  protected async request(item: CatalogueItem) {
    await this.run(item, () => this.mySpace.request(item.id));
  }

  protected async cancel(item: CatalogueItem, requestId: string) {
    await this.run(item, () => this.mySpace.cancelRequest(requestId));
  }

  private async run(item: CatalogueItem, action: () => Promise<unknown>) {
    this.pending.set(item.id);
    this.errors.update(errors => ({ ...errors, [item.id]: '' }));
    try {
      await action();
    } catch (e) {
      this.errors.update(errors => ({ ...errors, [item.id]: problemDetail(e) }));
    } finally {
      this.pending.set(null);
    }
  }
}

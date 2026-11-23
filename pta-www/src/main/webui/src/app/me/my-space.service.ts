import { HttpClient, httpResource } from '@angular/common/http';
import { computed, inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { Session } from '../sessions/session.model';
import { Address, MyRequest, MySubscriptions, Profile } from './my-space.model';

/**
 * The signed-in participant's data, one service per resource: a page only loads what it shows, and nothing is
 * requested until the participant is known to be signed in and linked.
 */
function linked() {
  const me = inject(AuthService).me;
  return computed(() => me().authenticated && me().linked);
}

const NONE: MySubscriptions = { subscriptions: [], requests: [] };

@Injectable({ providedIn: 'root' })
export class MyBookings {
  private readonly linked = linked();
  readonly bookings = httpResource<Session[]>(() => (this.linked() ? '/api/me/bookings' : undefined), { defaultValue: [] });
}

@Injectable({ providedIn: 'root' })
export class MySubscriptionsService {
  private readonly http = inject(HttpClient);
  private readonly linked = linked();
  readonly subscriptions = httpResource<MySubscriptions>(() => (this.linked() ? '/api/me/subscriptions' : undefined), { defaultValue: NONE });

  async request(articleId: string): Promise<MyRequest> {
    const request = await firstValueFrom(this.http.post<MyRequest>('/api/me/subscription-requests', { articleId }));
    this.subscriptions.reload();
    return request;
  }

  async cancelRequest(requestId: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`/api/me/subscription-requests/${requestId}`));
    this.subscriptions.reload();
  }
}

@Injectable({ providedIn: 'root' })
export class MyProfile {
  private readonly http = inject(HttpClient);
  private readonly linked = linked();
  readonly profile = httpResource<Profile>(() => (this.linked() ? '/api/me/profile' : undefined));

  async saveContact(email: string, phone: string): Promise<void> {
    await firstValueFrom(this.http.put<void>('/api/me/profile/contact', { email, phone }));
    this.profile.reload();
  }

  async saveAddresses(delivery: Address, billing?: Address): Promise<void> {
    await firstValueFrom(this.http.put<void>('/api/me/profile/address', { delivery, billing: billing ?? null }));
    this.profile.reload();
  }
}

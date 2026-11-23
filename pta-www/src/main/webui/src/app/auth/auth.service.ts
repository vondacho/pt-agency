import { httpResource } from '@angular/common/http';
import { computed, Injectable } from '@angular/core';

import { Me } from '../sessions/session.model';

const ANONYMOUS: Me = { authenticated: false, linked: false };

/**
 * Who is signed in. Sign-in and sign-out are full-page navigations to the server
 * (`/login`, `/logout`): the session lives in an HttpOnly cookie, tokens never reach the browser.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly meResource = httpResource<Me>(() => '/api/me', { defaultValue: ANONYMOUS });

  /** Anonymous until known, and when /api/me fails: the page stays usable for browsing. */
  readonly me = computed(() => (this.meResource.hasValue() ? this.meResource.value() : ANONYMOUS));
  readonly loaded = computed(() => !this.meResource.isLoading());
}

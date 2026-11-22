import { httpResource } from '@angular/common/http';
import { Injectable, Signal } from '@angular/core';

import { Session } from './session.model';

export interface Period {
  from: string;
  to: string;
}

@Injectable({ providedIn: 'root' })
export class SessionService {
  /** Sessions taking place within the period, in chronological order; reloads whenever the period changes. */
  sessionsIn(period: Signal<Period>) {
    return httpResource<Session[]>(() => ({
      url: '/api/sessions',
      params: { from: period().from, to: period().to },
    }), { defaultValue: [] });
  }
}

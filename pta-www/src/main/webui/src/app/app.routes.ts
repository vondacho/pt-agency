import { Routes } from '@angular/router';

import { SessionsPage } from './sessions/sessions-page';

export const routes: Routes = [
  { path: '', component: SessionsPage, title: 'Upcoming sessions · PT Agency' },
  { path: '**', redirectTo: '' },
];

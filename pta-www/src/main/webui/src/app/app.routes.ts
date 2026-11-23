import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';

import { HomePage } from './home/home-page';
import { ProfilePage } from './me/profile-page';
import { SessionsPage } from './sessions/sessions-page';
import { ShopPage } from './shop/shop-page';

/** The planning used to live at the root: links with its parameters still lead to it. */
export const legacyPlanningLinks: CanActivateFn = route => {
  const params = route.queryParams;
  return 'view' in params || 'date' in params || 'from' in params
    ? inject(Router).createUrlTree(['/sessions'], { queryParams: params })
    : true;
};

export const routes: Routes = [
  { path: '', component: HomePage, canActivate: [legacyPlanningLinks], title: 'PT Agency' },
  { path: 'sessions', component: SessionsPage, title: 'Sessions · PT Agency' },
  { path: 'shop', component: ShopPage, title: 'Shop · PT Agency' },
  { path: 'me/profile', component: ProfilePage, title: 'My profile · PT Agency' },
  { path: '**', redirectTo: '' },
];

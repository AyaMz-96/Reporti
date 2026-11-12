// web/src/app/app.routes.ts
import { Routes } from '@angular/router';

import { Home } from './pages/home';
import { InfoPage } from './pages/info-page';
import { ReportForm } from './report-form';
import { ReportList } from './report-list';
import { MapPage } from './pages/map-page';

import { authGuard } from './auth/auth-guard';
import { adminGuard } from './auth/role-guard';
import { LoginPage } from './pages/login-page';
import { RegisterPage } from './pages/register-page';
import { ForgotPasswordPage } from './pages/forgot-password-page';
import { ResetPasswordPage } from './pages/reset-password-page';
import { AccountPage } from './pages/account-page';
import { UsersPage } from './pages/users-page';
import { DashboardPage } from './pages/dashboard-page';
import { CategoriesPage } from './pages/categories-page';
import { AgentsPage } from './pages/agents-page';

export const routes: Routes = [
  { path: '', component: Home, title: 'Reporti' },
  { path: 'report', component: ReportForm, title: 'Submit a report' },
  { path: 'reports', component: ReportList, title: 'Reports' },
  { path: 'map', component: MapPage, title: 'Reports map' },

  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPage },
  { path: 'forgot-password', component: ForgotPasswordPage },
  { path: 'reset-password', component: ResetPasswordPage },
  { path: 'account', component: AccountPage, canActivate: [authGuard] },
  { path: 'dashboard', component: DashboardPage, title: 'Dashboard', canActivate: [adminGuard] },
  { path: 'admin/users', component: UsersPage, title: 'Users', canActivate: [adminGuard] },
  { path: 'admin/agents', component: AgentsPage, title: 'Agents', canActivate: [adminGuard] },
  { path: 'admin/categories', component: CategoriesPage, title: 'Categories', canActivate: [adminGuard] },

  {
    path: 'contact',
    component: InfoPage,
    title: 'Contact',
    data: { title: 'Contact', message: 'Contact page content will be added here.' },
  },
  {
    path: 'about',
    component: InfoPage,
    title: 'About Reporti',
    data: { title: 'About Reporti', message: 'Reporti helps communities document and follow urban issues.' },
  },

  // Wildcard route (must remain last)
  { path: '**', redirectTo: '', pathMatch: 'full' },
];
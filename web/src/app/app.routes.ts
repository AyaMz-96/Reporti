import { Routes } from '@angular/router';

import { Home } from './pages/home';
import { InfoPage } from './pages/info-page';
import { ReportForm } from './report-form';
import { ReportList } from './report-list';
import { MapPage } from './pages/map-page';

export const routes: Routes = [
  { path: '', component: Home, title: 'Reporti' },
  { path: 'report', component: ReportForm, title: 'Submit a report' },
  { path: 'reports', component: ReportList, title: 'Reports' },
  { path: 'map', component: MapPage, title: 'Reports map' },
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
    data: {
      title: 'About Reporti',
      message: 'Reporti helps communities document and follow urban issues.',
    },
  },
  { path: '**', redirectTo: '', pathMatch: 'full' },
];

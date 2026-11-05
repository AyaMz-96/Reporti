import { Routes } from '@angular/router';

import { ReportList } from './report-list';

export const routes: Routes = [
  { path: '', component: ReportList, title: 'Reporti - Reports' },
  // The map page is added in a later commit
  { path: '**', redirectTo: '' },
];

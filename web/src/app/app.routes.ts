import { Routes } from '@angular/router';

import { ReportList } from './report-list';
import { ReportMap } from './report-map';

export const routes: Routes = [
  { path: '', component: ReportList, title: 'Reporti - Reports' },
  { path: 'map', component: ReportMap, title: 'Reporti - Map' },
  { path: '**', redirectTo: '' },
];

import { Component } from '@angular/core';

import { NotificationBell } from './notification-bell';
import { ReportForm } from './report-form';
import { ReportList } from './report-list';

@Component({
  selector: 'app-root',
  imports: [NotificationBell, ReportForm, ReportList],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
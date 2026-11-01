import { Component } from '@angular/core';

import { ReportForm } from './report-form';

@Component({
  selector: 'app-root',
  imports: [ReportForm],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
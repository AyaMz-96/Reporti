import { Component, OnInit, signal, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthApi } from '../auth/auth-api';

import { NotificationBell } from '../notification-bell';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, NotificationBell],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header implements OnInit {
  protected readonly auth = inject(AuthApi);

  readonly languageOpen = signal(false);
  readonly darkMode = signal(false);
  readonly language = signal('EN');
  readonly adminMenuOpen = signal(false);

  protected logout(): void {
    this.auth.logout();
  }

  ngOnInit(): void {
    const savedTheme = localStorage.getItem('reporti-theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const dark = savedTheme ? savedTheme === 'dark' : prefersDark;

    this.darkMode.set(dark);
    document.documentElement.classList.toggle('dark-theme', dark);
  }

  toggleLanguages(): void {
    this.languageOpen.update((open) => !open);
  }

  selectLanguage(code: string): void {
    this.language.set(code);
    this.languageOpen.set(false);
  }

  toggleTheme(): void {
    this.darkMode.update((enabled) => {
      const next = !enabled;
      document.documentElement.classList.toggle('dark-theme', next);
      localStorage.setItem('reporti-theme', next ? 'dark' : 'light');
      return next;
    });
  }

  toggleAdminMenu(): void {
    this.adminMenuOpen.update((v) => !v);
  }

  closeAdminMenu(): void {
    this.adminMenuOpen.set(false);
  }
}

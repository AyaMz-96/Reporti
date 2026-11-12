import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Category } from '../models';
import { CategoryApi } from '../category-api';
import { readError } from '../report-api';

@Component({
  selector: 'app-categories-page',
  imports: [FormsModule],
  template: `
    <main class="wrap">
      <header class="top">
        <div>
          <h1>Category management</h1>
          <p class="muted">{{ categories().length }} category(ies)</p>
        </div>
        <button type="button" class="primary" (click)="openCreate()">+ New category</button>
      </header>

      @if (error()) { <p class="err">{{ error() }}</p> }
      @if (info()) { <p class="ok">{{ info() }}</p> }

      @if (formOpen()) {
        <section class="card">
          <h2>{{ editingId() ? 'Edit category' : 'New category' }}</h2>
          <form (ngSubmit)="save()" class="form">
            <label>Name
              <input name="name" [(ngModel)]="form.name" required />
            </label>
            <label>Description
              <input name="description" [(ngModel)]="form.description" />
            </label>
            <div class="actions">
              <button type="submit" class="primary" [disabled]="saving()">Save</button>
              <button type="button" (click)="closeForm()">Cancel</button>
            </div>
          </form>
        </section>
      }

      <section class="card table-card">
        @if (loading()) {
          <p class="muted">Loading…</p>
        } @else {
          <table>
            <thead>
              <tr><th>ID</th><th>Name</th><th>Description</th><th></th></tr>
            </thead>
            <tbody>
              @for (c of categories(); track c.id) {
                <tr>
                  <td>{{ c.id }}</td>
                  <td>{{ c.name }}</td>
                  <td>{{ c.description || '—' }}</td>
                  <td class="row-actions">
                    <button type="button" (click)="openEdit(c)">Edit</button>
                    <button type="button" class="danger" (click)="remove(c)">Delete</button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="muted">No categories found.</td></tr>
              }
            </tbody>
          </table>
        }
      </section>
    </main>
  `,
  styles: [`
    .wrap { max-width: 900px; margin: 2rem auto; padding: 0 1rem; display: grid; gap: 1.25rem; color: #1c1c1a; }
    .top { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; flex-wrap: wrap; }
    h1, h2 { margin: 0; }
    .muted { color: #6b6b66; margin: .25rem 0 0; }
    .card { background: #fffdf8; border-radius: 20px; padding: 1.5rem; box-shadow: 0 8px 24px rgba(0,0,0,.08); }
    .table-card { overflow-x: auto; }
    .form { display: grid; gap: .9rem; margin-top: 1rem; }
    label { display: grid; gap: .35rem; font-weight: 600; }
    input { padding: .65rem .85rem; border: 1px solid #d6d0c2; border-radius: 12px; font: inherit; }
    .actions { display: flex; gap: .75rem; }
    button { padding: .55rem 1rem; border: 1px solid #d6d0c2; border-radius: 999px; background: #fff; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: .45; cursor: default; }
    .primary { background: #d92f2f; color: #fff; border-color: #d92f2f; }
    .danger { color: #b3261e; border-color: #e3b5b1; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: .75rem .6rem; border-bottom: 1px solid #e6e1d6; }
    th { font-size: .8rem; text-transform: uppercase; letter-spacing: .04em; color: #6b6b66; }
    .row-actions { display: flex; gap: .4rem; flex-wrap: wrap; }
    .err { color: #b3261e; margin: 0; }
    .ok { color: #1b7f3b; margin: 0; }
  `]
})
export class CategoriesPage implements OnInit {
  private readonly api = inject(CategoryApi);

  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly info = signal('');
  protected readonly formOpen = signal(false);
  protected readonly editingId = signal<number | null>(null);

  protected form = { name: '', description: '' };

  ngOnInit(): void {
    this.load();
  }

  protected openCreate(): void {
    this.editingId.set(null);
    this.form = { name: '', description: '' };
    this.error.set('');
    this.formOpen.set(true);
  }

  protected openEdit(c: Category): void {
    this.editingId.set(c.id);
    this.form = { name: c.name, description: c.description || '' };
    this.error.set('');
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editingId.set(null);
  }

  protected save(): void {
    this.error.set('');
    this.info.set('');
    if (!this.form.name.trim()) {
      this.error.set('Name is required.');
      return;
    }

    this.saving.set(true);
    const payload = { name: this.form.name.trim(), description: this.form.description.trim() || null };
    const id = this.editingId();

    const req = id
      ? this.api.updateCategory(id, payload)
      : this.api.createCategory(payload);

    req.subscribe({
      next: () => {
        this.saving.set(false);
        this.info.set(id ? 'Category updated.' : 'Category created.');
        this.closeForm();
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.error.set(readError(e));
      },
    });
  }

  protected remove(c: Category): void {
    if (!confirm(`Delete category "${c.name}"? This cannot be undone.`)) return;
    this.error.set('');
    this.info.set('');
    this.api.deleteCategory(c.id).subscribe({
      next: () => {
        this.info.set('Category deleted.');
        this.load();
      },
      error: (e) => this.error.set(readError(e)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.getCategories().subscribe({
      next: (list) => {
        this.categories.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(readError(e));
        this.loading.set(false);
      },
    });
  }
}

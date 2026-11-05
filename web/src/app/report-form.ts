import { Component, ElementRef, OnInit, inject, output, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { forkJoin } from 'rxjs';

import { Category, Sector } from './models';
import { LocationPicker, PickedLocation } from './location-picker';
import { ReportApi, readError } from './report-api';

// Same limits as spring.servlet.multipart.* in application.properties
const MAX_FILE_BYTES = 10 * 1024 * 1024;
const MAX_TOTAL_BYTES = 30 * 1024 * 1024;

@Component({
  selector: 'app-report-form',
  imports: [ReactiveFormsModule, LocationPicker],
  templateUrl: './report-form.html',
  styleUrl: './report-form.css',
})
export class ReportForm implements OnInit {
  private readonly api = inject(ReportApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly photoInput = viewChild<ElementRef<HTMLInputElement>>('photoInput');

  /** Emits the id of the new report once it (and its photos) were sent. */
  readonly submitted = output<number>();

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    categoryId: this.fb.control<number | null>(null, Validators.required),
    sectorId: this.fb.control<number | null>(null, Validators.required),
    latitude: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(-90),
      Validators.max(90),
    ]),
    longitude: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(-180),
      Validators.max(180),
    ]),
  });

  // Mirror the coordinates as signals so the map pin follows the inputs (typed, picked or geolocated)
  readonly latitude = toSignal(this.form.controls.latitude.valueChanges, {
    initialValue: this.form.controls.latitude.value,
  });
  readonly longitude = toSignal(this.form.controls.longitude.valueChanges, {
    initialValue: this.form.controls.longitude.value,
  });

  // The app is zoneless, so everything that changes after an async call must be a signal
  readonly categories = signal<Category[]>([]);
  readonly sectors = signal<Sector[]>([]);
  readonly files = signal<File[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly locating = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly locationError = signal<string | null>(null);
  readonly fileError = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  /** Set when the report was created but its photos failed, so a retry never creates a duplicate report. */
  readonly pendingUploadId = signal<number | null>(null);

  ngOnInit(): void {
    forkJoin({
      categories: this.api.getCategories(),
      sectors: this.api.getSectors(),
    }).subscribe({
      next: ({ categories, sectors }) => {
        this.categories.set(categories);
        this.sectors.set(sectors);
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(`Could not load categories and sectors: ${readError(error)}`);
        this.loading.set(false);
      },
    });
  }

  showError(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  formatSize(bytes: number): string {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  onLocationPicked(location: PickedLocation): void {
    this.form.patchValue(location);
    this.form.controls.latitude.markAsTouched();
    this.form.controls.longitude.markAsTouched();
    this.locationError.set(null);
  }

  useMyLocation(): void {
    if (!('geolocation' in navigator)) {
      this.locationError.set('Geolocation is not supported by this browser. Type the coordinates instead.');
      return;
    }

    this.locating.set(true);
    this.locationError.set(null);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.form.patchValue({
          latitude: Number(position.coords.latitude.toFixed(6)),
          longitude: Number(position.coords.longitude.toFixed(6)),
        });
        this.locating.set(false);
      },
      (error) => {
        this.locating.set(false);
        this.locationError.set(
          error.code === error.PERMISSION_DENIED
            ? 'Location permission was denied. Type the coordinates instead.'
            : 'Could not get your location. Type the coordinates instead.',
        );
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const selected = Array.from(input.files ?? []);
    this.fileError.set(null);

    const tooBig = selected.find((file) => file.size > MAX_FILE_BYTES);
    const total = selected.reduce((sum, file) => sum + file.size, 0);

    if (tooBig) {
      this.fileError.set(`"${tooBig.name}" is larger than 10 MB`);
    } else if (total > MAX_TOTAL_BYTES) {
      this.fileError.set('The selected files add up to more than 30 MB');
    } else {
      this.files.set(selected);
      return;
    }

    input.value = '';
    this.files.set([]);
  }

  submit(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);

    // The report already exists, only the photos are left to send
    const pendingId = this.pendingUploadId();
    if (pendingId !== null) {
      this.uploadPhotos(pendingId);
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);

    this.api
      .createReport({
        title: value.title,
        description: value.description,
        latitude: value.latitude!,
        longitude: value.longitude!,
        categoryId: value.categoryId!,
        sectorId: value.sectorId!,
      })
      .subscribe({
        next: (report) => this.uploadPhotos(report.id),
        error: (error) => {
          this.submitting.set(false);
          this.errorMessage.set(readError(error));
        },
      });
  }

  private uploadPhotos(reportId: number): void {
    const files = this.files();
    if (files.length === 0) {
      this.finish(reportId);
      return;
    }

    this.submitting.set(true);
    this.api.uploadMedia(reportId, files).subscribe({
      next: () => this.finish(reportId),
      error: (error) => {
        this.submitting.set(false);
        this.pendingUploadId.set(reportId);
        this.form.disable();
        this.errorMessage.set(
          `Report #${reportId} was created, but the photo upload failed: ${readError(error)}`,
        );
      },
    });
  }

  private finish(reportId: number): void {
    this.submitting.set(false);
    this.pendingUploadId.set(null);
    this.form.enable();
    this.form.reset();
    this.files.set([]);

    const input = this.photoInput();
    if (input) {
      input.nativeElement.value = '';
    }

    this.submitted.emit(reportId);
  }
}

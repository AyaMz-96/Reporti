import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import * as L from 'leaflet';
import { forkJoin } from 'rxjs';

import { Category, PriorityLevel, Sector } from './models';
import { ReportApi, readError } from './report-api';

const MAX_FILE_BYTES = 10 * 1024 * 1024;
const MAX_TOTAL_BYTES = 30 * 1024 * 1024;

const DEFAULT_CENTER: L.LatLngExpression = [36.8065, 10.1815];
const DEFAULT_ZOOM = 8;
const PIN_ZOOM = 15;

const pinIcon = L.divIcon({
  className: 'report-pin',
  html: `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="40" viewBox="0 0 28 40">
           <path d="M14 0C6.27 0 0 6.27 0 14c0 10.5 14 26 14 26s14-15.5 14-26c0-7.73-6.27-14-14-14z"
                 fill="#d63031" stroke="#fff" stroke-width="2"/>
           <circle cx="14" cy="14" r="5" fill="#fff"/>
         </svg>`,
  iconSize: [28, 40],
  iconAnchor: [14, 40],
});

@Component({
  selector: 'app-report-form',
  imports: [ReactiveFormsModule],
  templateUrl: './report-form.html',
  styleUrl: './report-form.css',
})
export class ReportForm implements OnInit, AfterViewInit {
  private readonly api = inject(ReportApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly photoInput = viewChild<ElementRef<HTMLInputElement>>('photoInput');
  private readonly mapContainer = viewChild<ElementRef<HTMLDivElement>>('locationMap');

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;

  readonly priorities: PriorityLevel[] = ['URGENT', 'NORMAL', 'LOW'];

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    categoryId: this.fb.control<number | null>(null, Validators.required),
    sectorId: this.fb.control<number | null>(null, Validators.required),
    priority: this.fb.control<PriorityLevel>('NORMAL', Validators.required),
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

  readonly pendingUploadId = signal<number | null>(null);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.map?.remove());
  }

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

  ngAfterViewInit(): void {
    this.initMap();
  }

  showError(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  formatSize(bytes: number): string {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  selectPriority(level: PriorityLevel): void {
    this.form.controls.priority.setValue(level);
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
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        this.setPinAt(lat, lng, true);
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
        priority: value.priority,
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
    this.marker?.setOpacity(0);
    this.map?.setView(DEFAULT_CENTER, DEFAULT_ZOOM);

    const input = this.photoInput();
    if (input) {
      input.nativeElement.value = '';
    }

    this.successMessage.set(`Report #${reportId} submitted. Thank you!`);
  }

  private initMap(): void {
    const el = this.mapContainer()?.nativeElement;
    if (!el || this.map) return;

    const formValue = this.form.getRawValue();
    const hasPin =
      typeof formValue.latitude === 'number' &&
      typeof formValue.longitude === 'number';

    const initialCenter: L.LatLngExpression = hasPin
      ? [formValue.latitude!, formValue.longitude!]
      : DEFAULT_CENTER;
    const initialZoom = hasPin ? PIN_ZOOM : DEFAULT_ZOOM;

    this.map = L.map(el, {
      scrollWheelZoom: false,
      zoomControl: true,
    }).setView(initialCenter, initialZoom);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.map);

    this.marker = L.marker(initialCenter, {
      icon: pinIcon,
      draggable: true,
      autoPan: true,
      opacity: hasPin ? 1 : 0,
    }).addTo(this.map);

    this.marker.on('dragend', () => {
      const p = this.marker!.getLatLng();
      this.setPinAt(p.lat, p.lng, false);
    });

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.setPinAt(e.latlng.lat, e.latlng.lng, false);
    });

    this.form.valueChanges.subscribe((v) => {
      const lat = v.latitude;
      const lng = v.longitude;
      if (
        typeof lat === 'number' &&
        typeof lng === 'number' &&
        Number.isFinite(lat) &&
        Number.isFinite(lng)
      ) {
        this.marker?.setLatLng([lat, lng]);
        this.marker?.setOpacity(1);
      } else {
        this.marker?.setOpacity(0);
      }
    });

    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private setPinAt(lat: number, lng: number, recenter: boolean): void {
    if (!this.marker || !this.map) return;
    this.marker.setLatLng([lat, lng]);
    this.marker.setOpacity(1);
    if (recenter) {
      this.map.setView([lat, lng], Math.max(this.map.getZoom(), PIN_ZOOM));
    }
    this.form.patchValue(
      { latitude: Number(lat.toFixed(6)), longitude: Number(lng.toFixed(6)) },
      { emitEvent: false },
    );
  }
}

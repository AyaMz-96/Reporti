import { Component, DestroyRef, ElementRef, afterNextRender, effect, inject, input, output, signal, viewChild } from '@angular/core';
import * as L from 'leaflet';

// Centre of Tunisia, used until a location exists
const TUNISIA_CENTER: L.LatLngTuple = [33.9, 9.5];
const TUNISIA_ZOOM = 6;
// Zoom used when the pin is placed from "use my location" or typed coordinates
const PIN_ZOOM = 16;

export interface PickedLocation {
  latitude: number;
  longitude: number;
}

function round6(value: number): number {
  return Number(value.toFixed(6));
}

function isValid(latitude: number | null, longitude: number | null): boolean {
  return (
    latitude !== null &&
    longitude !== null &&
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    Math.abs(latitude) <= 90 &&
    Math.abs(longitude) <= 180
  );
}

/**
 * A small map with one draggable pin. The form stays the single source of truth:
 * clicking the map or dragging the pin emits `picked`, and the pin always follows the
 * latitude/longitude inputs (typed, picked, or filled by "Use my current location").
 */
@Component({
  selector: 'app-location-picker',
  templateUrl: './location-picker.html',
  styleUrl: './location-picker.css',
})
export class LocationPicker {
  private readonly container = viewChild.required<ElementRef<HTMLDivElement>>('pickerMap');
  private readonly destroyRef = inject(DestroyRef);

  readonly latitude = input<number | null>(null);
  readonly longitude = input<number | null>(null);
  readonly disabled = input(false);
  readonly picked = output<PickedLocation>();

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private readonly mapReady = signal(false);

  constructor() {
    afterNextRender(() => {
      const map = L.map(this.container().nativeElement).setView(TUNISIA_CENTER, TUNISIA_ZOOM);
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(map);

      map.on('click', (event: L.LeafletMouseEvent) => {
        if (!this.disabled()) {
          this.picked.emit({
            latitude: round6(event.latlng.lat),
            longitude: round6(event.latlng.lng),
          });
        }
      });

      this.map = map;
      this.mapReady.set(true);
      // The map lives in a modal that has just appeared, so make sure Leaflet measures its final size
      setTimeout(() => map.invalidateSize(), 0);
    });

    // Keep the pin in sync with the form values
    effect(() => {
      const latitude = this.latitude();
      const longitude = this.longitude();
      const disabled = this.disabled();
      if (!this.mapReady() || !this.map) {
        return;
      }

      if (!isValid(latitude, longitude)) {
        this.marker?.remove();
        this.marker = null;
        return;
      }

      const position: L.LatLngTuple = [latitude!, longitude!];
      if (!this.marker) {
        this.marker = this.createMarker(position);
        this.map.setView(position, Math.max(this.map.getZoom(), PIN_ZOOM));
      } else {
        this.marker.setLatLng(position);
        if (!this.map.getBounds().contains(position)) {
          this.map.setView(position, Math.max(this.map.getZoom(), PIN_ZOOM));
        }
      }

      if (disabled) {
        this.marker.dragging?.disable();
      } else {
        this.marker.dragging?.enable();
      }
    });

    this.destroyRef.onDestroy(() => this.map?.remove());
  }

  private createMarker(position: L.LatLngTuple): L.Marker {
    // A divIcon avoids Leaflet's default marker images, which bundlers tend to break
    const icon = L.divIcon({
      className: '',
      html: '<div style="font-size:32px;line-height:32px;text-shadow:0 1px 3px rgba(0,0,0,.4)">📍</div>',
      iconSize: [32, 32],
      iconAnchor: [16, 30],
    });

    const marker = L.marker(position, { icon, draggable: true }).addTo(this.map!);
    marker.on('dragend', () => {
      const { lat, lng } = marker.getLatLng();
      this.picked.emit({ latitude: round6(lat), longitude: round6(lng) });
    });
    return marker;
  }
}

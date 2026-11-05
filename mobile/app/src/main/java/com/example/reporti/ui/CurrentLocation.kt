package com.example.reporti.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** Asks the device for one fresh location. The caller must already hold a location permission. */
@SuppressLint("MissingPermission")
fun fetchCurrentLocation(
    context: Context,
    onLocation: (Location) -> Unit,
    onFailure: (String) -> Unit,
) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

    if (providers.isEmpty()) {
        onFailure("Location is turned off on this device. Turn it on or type the coordinates instead.")
        return
    }

    // Check last known location first across enabled providers for an immediate result
    var bestLocation: Location? = null
    for (p in providers) {
        val loc = runCatching { manager.getLastKnownLocation(p) }.getOrNull() ?: continue
        if (bestLocation == null || loc.accuracy < bestLocation.accuracy || loc.time > bestLocation.time) {
            bestLocation = loc
        }
    }

    if (bestLocation != null) {
        onLocation(bestLocation)
        return
    }

    // Fallback: request a new location from the primary provider
    val primaryProvider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: providers.first()
    LocationManagerCompat.getCurrentLocation(
        manager,
        primaryProvider,
        null as CancellationSignal?,
        ContextCompat.getMainExecutor(context)
    ) { location ->
        if (location != null) {
            onLocation(location)
        } else {
            val lastLoc = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            if (lastLoc != null) {
                onLocation(lastLoc)
            } else {
                onFailure("Could not get your location. Type the coordinates instead.")
            }
        }
    }
}

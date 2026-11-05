package com.example.reporti.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.data.PriorityLevel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun ReportFormScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportFormViewModel = viewModel(),
) {
    val context = LocalContext.current
    val locked = viewModel.fieldsLocked

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.photos = uris
        }
    }

    fun locate() {
        viewModel.startLocating()
        fetchCurrentLocation(
            context = context,
            onLocation = { viewModel.onLocationFound(it.latitude, it.longitude) },
            onFailure = viewModel::onLocationFailed,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) {
            locate()
        } else {
            viewModel.onLocationFailed("Location permission was denied. Type the coordinates instead.")
        }
    }

    fun onLocationClick() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val granted = permissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) locate() else permissionLauncher.launch(permissions)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Report a problem", style = MaterialTheme.typography.headlineSmall)

        viewModel.loadError?.let { message ->
            Banner(message, isError = true)
            TextButton(onClick = viewModel::load) { Text("Retry") }
        }

        OutlinedTextField(
            value = viewModel.title,
            onValueChange = { viewModel.title = it },
            label = { Text("Title") },
            isError = viewModel.showErrors && viewModel.titleError != null,
            supportingText = supporting(viewModel.titleError, viewModel.showErrors),
            enabled = !locked,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = viewModel.description,
            onValueChange = { viewModel.description = it },
            label = { Text("Description") },
            isError = viewModel.showErrors && viewModel.descriptionError != null,
            supportingText = supporting(viewModel.descriptionError, viewModel.showErrors),
            enabled = !locked,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Dropdown(
            label = "Category",
            options = viewModel.categories.map { it.id to it.name },
            selectedId = viewModel.categoryId,
            onSelected = { viewModel.categoryId = it },
            enabled = !locked,
            error = viewModel.categoryError.takeIf { viewModel.showErrors },
        )

        Dropdown(
            label = "Sector",
            options = viewModel.sectors.map { sector ->
                sector.id to (sector.name + (sector.zone?.let { " ($it)" } ?: ""))
            },
            selectedId = viewModel.sectorId,
            onSelected = { viewModel.sectorId = it },
            enabled = !locked,
            error = viewModel.sectorError.takeIf { viewModel.showErrors },
        )

        Text("Priority", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityLevel.entries.forEach { level ->
                FilterChip(
                    selected = viewModel.priority == level,
                    onClick = { viewModel.priority = level },
                    label = { Text(level.name) },
                    enabled = !locked,
                )
            }
        }

        Text("Location", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = ::onLocationClick,
            enabled = !locked && !viewModel.locating,
        ) {
            Text(if (viewModel.locating) "Locating…" else "Use my current location")
        }
        viewModel.locationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = viewModel.latitude,
                onValueChange = { viewModel.latitude = it },
                label = { Text("Latitude") },
                isError = viewModel.showErrors && viewModel.latitudeError != null,
                supportingText = supporting(viewModel.latitudeError, viewModel.showErrors),
                enabled = !locked,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = viewModel.longitude,
                onValueChange = { viewModel.longitude = it },
                label = { Text("Longitude") },
                isError = viewModel.showErrors && viewModel.longitudeError != null,
                supportingText = supporting(viewModel.longitudeError, viewModel.showErrors),
                enabled = !locked,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        LocationPickerMap(
            latitude = viewModel.latitude.replace(',', '.').toDoubleOrNull(),
            longitude = viewModel.longitude.replace(',', '.').toDoubleOrNull(),
            enabled = !locked,
            onPicked = viewModel::onLocationPicked,
        )

        Text("Photos or videos (optional)", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            enabled = !viewModel.submitting,
        ) {
            Text("Add photos or videos")
        }
        if (viewModel.photos.isNotEmpty()) {
            val count = viewModel.photos.size
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (count == 1) "1 file selected" else "$count files selected",
                    modifier = Modifier.padding(top = 12.dp),
                )
                TextButton(
                    onClick = { viewModel.photos = emptyList() },
                    enabled = !viewModel.submitting,
                ) {
                    Text("Remove all")
                }
            }
        }

        viewModel.errorMessage?.let { Banner(it, isError = true) }
        viewModel.successMessage?.let { Banner(it, isError = false) }

        Button(
            onClick = viewModel::submit,
            enabled = !viewModel.submitting && !viewModel.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    viewModel.submitting -> "Sending…"
                    viewModel.pendingUploadId != null -> "Retry photo upload"
                    else -> "Submit report"
                }
            )
        }
    }
}

/** Shows the error under a field only after the user has tried to submit once. */
private fun supporting(error: String?, show: Boolean): (@Composable () -> Unit)? =
    if (show && error != null) {
        { Text(error) }
    } else {
        null
    }

@Composable
private fun Dropdown(
    label: String,
    options: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
    enabled: Boolean,
    error: String?,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = options.firstOrNull { it.first == selectedId }?.second ?: "Choose…",
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelected(id)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Banner(text: String, isError: Boolean) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun LocationPickerMap(
    latitude: Double?,
    longitude: Double?,
    enabled: Boolean,
    onPicked: (Double, Double) -> Unit,
) {
    val context = LocalContext.current

    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = java.io.File(context.cacheDir, "osmdroid")
            osmdroidTileCache = java.io.File(context.cacheDir, "osmdroid/tiles")
        }
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(6.0)
            controller.setCenter(GeoPoint(33.9, 9.5))
        }
    }
    val marker = remember { Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) } }

    DisposableEffect(Unit) {
        mapView.onResume()
        val listener = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (enabled) onPicked(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint) = false
        })
        mapView.overlays.add(listener)
        mapView.overlays.add(marker)
        onDispose {
            mapView.overlays.clear()
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            val p = GeoPoint(latitude, longitude)
            marker.position = p
            mapView.controller.animateTo(p, 16.0, 400L)
            mapView.invalidate()
        } else {
            marker.position = null
            mapView.invalidate()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
    )
}
package com.example.reporti.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.max
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.data.PriorityLevel
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
)

@Composable
fun ReportFormScreen(
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
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
        Text("Report a problem", style = MaterialTheme.typography.headlineSmall, color = Color.White)

        viewModel.loadError?.let { message ->
            Banner(message, isError = true)
            TextButton(onClick = viewModel::load) { Text("Retry") }
        }

        OutlinedTextField(
            value = viewModel.title,
            onValueChange = { viewModel.title = it },
            label = { Text("Title", color = Color.White) },
            isError = viewModel.showErrors && viewModel.titleError != null,
            supportingText = supporting(viewModel.titleError, viewModel.showErrors),
            enabled = !locked,
            singleLine = true,
            colors = textFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = viewModel.description,
            onValueChange = { viewModel.description = it },
            label = { Text("Description", color = Color.White) },
            isError = viewModel.showErrors && viewModel.descriptionError != null,
            supportingText = supporting(viewModel.descriptionError, viewModel.showErrors),
            enabled = !locked,
            minLines = 3,
            colors = textFieldColors(),
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

        Text("Priority", style = MaterialTheme.typography.labelLarge, color = Color.White)
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

        Text("Location", style = MaterialTheme.typography.titleSmall, color = Color.White)
        OutlinedButton(
            onClick = ::onLocationClick,
            enabled = !locked && !viewModel.locating,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        ) {
            Text(
                text = if (viewModel.locating) "Locating…" else "Use my current location",
                color = Color.White
            )
        }
        viewModel.locationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = viewModel.latitude,
                onValueChange = { viewModel.latitude = it },
                label = { Text("Latitude", color = Color.White) },
                isError = viewModel.showErrors && viewModel.latitudeError != null,
                supportingText = supporting(viewModel.latitudeError, viewModel.showErrors),
                enabled = !locked,
                singleLine = true,
                colors = textFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = viewModel.longitude,
                onValueChange = { viewModel.longitude = it },
                label = { Text("Longitude", color = Color.White) },
                isError = viewModel.showErrors && viewModel.longitudeError != null,
                supportingText = supporting(viewModel.longitudeError, viewModel.showErrors),
                enabled = !locked,
                singleLine = true,
                colors = textFieldColors(),
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

        Text("Photos or videos (optional)", style = MaterialTheme.typography.titleSmall, color = Color.White)
        OutlinedButton(
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            enabled = !viewModel.submitting,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
        ) {
            Text("Add photos or videos", color = Color.White)
        }
        if (viewModel.photos.isNotEmpty()) {
            val count = viewModel.photos.size
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (count == 1) "1 file selected" else "$count files selected",
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp),
                )
                TextButton(
                    onClick = { viewModel.photos = emptyList() },
                    enabled = !viewModel.submitting,
                ) {
                    Text("Remove all", color = Color.White)
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

        // Scrollable room under the last button so the floating nav bar never sits on top of it
        Spacer(Modifier.height(bottomContentPadding))
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
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = options.firstOrNull { it.first == selectedId }?.second ?: "Choose…",
                    textAlign = TextAlign.Start,
                    color = Color.White,
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

// Zoom used when the pin is first placed (same as the web picker)
private const val PIN_ZOOM = 16.0

/**
 * A small map with one draggable pin, like the web LocationPicker.
 * Tap the map to place the pin, long-press the pin then drag it to adjust it.
 * The form stays the single source of truth: the pin always follows the latitude/longitude fields.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
private fun LocationPickerMap(
    latitude: Double?,
    longitude: Double?,
    enabled: Boolean,
    onPicked: (Double, Double) -> Unit,
) {
    val context = LocalContext.current

    // The listeners below live as long as the map, so they must read the latest values, not the first ones
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnPicked by rememberUpdatedState(onPicked)

    // Sets the User-Agent OpenStreetMap requires, see OsmSetup.kt
    remember {
        initOsm(context)
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(reportiTileSource())
            setMultiTouchControls(true)
            controller.setZoom(6.0)
            controller.setCenter(GeoPoint(33.9, 9.5))
            // The map sits inside a scrolling form: without this the form steals every drag and the map never pans
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isDraggable = true
            isEnabled = false   // hidden until there is a position
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragStart(marker: Marker) {}
                override fun onMarkerDragEnd(marker: Marker) {
                    if (currentEnabled) {
                        currentOnPicked(marker.position.latitude, marker.position.longitude)
                    }
                }
            })
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()

        val listener = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (currentEnabled) currentOnPicked(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint) = false
        })
        mapView.overlays.add(listener)
        mapView.overlays.add(marker)
        // OpenStreetMap attribution is mandatory under the tile usage policy
        mapView.overlays.add(CopyrightOverlay(context))

        onDispose {
            mapView.overlays.clear()
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Pin can not be dragged while the form is locked
    LaunchedEffect(enabled) {
        marker.isDraggable = enabled
    }

    LaunchedEffect(latitude, longitude) {
        val valid = latitude != null && longitude != null && abs(latitude) <= 90 && abs(longitude) <= 180
        if (valid) {
            val p = GeoPoint(latitude!!, longitude!!)
            val firstPlacement = !marker.isEnabled
            marker.position = p
            marker.isEnabled = true
            // Same rule as the web picker: zoom in on the first pin, afterwards only move if the pin left the screen
            if (firstPlacement || !mapView.boundingBox.contains(p)) {
                mapView.controller.animateTo(p, max(mapView.zoomLevelDouble, PIN_ZOOM), 400L)
            }
        } else {
            marker.isEnabled = false
        }
        mapView.invalidate()
    }

    // The map gets its own clipped, bordered box: tiles drawn while panning can no longer spill over the form
    val mapShape = RoundedCornerShape(12.dp)
    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                clipChildren = true
                clipToPadding = true
                // Re-entering composition would otherwise crash: the MapView still has its old parent
                (mapView.parent as? ViewGroup)?.removeView(mapView)
                addView(
                    mapView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(mapShape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), mapShape),
    )
    Text(
        "Tap the map to place the pin, or long-press the pin and drag it to adjust.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.8f),
    )
}

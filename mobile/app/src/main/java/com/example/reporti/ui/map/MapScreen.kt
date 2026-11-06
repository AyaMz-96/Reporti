package com.example.reporti.ui.map

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.ui.initOsm
import com.example.reporti.ui.reportiTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

// Room kept free at the bottom so the floating navigation bar does not cover the legend
private val BOTTOM_BAR_SPACE = 110.dp

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    viewModel: MapViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Reload every time the tab is opened (web reloads when a report is created)
    LaunchedEffect(Unit) { viewModel.load() }

    // Sets the User-Agent OpenStreetMap requires, see OsmSetup.kt
    remember {
        initOsm(context)
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(reportiTileSource())
            setBackgroundColor(android.graphics.Color.rgb(231, 226, 208))
            setMultiTouchControls(true)
            setUseDataConnection(true)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(6.0)
            controller.setCenter(GeoPoint(36.8065, 10.1815))
            // Tunis, matching the web map's default position
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        // OpenStreetMap attribution is mandatory under the tile usage policy
        mapView.overlays.add(CopyrightOverlay(context))
        onDispose {
            mapView.overlays.clear()
            mapView.onPause()
            mapView.onDetach()
        }
    }

    val density = LocalDensity.current.density
    LaunchedEffect(state.reports, state.categories, density) {
        mapView.overlays.removeAll { it is Marker }
        val points = mutableListOf<GeoPoint>()
        for (report in state.reports) {
            if (!isValidCoordinate(report.latitude, report.longitude)) continue
            val point = GeoPoint(report.latitude, report.longitude)
            points.add(point)

            val fill = statusFill(report.status)
            val (stroke, strokeWidth, radius) = priorityStroke(report.priority)

            mapView.overlays.add(
                Marker(mapView).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = dot(fill, stroke, strokeWidth * density, radius * density)
                    // Same content as the web popup: title, category · status · priority, date
                    title = report.title
                    snippet = "${state.categoryName(report.categoryId)} · " +
                        "${report.status.replace('_', ' ')} · ${report.priority}\n" +
                        shortDate(report.createdAt)
                }
            )
        }
        when {
            points.size == 1 -> {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(points.first())
            }
            points.size > 1 -> mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 80)
            else -> {
                mapView.controller.setZoom(10.0)
                mapView.controller.setCenter(GeoPoint(36.8065, 10.1815))
            }
        }
        mapView.invalidate()
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            update = {
                it.post {
                    it.onResume()
                    it.requestLayout()
                    it.invalidate()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }

        state.error?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(message)
                    TextButton(onClick = viewModel::load) { Text("Retry") }
                }
            }
        }

        val bottomPadding = if (bottomContentPadding > 0.dp) bottomContentPadding else BOTTOM_BAR_SPACE
        Legend(
            count = state.reports.size,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = bottomPadding + 12.dp),
        )
    }
}

private val LEGEND_STATUSES = listOf("RECEIVED", "IN_PROGRESS", "RESOLVED", "REJECTED")
private val LEGEND_PRIORITIES = listOf("LOW", "NORMAL", "URGENT")

/** Same legend as the web map: fill = status, outline = priority. Tap to collapse. */
@Composable
private fun Legend(count: Int, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.clickable { open = !open },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(10.dp)) {
            Text("$count report(s)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (open) {
                Spacer(Modifier.size(4.dp))
                LEGEND_STATUSES.forEach { status ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(Color(statusFill(status)), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(status.replace('_', ' '), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.size(4.dp))
                Text("Outline", fontSize = 11.sp, color = Color.Gray)
                LEGEND_PRIORITIES.forEach { priority ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                // white outlines would vanish on the light card, so LOW uses a light grey like the web legend
                                .border(2.dp, if (priority == "LOW") Color(0xFFCBD5E1) else Color(priorityStroke(priority).first), CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(priority.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun statusFill(status: String): Int = when (status) {
    "RECEIVED"    -> 0xFF2563EB.toInt()
    "IN_PROGRESS" -> 0xFFF59E0B.toInt()
    "RESOLVED"    -> 0xFF16A34A.toInt()
    "REJECTED"    -> 0xFF6B7280.toInt()
    else          -> 0xFF6B7280.toInt()
}

private fun priorityStroke(priority: String): Triple<Int, Float, Float> = when (priority) {
    "URGENT" -> Triple(0xFFDC2626.toInt(), 4f, 12f)
    "NORMAL" -> Triple(0xFFFACC15.toInt(), 3f, 8f)
    else     -> Triple(0xFFFFFFFF.toInt(), 2f, 8f)
}

private fun dot(fill: Int, stroke: Int, strokePx: Float, radiusPx: Float): Drawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        setStroke(strokePx.toInt().coerceAtLeast(1), stroke)
        val d = (radiusPx.toInt() * 2).coerceAtLeast(4)
        setSize(d, d)
    }

private fun shortDate(iso: String?): String =
    iso?.take(10) ?: ""

private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() &&
        longitude.isFinite() &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)

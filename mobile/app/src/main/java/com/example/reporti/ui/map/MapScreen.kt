package com.example.reporti.ui.map

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
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

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    val density = LocalDensity.current.density
    LaunchedEffect(state.reports, density) {
        mapView.overlays.removeAll { it is Marker }
        val points = mutableListOf<GeoPoint>()
        for (report in state.reports) {
            if (report.latitude == 0.0 && report.longitude == 0.0) continue
            val point = GeoPoint(report.latitude, report.longitude)
            points.add(point)

            val fill = statusFill(report.status)
            val (stroke, strokeWidth, radius) = priorityStroke(report.priority)

            mapView.overlays.add(
                Marker(mapView).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = dot(fill, stroke, strokeWidth * density, radius * density)
                    title = report.title
                    snippet = "${report.status.replace('_', ' ')} • ${report.priority}"
                }
            )
        }
        if (points.isNotEmpty()) {
            mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 80)
        }
        mapView.invalidate()
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
        state.error?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
            ) {
                Text(message, modifier = Modifier.padding(12.dp))
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
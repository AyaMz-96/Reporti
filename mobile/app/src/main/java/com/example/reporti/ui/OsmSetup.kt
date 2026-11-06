package com.example.reporti.ui

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.io.File

fun initOsm(context: Context) {
    val config = Configuration.getInstance()
    config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    val userAgent = "ReportiApp/1.0 (${context.packageName})"
    config.userAgentValue = userAgent
    config.userAgentHttpHeader = userAgent
    val basePath = File(context.filesDir, "osmdroid")
    val tileCache = File(basePath, "tiles")
    if (!tileCache.exists()) tileCache.mkdirs()
    config.osmdroidBasePath = basePath
    config.osmdroidTileCache = tileCache
    config.cacheMapTileCount = 9
    config.tileDownloadThreads = 4
    config.tileDownloadMaxQueueSize = 40
}

fun reportiTileSource(): ITileSource = XYTileSource(
    "OpenStreetMap",
    3,
    19,
    256,
    ".png",
    arrayOf(
        "https://tile.openstreetmap.org/",
    ),
    "© OpenStreetMap contributors",
)

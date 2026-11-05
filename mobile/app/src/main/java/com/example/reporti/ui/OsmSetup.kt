package com.example.reporti.ui

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.io.File

/**
 * OpenStreetMap's own tile servers (tile.openstreetmap.org) are volunteer-run and now block most apps
 * ("Access blocked. App is not following the tile usage policy"), even with a custom User-Agent.
 * Their policy tells apps to use a third-party tile provider instead, so the map uses MapTiler
 * (free plan, needs a free API key: https://cloud.maptiler.com/ -> Account -> API keys).
 * The map data is still OpenStreetMap's, and the attribution below is required by both MapTiler and OSM.
 *
 * Do not commit the key to a public repository.
 */
private const val MAPTILER_KEY = "jnuDzG1SYPynbPy70B4S"

// Still sent with every tile request; use your real contact
private const val USER_AGENT = "ReportiApp/1.0 (student project; contact: rami.neji123@gmail.com)"

fun reportiTileSource(): OnlineTileSourceBase = object : OnlineTileSourceBase(
    "MapTilerStreets",
    0,
    19,
    256,
    ".png",
    arrayOf("https://api.maptiler.com/maps/streets-v2/256/"),
    "© MapTiler © OpenStreetMap contributors",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) +
            mImageFilenameEnding + "?key=" + MAPTILER_KEY
}

fun initOsm(context: Context) {
    Configuration.getInstance().apply {
        userAgentValue = USER_AGENT
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
}

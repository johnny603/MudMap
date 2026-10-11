package com.mudmap

import com.mudmap.storage.TrailEntry
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

class MapViewController(
    mapView: MapView,
    private val onEntryTapped: (TrailEntry) -> Unit
) {
    private var map: MapLibreMap? = null
    private var styleReady = false
    private var framed = false
    private var entries = emptyList<TrailEntry>()
    private val markerEntries = mutableMapOf<Long, TrailEntry>()

    init {
        mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.setOnMarkerClickListener { marker ->
                markerEntries[marker.id]?.let(onEntryTapped)
                true
            }
            readyMap.setStyle(Style.Builder().fromJson(offlineStyle)) {
                styleReady = true
                redraw()
            }
        }
    }

    fun reload(savedEntries: List<TrailEntry>) {
        entries = savedEntries
        redraw()
    }

    private fun redraw() {
        val readyMap = map ?: return
        if (!styleReady) return
        readyMap.clear()
        markerEntries.clear()
        entries.forEach { entry ->
            val point = entry.geoPoint ?: return@forEach
            val marker = readyMap.addMarker(
                MarkerOptions().position(LatLng(point.latitude, point.longitude))
            )
            markerEntries[marker.id] = entry
        }
        if (!framed) {
            val first = entries.firstOrNull { it.geoPoint != null }?.geoPoint
            if (first != null) {
                readyMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(first.latitude, first.longitude), 13.0
                    )
                )
                framed = true
            }
        }
    }
}

private val offlineStyle = """
{
  "version": 8,
  "name": "MudMap Offline",
  "center": [-122.44, 37.77],
  "zoom": 11,
  "glyphs": "file:///android_asset/fonts/{fontstack}/{range}.pbf",
  "sprite": "file:///android_asset/map/sprite",
  "sources": {
    "region": {
      "type": "vector",
      "url": "pmtiles://file:///android_asset/region.pmtiles",
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": { "background-color": "#eeeade" }
    },
    {
      "id": "water",
      "type": "fill",
      "source": "region",
      "source-layer": "water",
      "paint": { "fill-color": "#a4c9d3" }
    },
    {
      "id": "paths",
      "type": "line",
      "source": "region",
      "source-layer": "roads",
      "paint": { "line-color": "#76684f", "line-width": 1.5 }
    }
  ]
}
""".trimIndent()

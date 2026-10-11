package com.mudmap.patterns.structural

import com.mudmap.domain.TrailEntry

/**
 * BRIDGE (GoF 151).
 * Participants: Abstraction + Implementor. MapViewController is the
 * RefinedAbstraction; MapRenderer is the Implementor. The abstraction
 * (screen semantics) and the implementation (MapLibre vs future renderer)
 * vary independently, which is exactly the situation described in the
 * README where PMTiles vs MBTiles vs a future SDK is unresolved.
 */
interface MapRenderer {
    fun showPins(entries: List<TrailEntry>)
    fun focusOn(lat: Double, lon: Double)
}

abstract class MapViewController(
    protected val renderer: MapRenderer
) {
    abstract fun render(entries: List<TrailEntry>)
}

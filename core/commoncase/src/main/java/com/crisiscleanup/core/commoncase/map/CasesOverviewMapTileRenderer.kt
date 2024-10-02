package com.crisiscleanup.core.commoncase.map

import kotlinx.coroutines.flow.Flow

interface CasesOverviewMapTileRenderer {
    val isBusy: Flow<Boolean>

    /**
     * Zoom level at which tiles still render
     *
     * Higher zooms will not render any tiles.
     * At a zoom level of 0 there is 1 tile (1x1).
     * At a zoom level of 8 there are 64x64 tiles.
     */
    var zoomThreshold: Int

    /**
     * @return true if incident is changed or false otherwise
     */
    fun setIncident(id: Long, worksitesCount: Int, clearCache: Boolean = true): Boolean

    fun setLocation(coordinates: Pair<Double, Double>?)

    fun enableTileBoundaries()
}

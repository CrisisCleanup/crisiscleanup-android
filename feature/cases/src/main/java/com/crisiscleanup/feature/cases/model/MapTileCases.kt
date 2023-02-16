package com.crisiscleanup.feature.cases.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider.NO_TILE

data class MapTileCases(
    val southwest: LatLng,
    val northeast: LatLng,
    val tileCaseCount: Int,
    val incidentCaseCount: Int,
    val tile: Tile?,
)

val NoCasesMapTile = MapTileCases(
    southwest = LatLng(0.0, 0.0),
    northeast = LatLng(0.0, 0.0),
    tileCaseCount = 0,
    incidentCaseCount = 0,
    tile = NO_TILE,
)
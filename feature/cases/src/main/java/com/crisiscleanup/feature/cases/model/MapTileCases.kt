package com.crisiscleanup.feature.cases.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile

data class MapTileCases(
    val southwest: LatLng,
    val northeast: LatLng,
    val caseCount: Int,
    val tile: Tile?,
)

val EmptyMapTileCases = MapTileCases(
    southwest = LatLng(0.0, 0.0),
    northeast = LatLng(0.0, 0.0),
    caseCount = 0,
    tile = null,
)
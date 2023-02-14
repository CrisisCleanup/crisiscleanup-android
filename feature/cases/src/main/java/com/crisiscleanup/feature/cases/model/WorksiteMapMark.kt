package com.crisiscleanup.feature.cases.model

import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState

data class WorksiteGoogleMapMark(
    val source: WorksiteMapMark,
    val latLng: LatLng,
    val markerState: MarkerState,
    val mapDotIcon: BitmapDescriptor?,
)

fun WorksiteMapMark.asWorksiteGoogleMapMark(dotProvider: MapCaseDotProvider): WorksiteGoogleMapMark {
    val latLng = LatLng(latitude, longitude)
    return WorksiteGoogleMapMark(
        source = this,
        latLng = latLng,
        markerState = MarkerState(latLng),
        dotProvider.getDotIcon(statusClaim),
    )
}

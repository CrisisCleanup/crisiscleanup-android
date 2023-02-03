package com.crisiscleanup.feature.cases.model

import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState

data class WorksiteGoogleMapMark(
    val source: WorksiteMapMark,
    val latLng: LatLng,
    val markerState: MarkerState,
)

fun WorksiteMapMark.asWorksiteGoogleMapMark(): WorksiteGoogleMapMark {
    val latLng = LatLng(latitude.toDouble(), longitude.toDouble())
    return WorksiteGoogleMapMark(
        source = this,
        latLng = latLng,
        markerState = MarkerState(latLng),
    )
}
